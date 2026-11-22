package com.universal.erp.platform.ui;

import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;

/**
 * NAMUNA: hozircha hamma joyda bo'sh ro'yxat qaytaradi (sahifa bo'sh holatlar bilan ochiladi).
 * Har bir metodga o'zingizning repository/servislaringizni ulang.
 *
 * Joylashuvi: src/main/java/com/universal/erp/platform/ui/BiDataSourceImpl.java
 */
@Service
public class BiDataSourceImpl implements BiDataSource {

    // TODO: private final YourFinanceRepo finance; ... konstruktorga qo'shing

    @Override public List<Income> incomes(String c, LocalDate f, LocalDate t) { return List.of(); }
    @Override public List<Expense> expenses(String c, LocalDate f, LocalDate t) { return List.of(); }
    @Override public List<Deal> deals(String c) { return List.of(); }
    @Override public List<Customer> customers(String c) { return List.of(); }
    @Override public List<Stock> stock(String c) { return List.of(); }
    @Override public List<Sale> sales(String c, LocalDate f, LocalDate t) { return List.of(); }
    @Override public List<Employee> employees(String c) { return List.of(); }
    @Override public List<Debt> debts(String c) { return List.of(); }
}