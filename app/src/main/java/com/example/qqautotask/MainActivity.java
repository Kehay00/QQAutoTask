package com.example.qqautotask;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

    private TextView tvStatus;
    private TextView tvDebug;
    private TextView tvLog;
    private ScrollView svLog;
    private Button btnToggle;
    private boolean isTaskRunning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        tvStatus = findViewById(R.id.tv_status);
        tvDebug = findViewById(R.id.tv_debug);
        tvLog = findViewById(R.id.tv_log);
        svLog = findViewById(R.id.sv_log);
        btnToggle = findViewById(R.id.btn_toggle);
        Button btnOpenQQ = findViewById(R.id.btn_open_qq);
        Button btnSettings = findViewById(R.id.btn_settings);

        tvLog.setMovementMethod(new ScrollingMovementMethod());

        // 加载历史日志
        for (String log : QQAutoService.getLogBuffer()) {
            appendLog(log);
        }

        // 设置日志监听
        QQAutoService.setLogListener(this::appendLog);

        // 开始/停止按钮
        btnToggle.setOnClickListener(v -> {
            if (!isAccessibilityEnabled()) {
                Toast.makeText(this, "请先开启无障碍服务", Toast.LENGTH_LONG).show();
                openAccessibilitySettings();
                return;
            }
            if (isTaskRunning) {
                stopTask();
            } else {
                startTask();
            }
        });

        // 打开QQ按钮 - 多种方式尝试
        btnOpenQQ.setOnClickListener(v -> openQQ());

        // 无障碍设置按钮
        btnSettings.setOnClickListener(v -> openAccessibilitySettings());

        updateStatus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    // ===================== 打开QQ（多重尝试） =====================

    private void openQQ() {
        // 方式1：用 Action + Package
        try {
            Intent intent = new Intent(Intent.ACTION_MAIN);
            intent.addCategory(Intent.CATEGORY_LAUNCHER);
            intent.setPackage(TaskConfig.QQ_PACKAGE);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Log.d(TAG, "QQ 启动成功(方式1)");
            return;
        } catch (Exception e) {
            Log.d(TAG, "QQ 启动方式1失败: " + e.getMessage());
        }

        // 方式2：指定 Activity
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName(
                    TaskConfig.QQ_PACKAGE,
                    "com.tencent.mobileqq.activity.SplashActivity"));
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            Log.d(TAG, "QQ 启动成功(方式2)");
            return;
        } catch (Exception e) {
            Log.d(TAG, "QQ 启动方式2失败: " + e.getMessage());
        }

        // 方式3：PackageManager
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(TaskConfig.QQ_PACKAGE);
            if (intent != null) {
                startActivity(intent);
                Log.d(TAG, "QQ 启动成功(方式3)");
                return;
            }
        } catch (Exception e) {
            Log.d(TAG, "QQ 启动方式3失败: " + e.getMessage());
        }

        Toast.makeText(this, "无法启动QQ，请确认已安装", Toast.LENGTH_SHORT).show();
    }

    // ===================== 任务控制 =====================

    private void startTask() {
        // 先启动QQ
        openQQ();

        // 延迟启动任务，等待QQ加载
        tvLog.postDelayed(() -> {
            QQAutoService service = QQAutoServiceRef.getInstance();
            if (service != null) {
                service.startAutoTask();
                isTaskRunning = true;
                updateStatus();
                appendLog("任务已启动");
            } else {
                appendLog("错误：无法获取无障碍服务实例");
                Toast.makeText(this, "服务未就绪，请稍后重试", Toast.LENGTH_SHORT).show();
            }
        }, 3000);
    }

    private void stopTask() {
        QQAutoService service = QQAutoServiceRef.getInstance();
        if (service != null) {
            service.stopAutoTask();
        }
        isTaskRunning = false;
        updateStatus();
        appendLog("任务已停止");
    }

    // ===================== UI 更新 =====================

    private void appendLog(String log) {
        runOnUiThread(() -> {
            tvLog.append(log + "\n");
            svLog.post(() -> svLog.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    private void updateStatus() {
        boolean enabled = isAccessibilityEnabled();

        // 调试信息
        String myService = getPackageName() + "/" + QQAutoService.class.getCanonicalName();
        String enabledRaw = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
        tvDebug.setText("期望ID: " + myService + "\n"
                + "已启用: " + (enabledRaw != null ? enabledRaw : "null"));

        if (!enabled) {
            tvStatus.setText("无障碍服务未开启");
            tvStatus.setTextColor(0xFFFF6B6B);
            btnToggle.setText("开启无障碍服务");
        } else if (isTaskRunning) {
            tvStatus.setText("正在执行任务...");
            tvStatus.setTextColor(0xFF69DB7C);
            btnToggle.setText("停止任务");
        } else {
            tvStatus.setText("就绪");
            tvStatus.setTextColor(0xFF69DB7C);
            btnToggle.setText("开始自动任务");
        }
    }

    // ===================== 无障碍服务检测 =====================

    private boolean isAccessibilityEnabled() {
        String myService = getPackageName() + "/" + QQAutoService.class.getCanonicalName();
        String myServiceShort = getPackageName() + "/." + QQAutoService.class.getSimpleName();

        // 方法1：Settings.Secure（冒号分隔）
        try {
            String enabled = Settings.Secure.getString(
                    getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (enabled != null && !enabled.isEmpty()) {
                String[] services = enabled.split(":");
                for (String s : services) {
                    s = s.trim();
                    if (s.equalsIgnoreCase(myService) || s.equalsIgnoreCase(myServiceShort)) {
                        Log.d(TAG, "方法1检测到服务: " + s);
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "方法1异常: " + e.getMessage());
        }

        // 方法2：AccessibilityManager 正在运行的服务
        try {
            AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
            List<AccessibilityServiceInfo> running = am.getEnabledAccessibilityServiceList(
                    AccessibilityServiceInfo.FEEDBACK_GENERIC);
            for (AccessibilityServiceInfo info : running) {
                String id = info.getId();
                if (id != null && (id.equals(myService) || id.equals(myServiceShort))) {
                    Log.d(TAG, "方法2检测到服务: " + id);
                    return true;
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "方法2异常: " + e.getMessage());
        }

        // 方法3：检查已安装的服务列表中是否有本服务且 enabled
        try {
            AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
            List<AccessibilityServiceInfo> installed = am.getInstalledAccessibilityServiceList();
            for (AccessibilityServiceInfo info : installed) {
                String id = info.getId();
                if (id != null && (id.equals(myService) || id.equals(myServiceShort))) {
                    String enabled = Settings.Secure.getString(
                            getContentResolver(),
                            "enabled_accessibility_services");
                    if (enabled != null && enabled.contains(getPackageName())) {
                        Log.d(TAG, "方法3检测到服务");
                        return true;
                    }
                }
            }
        } catch (Exception e) {
            Log.d(TAG, "方法3异常: " + e.getMessage());
        }

        Log.d(TAG, "未检测到无障碍服务已启用");
        return false;
    }

    // ===================== 辅助 =====================

    private void openAccessibilitySettings() {
        try {
            startActivity(new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS));
        } catch (Exception e) {
            Toast.makeText(this, "无法打开无障碍设置", Toast.LENGTH_SHORT).show();
        }
    }
}