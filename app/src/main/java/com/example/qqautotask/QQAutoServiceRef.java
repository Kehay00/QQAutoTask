package com.example.qqautotask;

/**
 * 简单的静态引用持有类
 * 用于在非 Activity/Service 类中获取 QQAutoService 实例
 */
public final class QQAutoServiceRef {

    private static QQAutoService instance;

    private QQAutoServiceRef() {
        // 工具类，禁止实例化
    }

    /**
     * 设置 QQAutoService 实例
     */
    public static void setInstance(QQAutoService service) {
        instance = service;
    }

    /**
     * 获取 QQAutoService 实例
     * @return 当前绑定的 QQAutoService 实例，可能为 null
     */
    public static QQAutoService getInstance() {
        return instance;
    }
}
