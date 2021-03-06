package de.baleipzig.iris.ui;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import javax.annotation.PostConstruct;

@UIScope
@SpringView(name = DefaultView.VIEW_NAME)
public class DefaultView extends VerticalLayout implements View {

    public static final String VIEW_NAME = "notDefault";

    @PostConstruct
    void init() {
        addComponent(new Label("defaultView"));
    }

    private void createTestEntity() {
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {
    }
}
