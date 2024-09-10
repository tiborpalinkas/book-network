package com.tibor.book.network.email;

public enum EmailTemplateName {

    ACTIVATE_ACCOUNT("activate_account");

    private final String name;

    EmailTemplateName(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
