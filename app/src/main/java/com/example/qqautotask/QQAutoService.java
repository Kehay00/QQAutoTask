package com.example.qqautotask;

import android.accessibilityservice.AccessibilityService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.Intent;
import android.os.Build;
import android.view.accessibility.AccessibilityEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * QQ 自动升级任务无障碍服务
 * 继承 AccessibilityService，实现 TaskEngine.Callback
 */
public class QQAutoService extends AccessibilityService implements TaskEngine.Callback {

    private static final String CHANNEL_ID = "qq_auto_task_channel";
    private static final int NOTIFICATION_ID = 1001;

    private TaskEngine engine;
    private boolean isTaskRunning = false;

    // 日志缓冲区
    private static final List<String> logBuffer = new ArrayList<>();
    private static final int MAX_LOG_LINES = 500;

    // 日志监听器
    private static LogListener logListener;

    /**
     * 日志监听接口
     */
    public interface LogListener {
        void onNewLog(String log);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        engine = new TaskEngine(this);
        engine.setCallback(this);
        QQAutoServiceRef.setInstance(this);
        log("[服务] 无障碍服务已创建");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 事件监听，可用于触发任务
        int eventType = event.getEventType();
        if (eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED ||
                eventType == AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED) {
            String packageName = event.getPackageName() != null ? event.getPackageName().toString() : "";
            if (packageName.contains(TaskConfig.QQ_PACKAGE)) {
                // QQ 界面变化，引擎内部自行处理
            }
        }
    }

    @Override
    public void onInterrupt() {
        log("[服务] 无障碍服务被中断");
        if (engine != null) {
            engine.stopTask();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        log("[服务] 无障碍服务已销毁");
        if (engine != null) {
            engine.stopTask();
        }
        QQAutoServiceRef.setInstance(null);
    }

    // ==================== 任务控制 ====================

    /**
     * 启动自动任务
     */
    public void startAutoTask() {
        if (isTaskRunning) {
            log("[服务] 任务已在运行中");
            return;
        }
        isTaskRunning = true;
        startForeground();
        engine.startTask();
    }

    /**
     * 停止自动任务
     */
    public void stopAutoTask() {
        isTaskRunning = false;
        if (engine != null) {
            engine.stopTask();
        }
        stopForeground(false);
    }

    /**
     * 获取任务运行状态
     */
    public boolean isTaskRunning() {
        return isTaskRunning;
    }

    // ==================== 前台服务 ====================

    /**
     * 启动前台服务通知
     */
    private void startForeground() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification notification = new Notification.Builder(this, CHANNEL_ID)
                    .setContentTitle("QQ 自动任务")
                    .setContentText("正在执行升级任务...")
                    .setSmallIcon(android.R.drawable.ic_dialog_info)
                    .setOngoing(true)
                    .build();
            startForeground(NOTIFICATION_ID, notification);
        }
    }

    /**
     * 创建通知渠道
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "QQ 自动任务",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setDescription("QQ 自动升级任务通知");
            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    // ==================== TaskEngine.Callback ====================

    @Override
    public void onLog(String message) {
        String timestamp = java.text.SimpleDateFormat.getDateTimeInstance()
                .format(new java.util.Date());
        String logLine = "[" + timestamp + "] " + message;

        synchronized (logBuffer) {
            logBuffer.add(logLine);
            if (logBuffer.size() > MAX_LOG_LINES) {
                logBuffer.remove(0);
            }
        }

        // 通知日志监听器
        if (logListener != null) {
            logListener.onNewLog(logLine);
        }
    }

    @Override
    public void onTaskComplete() {
        isTaskRunning = false;
        log("[服务] 所有任务已完成");
        stopForeground(false);
    }

    // ==================== 静态方法 ====================

    /**
     * 获取日志缓冲区（线程安全）
     */
    public static List<String> getLogBuffer() {
        synchronized (logBuffer) {
            return new ArrayList<>(logBuffer);
        }
    }

    /**
     * 设置日志监听器
     */
    public static void setLogListener(LogListener listener) {
        logListener = listener;
    }
}
