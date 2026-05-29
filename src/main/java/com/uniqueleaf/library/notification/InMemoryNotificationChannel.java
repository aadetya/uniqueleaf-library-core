package com.uniqueleaf.library.notification;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InMemoryNotificationChannel implements NotificationChannel {
    private final List<NotificationMessage> sentMessages = new ArrayList<>();

    @Override
    public void send(NotificationMessage message) {
        sentMessages.add(message);
    }

    @Override
    public List<NotificationMessage> sentMessages() {
        return Collections.unmodifiableList(sentMessages);
    }
}
