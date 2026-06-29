package com.example.qqautotask;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;
import android.view.accessibility.AccessibilityEvent;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class QQAutoService extends AccessibilityService implements TaskEngine.Callback {

    private static final String TAG = "QQAutoService";
    private static final String CHANNEL_ID = "qq_auto_task";
    private static final int NOTIFICATION_ID = 1001;

    private TaskEngine engine;
    private static final List<String> logBuffer = new ArrayList<>();
    private static LogListener logListener;

    public interface LogListener {
        void onNewLog(String log);
    }

    public static void setLogListener(LogListener listener) {
        logListener = listener;
    }

    public static List<String> getLogBuffer() {
        synchronized (logBuffer) {
            return new ArrayList<>(logBuffer);
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        QQAutoServiceRef.setInstance(this);
        engine = new TaskEngine(this, this);
        createNotificationChannel();
        Log.d(TAG, "无障碍服务已创建");
    }

    @Override
    public void onServiceConnected() {
        super.onServiceConnected();
        AccessibilityServiceInfo info = getServiceInfo();
        if (info != null) {
            info.flags |= AccessibilityServiceInfo.FLAG_REPORT_VIEW_IDS;
            info.flags |= AccessibilityServiceInfo.FLAG_INCLUDE_NOT_IMPORTANT_VIEWS;
            setServiceInfo(info);
        }
        Log.d(TAG, "无障碍服务已连接");
        doLog("无障碍服务已连接");
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        // 核心逻辑由 TaskEngine 主动查询节点，这里留空
    }

    @Override
    public void onInterrupt() {
        Log.w(TAG, "无障碍服务被中断");
        doLog("无障碍服务被中断");
        if (engine != null) engine.stop();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "无障碍服务已销毁");
        doLog("无障碍服务已销毁");
        QQAutoServiceRef.setInstance(null);
        if (engine != null) engine.stop();
        super.onDestroy();
    }

    // ===================== 控制接口 =====================

    public void startAutoTask() {
        Log.d(TAG, "startAutoTask 被调用");
        startForeground(NOTIFICATION_ID, buildNotification("正在执行任务..."));
        if (engine != null && !engine.isRunning()) {
            engine.start();
        }
    }

    public void stopAutoTask() {
        Log.d(TAG, "stopAutoTask 被调用");
        if (engine != null) engine.stop();
        stopForeground(true);
    }

    // ===================== Callback =====================

    @Override
    public void onLog(String msg) {
        doLog(msg);
    }

    @Override
    public void onTaskComplete() {
        doLog("全部任务完成");
        updateNotification("任务已完成");
    }

    private void doLog(String msg) {
        String ts = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());
        String line = "[" + ts + "] " + msg;
        synchronized (logBuffer) {
            logBuffer.add(line);
            if (logBuffer.size() > 200) logBuffer.remove(0);
        }
        if (logListener != null) {
            logListener.onNewLog(line);
        }
    }

    // ===================== 通知 =====================

    private void createNotificationChannel() {
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID, "QQ自动任务", NotificationManager.IMPORTANCE_LOW);
        channel.setDescription("QQ升级任务自动执行");
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) nm.createNotificationChannel(channel);
    }

    private Notification buildNotification(String text) {
        Intent intent = new Intent(this, MainActivity.class);
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent,
                PendingIntent.FLAG_IMMUTABLE);
        return new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("QQ自动任务")
                .setContentText(text)
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentIntent(pi)
                .setOngoing(true)
                .build();
    }

    private void updateNotification(String text) {
        NotificationManager nm = getSystemService(NotificationManager.class);
        if (nm != null) {
            nm.notify(NOTIFICATION_ID, buildNotification(text));
        }
    }
}