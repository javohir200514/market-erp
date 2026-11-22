package com.universal.erp.platform.localization;

import com.vaadin.flow.i18n.I18NProvider;
import org.springframework.stereotype.Component;

import java.text.MessageFormat;
import java.util.List;
import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

/**
 * Central localization for every Vaadin view (uz, ru, en). Views never contain user-facing text:
 * they call {@code getTranslation("some.key")}. Texts live in {@code src/main/resources/i18n/messages_*.properties}.
 *
 * The first locale of {@link #getProvidedLocales()} is the default when the browser language is not supported.
 */
@Component
public class ErpI18NProvider implements I18NProvider {

    public static final Locale UZ = Locale.forLanguageTag("uz");
    public static final Locale RU = Locale.forLanguageTag("ru");
    public static final Locale EN = Locale.ENGLISH;

    private static final String BASE_NAME = "i18n.messages";
    private static final List<Locale> LOCALES = List.of(UZ, RU, EN);

    @Override
    public List<Locale> getProvidedLocales() {
        return LOCALES;
    }

    @Override
    public String getTranslation(String key, Locale locale, Object... params) {
        ResourceBundle bundle = ResourceBundle.getBundle(BASE_NAME,
                locale == null ? UZ : locale,
                ResourceBundle.Control.getNoFallbackControl(ResourceBundle.Control.FORMAT_PROPERTIES));
        String pattern;
        try {
            pattern = bundle.getString(key);
        } catch (MissingResourceException e) {
            return "!" + key;
        }
        // MessageFormat treats ' specially, so it is applied only to texts that really take parameters.
        return params == null || params.length == 0 ? pattern : MessageFormat.format(pattern, params);
    }
}