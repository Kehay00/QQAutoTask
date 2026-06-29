package com.example.qqautotask;

public class QQAutoServiceRef {
    private static QQAutoService instance;

    public static void setInstance(QQAutoService service) {
        instance = service;
    }

    public static QQAutoService getInstance() {
        return instance;
    }
}