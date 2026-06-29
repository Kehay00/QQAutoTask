package com.example.qqautotask;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.GestureDescription;
import android.graphics.Path;
import android.graphics.Rect;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.accessibility.AccessibilityNodeInfo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public class TaskEngine {

    private static final String TAG = "TaskEngine";

    public interface Callback {
        void onLog(String msg);
        void onTaskComplete();
    }

    private final AccessibilityService service;
    private final Handler handler;
    private final Callback callback;
    private final Deque<Runnable> taskQueue = new ArrayDeque<>();
    private volatile boolean running = false;

    public TaskEngine(AccessibilityService service, Callback callback) {
        this.service = service;
        this.handler = new Handler(Looper.getMainLooper());
        this.callback = callback;
    }

    public void start() {
        if (running) return;
        running = true;
        Log.d(TAG, "引擎启动");
        log("引擎启动");
        buildTaskQueue();
        executeNext();
    }

    public void stop() {
        running = false;
        taskQueue.clear();
        handler.removeCallbacksAndMessages(null);
        Log.d(TAG, "引擎停止");
        log("引擎停止");
    }

    public boolean isRunning() {
        return running;
    }

    private void buildTaskQueue() {
        taskQueue.clear();
        taskQueue.add(this::dismissPopups);
        taskQueue.add(this::doSignIn);
        taskQueue.add(this::claimAllRewards);
        taskQueue.add(this::doBrowseTasks);
        taskQueue.add(this::claimAllRewards);
        taskQueue.add(() -> {
            Log.d(TAG, "全部任务完成");
            log("=== 全部任务完成 ===");
            running = false;
            callback.onTaskComplete();
        });
    }

    private void executeNext() {
        if (!running || taskQueue.isEmpty()) return;
        Runnable next = taskQueue.poll();
        handler.post(() -> {
            if (running && next != null) next.run();
        });
    }

    private void delayThenNext(long ms) {
        handler.postDelayed(this::executeNext, ms);
    }

    // ===================== 任务实现 =====================

    private void dismissPopups() {
        Log.d(TAG, "检查弹窗...");
        log("检查弹窗...");
        clickByText(TaskConfig.SKIP_TEXT);
        clickByText(TaskConfig.KNOWN_TEXT);
        clickByText(TaskConfig.CANCEL_TEXT);
        clickByDesc(TaskConfig.CLOSE_DESC);
        delayThenNext(TaskConfig.MEDIUM_DELAY);
    }

    private void doSignIn() {
        Log.d(TAG, "执行签到...");
        log("执行签到...");
        boolean r = clickByTextContains(TaskConfig.SIGN_IN_TEXT);
        if (r) {
            Log.d(TAG, "已点击签到");
            log("已点击签到");
        } else {
            Log.d(TAG, "未找到签到");
            log("未找到签到(可能已完成)");
        }
        delayThenNext(TaskConfig.LONG_DELAY);
    }

    private void claimAllRewards() {
        Log.d(TAG, "领取奖励...");
        log("领取奖励...");
        boolean r = clickByText(TaskConfig.CLAIM_ALL_TEXT);
        if (!r) {
            List<AccessibilityNodeInfo> nodes = findNodesByText(TaskConfig.CLAIM_REWARD_TEXT);
            for (AccessibilityNodeInfo node : nodes) {
                performClick(node);
                try { Thread.sleep(800); } catch (InterruptedException ignored) {}
                clickByText(TaskConfig.KNOWN_TEXT);
            }
            recycleAll(nodes);
        }
        delayThenNext(TaskConfig.MEDIUM_DELAY);
    }

    private void doBrowseTasks() {
        Log.d(TAG, "浏览类任务...");
        log("浏览类任务...");
        boolean found = clickByTextContains(TaskConfig.BROWSE_TASK_TEXT);
        if (found) {
            int waitSec = TaskConfig.BROWSE_WAIT_MS / 1000;
            Log.d(TAG, "等待浏览 " + waitSec + "秒...");
            log("等待浏览 " + waitSec + "秒...");
            handler.postDelayed(() -> {
                Log.d(TAG, "浏览结束，返回...");
                log("浏览结束，返回...");
                service.performGlobalAction(AccessibilityService.GLOBAL_ACTION_BACK);
                handler.postDelayed(() -> delayThenNext(TaskConfig.MEDIUM_DELAY), TaskConfig.PAGE_LOAD_DELAY);
            }, TaskConfig.BROWSE_WAIT_MS);
        } else {
            Log.d(TAG, "未找到浏览任务");
            log("未找到浏览任务");
            delayThenNext(TaskConfig.SHORT_DELAY);
        }
    }

    // ===================== 节点查找 =====================

    private AccessibilityNodeInfo getRoot() {
        return service.getRootInActiveWindow();
    }

    private boolean clickByText(String text) {
        AccessibilityNodeInfo root = getRoot();
        if (root == null) return false;
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        boolean r = false;
        for (AccessibilityNodeInfo n : nodes) {
            CharSequence t = n.getText();
            if (t != null && t.toString().equals(text)) {
                r = performClick(n);
                if (r) break;
            }
        }
        recycleAll(nodes);
        root.recycle();
        return r;
    }

    private boolean clickByTextContains(String text) {
        AccessibilityNodeInfo root = getRoot();
        if (root == null) return false;
        List<AccessibilityNodeInfo> nodes = root.findAccessibilityNodeInfosByText(text);
        boolean r = false;
        for (AccessibilityNodeInfo n : nodes) {
            CharSequence t = n.getText();
            if (t != null && t.toString().contains(text)) {
                if (!t.toString().contains(TaskConfig.TASK_DONE_TEXT)
                        && !t.toString().contains(TaskConfig.TASK_CLAIMED_TEXT)) {
                    r = performClick(n);
                    if (r) break;
                }
            }
        }
        recycleAll(nodes);
        root.recycle();
        return r;
    }

    private boolean clickByDesc(String desc) {
        AccessibilityNodeInfo root = getRoot();
        if (root == null) return false;
        boolean r = findByDesc(root, desc);
        root.recycle();
        return r;
    }

    private boolean findByDesc(AccessibilityNodeInfo node, String desc) {
        if (node == null) return false;
        CharSequence cd = node.getContentDescription();
        if (cd != null && cd.toString().contains(desc)) {
            return performClick(node);
        }
        for (int i = 0; i < node.getChildCount(); i++) {
            AccessibilityNodeInfo child = node.getChild(i);
            if (child != null) {
                if (findByDesc(child, desc)) return true;
            }
        }
        return false;
    }

    private List<AccessibilityNodeInfo> findNodesByText(String text) {
        AccessibilityNodeInfo root = getRoot();
        if (root == null) return new ArrayList<>();
        List<AccessibilityNodeInfo> r = root.findAccessibilityNodeInfosByText(text);
        root.recycle();
        return r;
    }

    // ===================== 点击逻辑 =====================

    private boolean performClick(AccessibilityNodeInfo node) {
        if (node == null) return false;

        // 方式1：直接点击
        if (node.isClickable()) {
            return node.performAction(AccessibilityNodeInfo.ACTION_CLICK);
        }

        // 方式2：点击父节点
        AccessibilityNodeInfo parent = node.getParent();
        if (parent != null && parent.isClickable()) {
            boolean r = parent.performAction(AccessibilityNodeInfo.ACTION_CLICK);
            parent.recycle();
            return r;
        }

        // 方式3：坐标手势点击
        Rect rect = new Rect();
        node.getBoundsInScreen(rect);
        return gestureClick(rect.centerX(), rect.centerY());
    }

    private boolean gestureClick(int x, int y) {
        Path path = new Path();
        path.moveTo(x, y);
        GestureDescription.StrokeDescription stroke =
                new GestureDescription.StrokeDescription(path, 0, 100);
        GestureDescription gesture = new GestureDescription.Builder()
                .addStroke(stroke)
                .build();
        return service.dispatchGesture(gesture, null, null);
    }

    // ===================== 辅助 =====================

    private void recycleAll(List<AccessibilityNodeInfo> nodes) {
        for (AccessibilityNodeInfo n : nodes) {
            if (n != null) n.recycle();
        }
    }

    private void log(String msg) {
        callback.onLog(msg);
    }
}