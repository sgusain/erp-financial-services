package com.erp.notification.service;

import com.erp.common.exception.ResourceNotFoundException;
import com.erp.notification.entity.Notification;
import com.erp.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;

    public Page<Notification> getAllNotifications(Pageable pageable) {
        return notificationRepository.findAll(pageable);
    }

    public Notification getNotificationById(Long id) {
        return notificationRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found with id: " + id));
    }

    public Notification createNotification(Notification notification) {
        return notificationRepository.save(notification);
    }

    public Notification updateNotification(Long id, Notification notificationDetails) {
        Notification notification = getNotificationById(id);
        notification.setUserId(notificationDetails.getUserId());
        notification.setMessage(notificationDetails.getMessage());
        notification.setType(notificationDetails.getType());
        notification.setIsRead(notificationDetails.getIsRead());
        return notificationRepository.save(notification);
    }

    public void deleteNotification(Long id) {
        Notification notification = getNotificationById(id);
        notificationRepository.delete(notification);
    }

}
