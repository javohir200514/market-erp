package com.universal.erp.platform.ui;

import java.time.LocalDate;
import java.util.List;

/**
 * BI uchun xom ma'lumot manbai. Har bir metodni o'z CRM / Ombor / Kadrlar / Moliya
 * servislaringiz bilan to'ldiring (BiDataSourceImpl.java ichida).
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/BiDataSource.java
 */
public interface BiDataSource {

    /** Moliya: kirimlar (kassa + hisob-fakturalar bo'yicha to'langan summa), so'mda. */
    List<Income> incomes(String company, LocalDate from, LocalDate to);

    /** Moliya: barcha chiqimlar. kind: COGS (tannarx), OPEX (xarajat), TAX (soliq), OTHER (boshqa to'lovlar). */
    List<Expense> expenses(String company, LocalDate from, LocalDate to);

    /** CRM: bitimlar. stage: lid / muzokara / yutildi / yo'qotildi (yoki ru/en nomlari). */
    List<Deal> deals(String company);

    /** CRM: mijozlar. */
    List<Customer> customers(String company);

    /** Ombor: joriy qoldiq (unitCost = tannarx narxi). */
    List<Stock> stock(String company);

    /** Ombor/Kassa: sotuvlar. */
    List<Sale> sales(String company, LocalDate from, LocalDate to);

    /** Kadrlar: xodimlar va oylik maoshi (yalpi). */
    List<Employee> employees(String company);

    /** Mijozlarning to'lanmagan qarzlari (ochiq hisob-fakturalar qoldig'i). */
    List<Debt> debts(String company);

    enum Kind { COGS, OPEX, TAX, OTHER }

    record Income(LocalDate date, double amount) {}
    record Expense(LocalDate date, String category, Kind kind, double amount) {}
    record Deal(String customer, String stage, double amount, LocalDate date) {}
    record Customer(String name, LocalDate created) {}
    record Stock(String name, String category, double qty, double unitCost) {}
    record Sale(LocalDate date, String product, double qty, double revenue) {}
    record Employee(String name, String department, double monthlySalary) {}
    record Debt(String customer, double amount, LocalDate dueDate) {}
}