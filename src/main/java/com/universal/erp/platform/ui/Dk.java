package com.universal.erp.platform.ui;

import java.math.BigDecimal;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.html.Input;
import com.vaadin.flow.component.html.NativeButton;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.data.value.ValueChangeMode;

public final class Dk {

    private Dk() {
    }

    public static Input input(String type, String placeholder, String value) {
        Input i = new Input();
        i.setType(type);
        i.setPlaceholder(placeholder);
        i.addClassName("dk-input");
        i.setValueChangeMode(ValueChangeMode.EAGER);
        i.setValue(value == null ? "" : value);
        i.getElement().setAttribute("autocomplete", "off");
        if ("number".equals(type)) {
            i.getElement().setAttribute("min", "0");
            i.getElement().setAttribute("step", "any");
            i.getElement().setAttribute("inputmode", "decimal");
        }
        return i;
    }

    public static BigDecimal num(String s) {
        if (s == null || s.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(s.trim().replace(" ", "").replace(',', '.'));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static String plain(BigDecimal v) {
        return v.stripTrailingZeros().toPlainString();
    }

    public static Div field(String label, Component c) {
        Span l = new Span(label);
        l.addClassName("dk-label");
        Div d = new Div(l, c);
        d.addClassName("dk-field");
        return d;
    }

    public static Div row(Component... c) {
        Div d = new Div(c);
        d.addClassName("dk-row");
        return d;
    }

    public static NativeButton btn(String text, String cls) {
        NativeButton b = new NativeButton(text);
        b.addClassNames("dk-btn", cls);
        return b;
    }

    public static Div thumb(String img, String name) {
        Div box = new Div();
        box.addClassName("dk-thumb");
        if (img != null && !img.isBlank()) {
            box.add(new Image(img, ""));
        } else {
            String s = name == null || name.isBlank() ? "?" : name.trim().substring(0, 1).toUpperCase();
            box.add(new Span(s));
        }
        return box;
    }

    public static void fail(Div err, String msg) {
        err.setText(msg);
        err.setVisible(true);
    }

    public static Div errorBox() {
        Div err = new Div();
        err.addClassName("dk-error");
        err.setVisible(false);
        return err;
    }

    public static void modal(Div host, String title, Component body, Component foot) {
        NativeButton x = new NativeButton("✕");
        x.addClassName("dk-x");
        x.addClickListener(e -> host.removeAll());
        Span t = new Span(title);
        t.addClassName("dk-modal-title");
        Div head = new Div(t, x);
        head.addClassName("dk-modal-head");
        Div b = new Div(body);
        b.addClassName("dk-modal-body");
        Div f = new Div(foot);
        f.addClassName("dk-modal-foot");
        Div card = new Div(head, b, f);
        card.addClassName("dk-card");
        Div back = new Div(card);
        back.addClassName("dk-backdrop");
        host.removeAll();
        host.add(back);
    }
}