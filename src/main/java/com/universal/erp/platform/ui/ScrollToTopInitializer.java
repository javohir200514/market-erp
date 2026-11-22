package com.universal.erp.platform.ui;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

/**
 * Bir sahifadan boshqasiga o'tilganda yangi sahifa eng tepadan boshlanadi.
 * Barcha sahifalar (Home, Login, Register, ilova qobig'i) uchun bitta joyda ishlaydi — layout'larga tegish shart emas.
 * "#features" kabi havola (hash) bilan o'tilganda tepaga otmaydi, o'sha bo'limga boradi.
 * Spring Boot bu klassni avtomatik topadi (@Component).
 */
@Component
public class ScrollToTopInitializer implements VaadinServiceInitListener {

    private static final String SCROLL_TOP_JS = """
            if (!location.hash) {
              requestAnimationFrame(() => {
                window.scrollTo(0, 0);
                document.documentElement.scrollTop = 0;
                document.body.scrollTop = 0;
                // login sahifasi o'z ichida skroll bo'ladi; ilova qobig'i ham shunday bo'lishi mumkin
                document.querySelectorAll('.login-page, .wz-page, .sh-content, .sh-main')
                  .forEach((el) => { el.scrollTop = 0; });
              });
            }
            """;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> {
            var ui = uiEvent.getUI();
            ui.addAfterNavigationListener(nav -> ui.getPage().executeJs(SCROLL_TOP_JS));
        });
    }
}