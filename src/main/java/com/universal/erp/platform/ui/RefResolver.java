package com.universal.erp.platform.ui;

public final class RefResolver {

    public interface Lookup {
        /** sectionKey = "crm/customers", id = yozuv ID'si; qaytaradi: ko'rsatiladigan nom */
        String label(String sectionKey, String id);
    }

    private static volatile Lookup lookup = (key, id) -> id;

    private RefResolver() {}

    /** Saqlash servisi ishga tushganda bir marta chaqiriladi. */
    public static void install(Lookup l) {
        lookup = l;
    }

    public static String label(String sectionKey, String id) {
        return lookup.label(sectionKey, id);
    }
}