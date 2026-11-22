package com.universal.erp.platform.ui;

import static com.universal.erp.platform.ui.AppTexts.t;

import com.universal.erp.platform.security.RolePolicy;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.HasDynamicTitle;
import com.vaadin.flow.router.Route;
import jakarta.annotation.security.PermitAll;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Route(value = "app/:key", layout = MainLayout.class)
@PermitAll
public class ModuleView extends Div implements BeforeEnterObserver, HasDynamicTitle {

    private final TenantModules tenantModules;
    private final DashboardSummaryService summaryService;
    private AppModule module;

    public ModuleView(TenantModules tenantModules, DashboardSummaryService summaryService) {
        this.tenantModules = tenantModules;
        this.summaryService = summaryService;
    }

    private static List<AppSection> sectionsOf(AppModule module) {
        return AppSection.of(module);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        String company = CurrentCompany.get();
        Optional<AppModule> m = AppModule.byKey(event.getRouteParameters().get("key").orElse(""));
        if (m.isEmpty() || !tenantModules.enabled(company).contains(m.get()) || !RolePolicy.canView(m.get())) {
            event.forwardTo(DashboardView.class);
            return;
        }
        module = m.get();
        build(company);
    }

    private void build(String company) {
        removeAll();
        addClassName("sh-page");

        Anchor home = new Anchor("/app", tx("dash.title", "Bosh sahifa"));
        Span sep = new Span();
        sep.addClassName("sh-chev-r");
        Span here = new Span(module.title());
        Div crumbs = new Div(home, sep, here);
        crumbs.addClassName("sh-crumbs");

        H1 title = new H1(module.title());
        title.addClassName("sh-page-title");
        Paragraph sub = new Paragraph(tx("ov.sub", "Bo'limni tanlang — har birining vazifasi quyida yozilgan."));
        sub.addClassName("sh-page-sub");
        Div titleBox = new Div(title, sub);
        titleBox.addClassName("sh-title-box");
        Div header = new Div(titleBox);
        header.addClassName("sh-header");

        add(crumbs, header);

        List<AppSection> sections = sectionsOf(module);
        Div grid = new Div();
        grid.addClassName("ov-grid");

        for (AppSection s : sections) {
            grid.add(card(company, s));
        }
        add(grid);
    }

    private Div card(String company, AppSection s) {
        String[] g = SectionGuide.of(s);

        Span ico = new Span(s.title().isBlank() ? "•" : s.title().substring(0, 1).toUpperCase(Locale.ROOT));
        ico.addClassName("ov-ico");
        ico.getElement().setAttribute("data-icon", resolveIcon(s));
        ico.getElement().setAttribute("aria-hidden", "true");

        Span tag = new Span();
        tag.addClassName("ov-tag");
        if (s.isReport()) {
            tag.setText(tx("ov.report", "Hisobot"));
        } else {
            int n = summaryService.countSectionRecords(company, s);
            tag.setText(n == 0 ? tx("ov.empty", "Hali bo'sh") : n + " " + tx("ov.records", "ta yozuv"));
            tag.setClassName("is-empty", n == 0);
        }

        Div top = new Div(ico, tag);
        top.addClassName("ov-top");

        H3 h = new H3(s.title());
        Paragraph what = new Paragraph(g.length > 0 && !g[0].isBlank() ? g[0] : s.description());
        what.addClassName("ov-what");

        Anchor open = new Anchor("/app/" + module.key() + "/" + s.slug(), tx("ov.open", "Ochish") + "  →");
        open.addClassName("ov-open");

        Div card = new Div(top, h, what, open);
        card.addClassName("ov-card");
        return card;
    }

    private String resolveIcon(AppSection s) {
        String probe = (s.slug() + " " + s.key() + " " + s.title() + " " + module.key()).toLowerCase(Locale.ROOT);
        if (probe.contains("kiyim") || probe.contains("cloth") || probe.contains("apparel") || probe.contains("wardrobe")) {
            return "shirt";
        }
        if (probe.contains("qoldiq") || probe.contains("stock") || probe.contains("balance") || probe.contains("ostatok")) {
            return "layers";
        }
        if (probe.contains("kirim") || probe.contains("income") || probe.contains("incoming") || probe.contains("prihod") || probe.contains("receipt")) {
            return "in";
        }
        if (probe.contains("chiqim") || probe.contains("outgo") || probe.contains("outgoing") || probe.contains("expense") || probe.contains("rashod") || probe.contains("writeoff")) {
            return "out";
        }
        if (probe.contains("inventar") || probe.contains("audit") || probe.contains("rekom") || probe.contains("check")) {
            return "check";
        }
        if (probe.contains("yetkazib") || probe.contains("supplier") || probe.contains("delivery") || probe.contains("postav")) {
            return "truck";
        }
        if (probe.contains("moliya") || probe.contains("finance") || probe.contains("kassa") || probe.contains("invoice") || probe.contains("pay") || probe.contains("wallet")) {
            return "wallet";
        }
        if (probe.contains("crm") || probe.contains("mijoz") || probe.contains("client") || probe.contains("customer") || probe.contains("kadr") || probe.contains("staff") || probe.contains("hr")) {
            return "users";
        }
        if (s.isReport() || probe.contains("hisobot") || probe.contains("report") || probe.contains("bi") || probe.contains("analit")) {
            return "chart";
        }
        return "box";
    }

    private static String tx(String key, String fallback) {
        String v = t(key);
        return (v == null || v.isBlank() || v.equals(key)) ? fallback : v;
    }

    @Override
    public String getPageTitle() {
        return module == null ? "DOKONIX" : "DOKONIX — " + module.title();
    }
}