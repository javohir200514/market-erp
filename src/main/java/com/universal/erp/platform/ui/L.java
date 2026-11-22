package com.universal.erp.platform.ui;

public final class L {

    private L() {
    }

    public static String t(String uz, String ru, String en) {
        return new String[] {uz, ru, en}[AppTexts.index()];
    }
}