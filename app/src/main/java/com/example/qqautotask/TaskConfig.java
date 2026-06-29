package com.example.qqautotask;

/**
 * 任务配置常量类
 * 包含QQ包名、界面文字匹配、延迟时间等配置
 */
public final class TaskConfig {

    private TaskConfig() {
        // 工具类，禁止实例化
    }

    // ==================== 包名 ====================
    /** QQ 包名 */
    public static final String QQ_PACKAGE = "com.tencent.mobileqq";

    // ==================== 界面文字匹配 ====================
    /** 签到 */
    public static final String TEXT_SIGN_IN = "签到";
    /** 领取 */
    public static final String TEXT_CLAIM = "领取";
    /** 一键领取 */
    public static final String TEXT_CLAIM_ALL = "一键领取";
    /** 浏览 */
    public static final String TEXT_BROWSE = "浏览";
    /** 跳过 */
    public static final String TEXT_SKIP = "跳过";
    /** 我知道了 */
    public static final String TEXT_GOT_IT = "我知道了";
    /** 取消 */
    public static final String TEXT_CANCEL = "取消";
    /** 已完成 */
    public static final String TEXT_COMPLETED = "已完成";
    /** 已领取 */
    public static final String TEXT_CLAIMED = "已领取";
    /** 关闭 */
    public static final String TEXT_CLOSE = "关闭";

    // ==================== 延迟时间 (毫秒) ====================
    /** 短延迟 */
    public static final long SHORT_DELAY = 1000L;
    /** 中等延迟 */
    public static final long MEDIUM_DELAY = 2000L;
    /** 长延迟 */
    public static final long LONG_DELAY = 3000L;
    /** 页面加载等待 */
    public static final long PAGE_LOAD_DELAY = 2500L;
    /** 浏览任务等待时间 */
    public static final long BROWSE_WAIT_MS = 15000L;
    /** 视频任务等待时间 */
    public static final long VIDEO_WAIT_MS = 30000L;
}
