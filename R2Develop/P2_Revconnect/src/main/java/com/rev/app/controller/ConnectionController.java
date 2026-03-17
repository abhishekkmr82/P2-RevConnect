package com.rev.app.controller;

import com.rev.app.entity.Connection;
import com.rev.app.entity.User;
import com.rev.app.service.ConnectionService;
import com.rev.app.service.NotificationService;
import com.rev.app.service.UserService;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/connections")
public class ConnectionController {

    private static final Logger logger = LogManager.getLogger(ConnectionController.class);

    private final ConnectionService connectionService;
    private final UserService userService;
    private final NotificationService notificationService;

    public ConnectionController(ConnectionService connectionService,
            UserService userService,
            NotificationService notificationService) {
        this.connectionService = connectionService;
        this.userService = userService;
        this.notificationService = notificationService;
    }

    @GetMapping
    public String connections(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());

        List<User> connections = connectionService.getConnections(currentUser);

        model.addAttribute("connections", connections);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(currentUser.getId()));
        return "connections";
    }

    @PostMapping("/request/{userId}")
    public String sendRequest(@PathVariable Long userId,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        User target = userService.findById(userId);
        try {
            connectionService.sendRequest(currentUser, target);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Connection request sent to " + target.getUsername());
        } catch (IllegalStateException e) {
            redirectAttributes.addFlashAttribute("errorMessage", e.getMessage());
        }
        return "redirect:/profile/" + target.getUsername();
    }

    // Pending/Accept/Reject functionality removed as connections are now instant.

    @PostMapping("/remove/{targetUserId}")
    public String remove(@PathVariable Long targetUserId,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        User target = userService.findById(targetUserId);
        connectionService.removeByUsers(currentUser, target);
        redirectAttributes.addFlashAttribute("successMessage", "Connection removed.");
        return "redirect:/connections";
    }
}
