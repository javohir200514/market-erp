package com.universal.erp.platform.ui;

import java.util.Optional;

/**
 * Kirish paytida foydalanuvchi kiritgan email yoki foydalanuvchi nomi bo'yicha
 * kompaniyani (kirish nomini) topadi, shunda foydalanuvchi kompaniya nomini yozmaydi.
 *
 * Realizatsiya talablari:
 * - qidiruv tenant filtrisiz ishlashi kerak (kirishdan oldin TenantContext yo'q);
 * - email butun tizimda yagona bo'lsa, bitta kompaniya qaytadi;
 * - identifikator bir nechta kompaniyada uchrasa (masalan, bir xil foydalanuvchi nomi),
 *   Optional.empty() qaytaring: shunda kirish rad etiladi va hech narsa oshkor bo'lmaydi;
 * - topilmasa ham Optional.empty(): sahifa "noto'g'ri" xabarini ko'rsatadi, mavjudligini bildirmaydi.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/LoginCompanyResolver.java
 */
public interface LoginCompanyResolver {

    Optional<String> resolveCompany(String identifier);
}