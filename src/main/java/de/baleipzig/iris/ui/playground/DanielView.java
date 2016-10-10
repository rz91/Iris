package de.baleipzig.iris.ui.playground;

import com.vaadin.navigator.View;
import com.vaadin.navigator.ViewChangeListener;
import com.vaadin.spring.annotation.SpringView;
import com.vaadin.spring.annotation.UIScope;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

import javax.annotation.PostConstruct;

@UIScope
@SpringView(name = DanielView.VIEW_NAME)
public class DanielView extends VerticalLayout implements View {
    public static final String VIEW_NAME = "daniel";

    @PostConstruct
    void init() {
        this.addComponent(new Label("Daniels Spielwiese"));
    }

    @Override
    public void enter(ViewChangeListener.ViewChangeEvent event) {

    }
}
