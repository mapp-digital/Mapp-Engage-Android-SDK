package com.mapp.engagesample.inbox;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.appoxee.Appoxee;
import com.appoxee.shared.InboxMessage;
import com.appoxee.shared.MessageStatus;

import java.util.ArrayList;
import java.util.List;

public class InboxViewModel extends ViewModel {

    private final List<InboxMessage> internalMessages = new ArrayList<>();
    private final MutableLiveData<InboxStateUi> state = new MutableLiveData<>(new InboxStateUi(false));
    public LiveData<InboxStateUi> stateUi = state;

    public InboxViewModel() {
        state.setValue(new InboxStateUi(true));
        Appoxee.instance().fetchInboxMessages().enqueue(result -> {
            if (result.isSuccess() && result.getData() != null) {
                List<InboxMessage> messages = result.getData().getMessages();
                state.setValue(new InboxStateUi(updateMessages(messages)));
            } else {
                Throwable t = result.getError();
                String error = (t != null && t.getMessage() != null) ? t.getMessage() : "Unknown error";
                state.setValue(new InboxStateUi(error));
            }
        });
    }

    public void updateStatus(InboxMessage message, int index, MessageStatus messageStatus) {
        Appoxee.instance().updateInboxMessageStatus(message, messageStatus).enqueue(result -> {
            if (result.isSuccess()) {
                InboxMessage updatedMessage = message.setStatus(messageStatus);
                internalMessages.set(index, updatedMessage);
                InboxStateUi inboxState = new InboxStateUi(internalMessages);
                state.setValue(inboxState);
            }
        });
    }

    private List<InboxMessage> updateMessages(List<InboxMessage> messages) {
        internalMessages.clear();
        if (messages != null && !messages.isEmpty()) {
            internalMessages.addAll(messages);
        }
        return internalMessages;
    }
}
