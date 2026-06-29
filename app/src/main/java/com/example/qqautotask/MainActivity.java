package com.example.qqautotask;

import android.accessibilityservice.AccessibilityServiceInfo;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.TextUtils;
import android.view.accessibility.AccessibilityManager;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * 主界面
 * 提供开始任务、打开QQ、无障碍设置三个按钮
 * 实时显示日志并自动滚动
 */
public class MainActivity extends AppCompatActivity {

    private TextView tvStatus;
    private TextView tvLog;
    private ScrollView scrollLog;
    private Button btnStart;

    private boolean isTaskRunning = false;

    private static final SimpleDateFormat TIME_FORMAT =
            new SimpleDateFormat("HH:mm:ss", Locale.getDefault());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initListeners();

        // 加载历史日志
        loadHistoryLog();

        // 注册日志监听
        QQAutoService.setLogListener(log -> runOnUiThread(() -> appendLog(log)));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 清除日志监听
        QQAutoService.setLogListener(null);
    }

    /**
     * 初始化视图
     */
    private void initViews() {
        tvStatus = findViewById(R.id.tvStatus);
        tvLog = findViewById(R.id.tvLog);
        scrollLog = findViewById(R.id.scrollLog);
        btnStart = findViewById(R.id.btnStart);
    }

    /**
     * 初始化按钮监听
     */
    private void initListeners() {
        findViewById(R.id.btnStart).setOnClickListener(v -> onStartClick());
        findViewById(R.id.btnOpenQQ).setOnClickListener(v -> openQQ());
        findViewById(R.id.btnAccessibility).setOnClickListener(v -> openAccessibilitySettings());
    }

    /**
     * 加载历史日志
     */
    private void loadHistoryLog() {
        List<String> history = QQAutoService.getLogBuffer();
        for (String log : history) {
            appendLog(log);
        }
    }

    // ==================== 按钮点击事件 ====================

    /**
     * 开始/停止任务按钮点击
     */
    private void onStartClick() {
        if (isTaskRunning) {
            // 停止任务
            QQAutoService service = QQAutoServiceRef.getInstance();
            if (service != null) {
                service.stopAutoTask();
            }
            updateStatus("已停止", false);
            btnStart.setText("开始自动任务");
            isTaskRunning = false;
            appendLog("[界面] 用户手动停止任务");
        } else {
            // 检查无障碍服务是否启用
            if (!isAccessibilityServiceEnabled()) {
                appendLog("[界面] 无障碍服务未启用，请先开启");
                updateStatus("需要无障碍权限", false);
                openAccessibilitySettings();
                return;
            }

            // 检查无障碍服务实例
            QQAutoService service = QQAutoServiceRef.getInstance();
            if (service == null) {
                appendLog("[界面] 无障碍服务未连接，请确保服务已开启");
                updateStatus("服务未连接", false);
                return;
            }

            // 先启动QQ，延迟3秒后启动引擎
            appendLog("[界面] 正在启动QQ...");
            updateStatus("启动QQ中...", true);
            openQQ();

            btnStart.postDelayed(() -> {
                appendLog("[界面] 启动任务引擎...");
                updateStatus("任务执行中...", true);
                service.startAutoTask();
                btnStart.setText("停止任务");
                isTaskRunning = true;
            }, 3000);
        }
    }

    /**
     * 打开QQ
     */
    private void openQQ() {
        try {
            Intent intent = getPackageManager().getLaunchIntentForPackage(TaskConfig.QQ_PACKAGE);
            if (intent != null) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent);
                appendLog("[界面] 已启动QQ");
            } else {
                appendLog("[界面] 未安装QQ");
                updateStatus("未安装QQ", false);
            }
        } catch (Exception e) {
            appendLog("[界面] 启动QQ失败: " + e.getMessage());
        }
    }

    /**
     * 打开无障碍设置页面
     */
    private void openAccessibilitySettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(intent);
            appendLog("[界面] 已打开无障碍设置");
        } catch (Exception e) {
            appendLog("[界面] 打开设置失败: " + e.getMessage());
        }
    }

    // ==================== 状态更新 ====================

    /**
     * 更新状态显示
     */
    private void updateStatus(String status, boolean isRunning) {
        tvStatus.setText(status);
        if (isRunning) {
            tvStatus.setTextColor(getColor(R.color.primary_green_light));
        } else {
            tvStatus.setTextColor(getColor(R.color.text_secondary));
        }
    }

    // ==================== 日志 ====================

    /**
     * 追加日志并自动滚动到底部
     */
    private void appendLog(String message) {
        String time = TIME_FORMAT.format(new Date());
        String logLine = "[" + time + "] " + message + "\n";

        tvLog.append(logLine);

        // 自动滚动到底部
        scrollLog.post(() -> scrollLog.fullScroll(ScrollView.FOCUS_DOWN));
    }

    // ==================== 无障碍检查 ====================

    /**
     * 检查本应用的无障碍服务是否已启用
     */
    private boolean isAccessibilityServiceEnabled() {
        AccessibilityManager am = (AccessibilityManager) getSystemService(Context.ACCESSIBILITY_SERVICE);
        if (am == null) return false;

        List<AccessibilityServiceInfo> enabledServices = am.getEnabledAccessibilityServiceList(
                AccessibilityServiceInfo.FEEDBACK_ALL_MASK);

        String serviceName = getPackageName() + "/.QQAutoService";

        for (AccessibilityServiceInfo info : enabledServices) {
            if (info.getId().equals(serviceName)) {
                return true;
            }
        }
        return false;
    }
}
