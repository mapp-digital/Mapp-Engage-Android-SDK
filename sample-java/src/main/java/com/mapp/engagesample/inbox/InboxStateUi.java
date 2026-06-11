package com.mapp.engagesample.inbox;

import com.appoxee.shared.InboxMessage;

import java.util.ArrayList;
import java.util.List;

public class InboxStateUi {
    private boolean loading = false;
    private final List<InboxMessage> messages = new ArrayList<>();
    private String error = null;

    public InboxStateUi(boolean loading) {
        this.loading = loading;
    }

    public InboxStateUi(List<InboxMessage> messages) {
        this.messages.clear();
        this.messages.addAll(new ArrayList<>(messages));
    }

    public InboxStateUi(String error) {
        this.error = error;
    }

    public boolean isLoading() {
        return loading;
    }

    public List<InboxMessage> getMessages() {
        return messages;
    }

    public String getError() {
        return error;
    }
}
