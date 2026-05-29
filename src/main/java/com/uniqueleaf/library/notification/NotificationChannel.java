package com.uniqueleaf.library.notification;

import java.util.List;

public interface NotificationChannel {
    void send(NotificationMessage message);

    List<NotificationMessage> sentMessages();
}
