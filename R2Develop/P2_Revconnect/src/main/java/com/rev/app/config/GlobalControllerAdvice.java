package com.rev.app.config;

import com.rev.app.entity.User;
import com.rev.app.service.NotificationService;
import com.rev.app.service.UserService;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalControllerAdvice {

    private final UserService userService;
    private final NotificationService notificationService;

    public GlobalControllerAdvice(UserService userService, NotificationService notificationService) {
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @ModelAttribute("currentUser")
    public User currentUser(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return null;
        }
        try {
            return userService.findByUsername(userDetails.getUsername());
        } catch (Exception e) {
            return null;
        }
    }

    @ModelAttribute("unreadCount")
    public Long unreadCount(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return 0L;
        }
        try {
            User user = userService.findByUsername(userDetails.getUsername());
            return notificationService.getUnreadCount(user.getId());
        } catch (Exception e) {
            return 0L;
        }
    }
}
