package com.example.qqautotask;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;

/**
 * 核心任务引擎
 * 使用 AccessibilityService 标准 API，任务队列驱动
 * 任务顺序：关闭弹窗 → 签到 → 领取奖励 → 浏览任务 → 再领一次 → 完成
 */
public class TaskEngine {

    private final AccessibilityService service;
    private final Handler handler;
    private final Deque<Runnable> taskQueue;
    private boolean isRunning = false;

    private Callback callback;

    /**
     * 任务回调接口
     */
    public interface Callback {
        /** 日志输出 */
        void onLog(String message);
        /** 任务完成 */
        void onTaskComplete();
    }

    public TaskEngine(AccessibilityService service) {
        this.service = service;
        this.handler = new Handler(Looper.getMainLooper());
        this.taskQueue = new ArrayDeque<>();
    }

    /**
     * 设置回调
     */
    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    /**
     * 获取运行状态
     */
    public boolean isRunning() {
        return isRunning;
    }

    // ==================== 日志 ====================

    private void log(String message) {
        if (callback != null) {
            callback.onLog(message);
        }
    }

    // ==================== 任务调度 ====================

    /**
     * 启动任务序列
     */
    public void startTask() {
        if (isRunning) {
            log("[引擎] 任务已在运行中");
            return;
        }
        isRunning = true;
        log("[引擎] ====== 开始执行任务序列 ======");

        // 构建任务队列
        taskQueue.clear();

        // 1. 关闭弹窗
        taskQueue.addLast(() -> {
            log("[引擎] 步骤1: 尝试关闭弹窗");
            clickText(TaskConfig.TEXT_CLOSE, () -> {
                log("[引擎] 弹窗已关闭");
                nextTask();
            });
        });

        // 2. 签到
        taskQueue.addLast(() -> {
            log("[引擎] 步骤2: 尝试签到");
            delay(TaskConfig.PAGE_LOAD_DELAY);
            clickText(TaskConfig.TEXT_SIGN_IN, () -> {
                log("[引擎] 签到完成");
                nextTask();
            });
        });

        // 3. 领取奖励
        taskQueue.addLast(() -> {
            log("[引擎] 步骤3: 尝试领取奖励");
            delay(TaskConfig.SHORT_DELAY);
            clickText(TaskConfig.TEXT_CLAIM, () -> {
                log("[引擎] 领取奖励完成");
                nextTask();
            });
        });

        // 4. 一键领取
        taskQueue.addLast(() -> {
            log("[引擎] 步骤4: 尝试一键领取");
            delay(TaskConfig.SHORT_DELAY);
            clickText(TaskConfig.TEXT_CLAIM_ALL, () -> {
                log("[引擎] 一键领取完成");
                nextTask();
            });
        });

        // 5. 浏览任务
        taskQueue.addLast(() -> {
            log("[引擎] 步骤5: 执行浏览任务");
            delay(TaskConfig.SHORT_DELAY);
            clickText(TaskConfig.TEXT_BROWSE, () -> {
                log("[引擎] 已点击浏览任务，等待 " + (TaskConfig.BROWSE_WAIT_MS / 1000) + " 秒...");
                delay(TaskConfig.BROWSE_WAIT_MS);
                // 返回上一页
                performBack();
                log("[引擎] 浏览任务完成，已返回");
                nextTask();
            });
        });

        // 6. 再领一次
        taskQueue.addLast(() -> {
            log("[引擎] 步骤6: 再次尝试领取");
            delay(TaskConfig.SHORT_DELAY);
            clickText(TaskConfig.TEXT_CLAIM, () -> {
                log("[引擎] 再次领取完成");
                nextTask();
            });
        });

        // 7. 完成
        taskQueue.addLast(() -> {
            log("[引擎] ====== 所有任务执行完毕 ======");
            isRunning = false;
            if (callback != null) {
                callback.onTaskComplete();
            }
        });

        // 开始执行第一个任务
        nextTask();
    }

    /**
     * 停止任务
     */
    public void stopTask() {
        log("[引擎] 任务已停止");
        taskQueue.clear();
        isRunning = false;
    }

    /**
     * 执行下一个任务
     */
    private void nextTask() {
        if (!isRunning) {
            return;
        }
        Runnable task = taskQueue.pollFirst();
        if (task != null) {
            handler.post(task);
        } else {
            log("[引擎] 任务队列为空");
            isRunning = false;
            if (callback != null) {
                callback.onTaskComplete();
            }
        }
    }

    // ==================== 延迟 ====================

    /**
     * 延迟指定毫秒数（阻塞当前任务，但通过 Handler 实现非阻塞）
     */
    private void delay(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    // ==================== 节点查找 ====================

    /**
     * 根据文字查找可点击节点
     */
    private AccessibilityNodeInfo findClickableNodeByText(String text) {
        AccessibilityNodeInfo root = service.getRootInActiveWindow();
        if (root == null) {
            log("[查找] 无法获取根节点");
            return null;
        }

        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        if (nodes != null && !nodes.isEmpty()) {
            for (AccessibilityNodeInfo node : nodes) {
                if (node != null) {
                    // 优先返回可点击的节点
                    if (node.isClickable()) {
                        return node;
                    }
                    // 检查父节点是否可点击
                    AccessibilityNodeInfo parent = node.getParent();
                    if (parent != null && parent.isClickable()) {
                        return parent;
                    }
                }
            }
            // 没有可点击的，返回第一个
            AccessibilityNodeInfo first = nodes.get(0);
            if (first != null) {
                return first;
            }
        }
        return null;
    }

    // ==================== 点击操作 ====================

    /**
     * 点击包含指定文字的节点
     * 三级降级策略：本身可点击 → 父节点可点击 → 手势坐标点击
     */
    private void clickText(String text, Runnable onSuccess) {
        AccessibilityNodeInfo node = findClickableNodeByText(text);
        if (node == null) {
            log("[点击] 未找到文字: " + text + "，跳过");
            if (onSuccess != null) {
                onSuccess.run();
            }
            return;
        }

        boolean clicked = false;

        // 第一级：节点本身可点击
        if (node.isClickable()) {
            clicked = node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            if (clicked) {
                log("[点击] 一级点击成功: " + text);
            }
        }

        // 第二级：父节点可点击
        if (!clicked) {
            AccessibilityNodeInfo parent = node.getParent();
            if (parent != null && parent.isClickable()) {
                clicked = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
                if (clicked) {
                    log("[点击] 二级点击(父节点)成功: " + text);
                }
            }
        }

        // 第三级：手势坐标点击
        if (!clicked) {
            clicked = performGestureClick(node);
            if (clicked) {
                log("[点击] 三级点击(手势)成功: " + text);
            }
        }

        if (!clicked) {
            log("[点击] 所有点击方式均失败: " + text);
        }

        if (onSuccess != null) {
            onSuccess.run();
        }
    }

    /**
     * 使用手势在节点中心位置点击
     */
    private boolean performGestureClick(AccessibilityNodeInfo node) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
            return false;
        }

        Rect rect = new Rect();
        node.getBoundsInScreen(rect);

        float centerX = rect.centerX();
        float centerY = rect.centerY();

        Path path = new Path();
        path.moveTo(centerX, centerY);

        GestureDescription.Builder builder = new GestureDescription.Builder();
        GestureDescription gesture = builder
                .addStroke(new GestureDescription.StrokeDescription(path, 0, 100))
                .build();

        return service.dispatchGesture(gesture, null, null);
    }

    /**
     * 执行返回操作
     */
    private void performBack() {
        service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
        log("[导航] 执行返回操作");
    }
}
