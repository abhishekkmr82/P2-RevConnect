package com.rev.app.controller;

import com.rev.app.dto.ForgotPasswordDTO;
import com.rev.app.dto.RegisterDTO;
import com.rev.app.dto.ResetPasswordDTO;
import com.rev.app.entity.User;
import com.rev.app.exception.ResourceNotFoundException;
import com.rev.app.exception.UserAlreadyExistsException;
import com.rev.app.service.UserService;
import jakarta.validation.Valid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class AuthController {

    private static final Logger logger = LogManager.getLogger(AuthController.class);

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/")
    public String home(Authentication authentication) {
        if (authentication != null && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken)) {
            return "redirect:/feed";
        }
        return "index";
    }

    @GetMapping("/login")
    public String loginPage(@RequestParam(required = false) String error,
            @RequestParam(required = false) String logout,
            Model model) {
        if (error != null)
            model.addAttribute("errorMessage", "Invalid username/email or password.");
        if (logout != null)
            model.addAttribute("successMessage", "You have been logged out successfully.");
        return "auth/login";
    }

    @GetMapping("/register")
    public String registerPage(Model model) {
        model.addAttribute("registerDTO", new RegisterDTO());
        model.addAttribute("roles", User.UserRole.values());
        return "auth/register";
    }

    @PostMapping("/register")
    public String register(@Valid @ModelAttribute("registerDTO") RegisterDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            model.addAttribute("roles", User.UserRole.values());
            return "auth/register";
        }
        try {
            userService.register(dto);
            logger.info("AuthController: Registration successful for user: {}. Redirecting to login.",
                    dto.getUsername());
            redirectAttributes.addFlashAttribute("successMessage", "Account created! Please log in.");
            return "redirect:/login";
        } catch (UserAlreadyExistsException ex) {
            logger.warn("AuthController: Registration failed - user already exists: {}", ex.getMessage());
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("roles", User.UserRole.values());
            return "auth/register";
        }
    }

    // ==================== Forgot Password Flow ====================

    @GetMapping("/forgot-password")
    public String forgotPasswordPage(Model model) {
        model.addAttribute("forgotPasswordDTO", new ForgotPasswordDTO());
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String handleForgotPassword(@Valid @ModelAttribute("forgotPasswordDTO") ForgotPasswordDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "auth/forgot-password";
        }
        try {
            String securityQuestion = userService.getSecurityQuestion(dto.getUsernameOrEmail());
            redirectAttributes.addFlashAttribute("securityQuestion", securityQuestion);
            redirectAttributes.addFlashAttribute("usernameOrEmail", dto.getUsernameOrEmail());
            return "redirect:/security-question";
        } catch (ResourceNotFoundException ex) {
            logger.warn("AuthController: Forgot password - user not found: {}", dto.getUsernameOrEmail());
            model.addAttribute("errorMessage", "No account found with that username or email.");
            return "auth/forgot-password";
        }
    }

    @GetMapping("/security-question")
    public String securityQuestionPage(Model model) {
        // Flash attributes are automatically added to the model
        if (!model.containsAttribute("securityQuestion") || !model.containsAttribute("usernameOrEmail")) {
            return "redirect:/forgot-password";
        }
        model.addAttribute("resetPasswordDTO", new ResetPasswordDTO());
        return "auth/security-question";
    }

    @PostMapping("/reset-password")
    public String handleResetPassword(@Valid @ModelAttribute("resetPasswordDTO") ResetPasswordDTO dto,
            BindingResult result,
            Model model,
            RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            try {
                String securityQuestion = userService.getSecurityQuestion(dto.getUsernameOrEmail());
                model.addAttribute("securityQuestion", securityQuestion);
                model.addAttribute("usernameOrEmail", dto.getUsernameOrEmail());
            } catch (ResourceNotFoundException ex) {
                return "redirect:/forgot-password";
            }
            return "auth/security-question";
        }
        try {
            userService.resetPassword(dto);
            logger.info("AuthController: Password reset successful for: {}", dto.getUsernameOrEmail());
            redirectAttributes.addFlashAttribute("successMessage",
                    "Password reset successfully! Please log in with your new password.");
            return "redirect:/login";
        } catch (IllegalArgumentException ex) {
            logger.warn("AuthController: Password reset failed: {}", ex.getMessage());
            try {
                String securityQuestion = userService.getSecurityQuestion(dto.getUsernameOrEmail());
                model.addAttribute("securityQuestion", securityQuestion);
                model.addAttribute("usernameOrEmail", dto.getUsernameOrEmail());
            } catch (ResourceNotFoundException e) {
                return "redirect:/forgot-password";
            }
            model.addAttribute("errorMessage", ex.getMessage());
            model.addAttribute("resetPasswordDTO", dto);
            return "auth/security-question";
        }
    }
}
