package com.r42914lg.arkados.vitalk.model;

public class TerminateDialogEvent {
    private final String title;
    private final String text;

    public TerminateDialogEvent(String title, String text) {
        this.title = title;
        this.text = text;
    }

    public String getTitle() {
        return title;
    }
    public String getText() {
        return text;
    }
}
