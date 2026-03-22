package com.sahil.notification.provider;

import com.sahil.notification.model.NotificationEvent;

public interface NotificationProvider {

    void send(NotificationEvent event);
}
