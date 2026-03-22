package com.sahil.notification.controller;

import com.sahil.notification.entity.Notification;
import jakarta.validation.Valid;
import com.sahil.notification.service.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService service;

    @PostMapping
    public Notification create(@Valid @RequestBody Notification notification){
        return service.createNotification(notification);
    }
}
