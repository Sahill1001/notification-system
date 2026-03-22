package com.sahil.notification.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent {

    private Long id;
    private Long userId;
    private String channel;
    private String message;
    private String status;
}
