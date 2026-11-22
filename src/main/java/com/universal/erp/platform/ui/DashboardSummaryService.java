package com.universal.erp.platform.ui;

import java.util.List;

public interface DashboardSummaryService {

    record LowStockItem(
            String title,
            String detail,
            String imageUrl,
            String iconKey,
            double quantity,
            String unit,
            String targetRoute
    ) {}

    record ProfileHighlightItem(
            String title,
            String detail,
            String imageUrl,
            String iconKey,
            String badgeText,
            String badgeTone, // "bad", "warn", "ok", "info"
            String targetRoute
    ) {}

    record ProfileHighlightGroup(
            String sectionTitle,
            String countBadgeText,
            String badgeTone,
            String emptyMessage,
            List<ProfileHighlightItem> items
    ) {}

    record RecentActivityItem(
            String title,
            String subtitle,
            String statusOrAmount,
            String dateLabel,
            String targetRoute
    ) {}

    record ModuleCardSummary(
            AppModule module,
            int totalRecords,
            String highlightLabel,
            String highlightValue,
            int todayRecords,
            String secondaryLabel,
            String secondaryValue,
            List<Integer> last7DaysBars
    ) {}

    List<LowStockItem> loadLowStockItems(String company, List<AppModule> modules);

    ProfileHighlightGroup loadProfileHighlights(String company, List<AppModule> modules);

    List<RecentActivityItem> loadRecentActivities(String company, List<AppModule> modules);

    List<ModuleCardSummary> loadModuleSummaries(String company, List<AppModule> modules);

    int countSectionRecords(String company, AppSection section);
}