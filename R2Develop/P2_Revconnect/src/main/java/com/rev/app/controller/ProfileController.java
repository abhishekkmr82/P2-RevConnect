package com.rev.app.controller;

import com.rev.app.dto.ProductDTO;
import com.rev.app.dto.ProfileUpdateDTO;
import com.rev.app.entity.Product;
import com.rev.app.entity.User;
import com.rev.app.entity.UserLink;
import com.rev.app.repository.ProductRepository;
import com.rev.app.repository.UserLinkRepository;
import com.rev.app.service.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    private static final Logger logger = LogManager.getLogger(ProfileController.class);

    private final UserService userService;
    private final PostService postService;
    private final ConnectionService connectionService;
    private final FollowService followService;
    private final NotificationService notificationService;
    private final UserLinkRepository userLinkRepository;
    private final ProductRepository productRepository;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public ProfileController(UserService userService, PostService postService,
            ConnectionService connectionService, FollowService followService,
            NotificationService notificationService,
            UserLinkRepository userLinkRepository,
            ProductRepository productRepository) {
        this.userService = userService;
        this.postService = postService;
        this.connectionService = connectionService;
        this.followService = followService;
        this.notificationService = notificationService;
        this.userLinkRepository = userLinkRepository;
        this.productRepository = productRepository;
    }

    @GetMapping
    public String myProfile(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        return viewProfile(userDetails.getUsername(), userDetails, model);
    }

    @GetMapping("/{username}/followers")
    public String viewFollowers(@PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User profileUser = userService.findByUsername(username);
        User currentUser = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("title", username + "'s Followers");
        model.addAttribute("users", followService.getFollowers(profileUser.getId()));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(currentUser.getId()));
        return "user-list";
    }

    @GetMapping("/{username}/following")
    public String viewFollowing(@PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User profileUser = userService.findByUsername(username);
        User currentUser = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("title", "Following by " + username);
        model.addAttribute("users", followService.getFollowing(profileUser.getId()));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(currentUser.getId()));
        return "user-list";
    }

    @GetMapping("/{username}/network")
    public String viewConnections(@PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User profileUser = userService.findByUsername(username);
        User currentUser = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("title", username + "'s Network");
        model.addAttribute("users", connectionService.getConnections(profileUser));
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(currentUser.getId()));
        return "user-list";
    }

    @GetMapping("/{username}")
    public String viewProfile(@PathVariable String username,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User profileUser = userService.findByUsername(username);
        User currentUser = userService.findByUsername(userDetails.getUsername());

        boolean isOwnProfile = currentUser.getId().equals(profileUser.getId());
        boolean isConnected = connectionService.areConnected(currentUser.getId(), profileUser.getId());
        boolean isFollowing = followService.isFollowing(currentUser.getId(), profileUser.getId());

        // Private profile visibility
        boolean canViewPosts = isOwnProfile || profileUser.getPrivacySetting() == User.PrivacySetting.PUBLIC
                || isConnected;

        model.addAttribute("profileUser", profileUser);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isOwnProfile", isOwnProfile);
        model.addAttribute("isConnected", isConnected);
        model.addAttribute("isFollowing", isFollowing);
        model.addAttribute("canViewPosts", canViewPosts);
        model.addAttribute("posts", canViewPosts ? postService.getUserPosts(profileUser.getId()) : null);
        model.addAttribute("followerCount", followService.countFollowers(profileUser.getId()));
        model.addAttribute("followingCount", followService.countFollowing(profileUser.getId()));
        model.addAttribute("connectionCount", connectionService.getConnections(profileUser).size());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(currentUser.getId()));
        model.addAttribute("connections", connectionService.getConnections(currentUser));
        // External links & products
        model.addAttribute("userLinks", userLinkRepository.findByUserIdOrderByIdAsc(profileUser.getId()));
        model.addAttribute("products", productRepository.findByUserIdOrderByCreatedAtDesc(profileUser.getId()));
        return "profile";
    }

    @GetMapping("/edit")
    public String editProfileForm(@AuthenticationPrincipal UserDetails userDetails, Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        ProfileUpdateDTO dto = new ProfileUpdateDTO();
        dto.setFullName(currentUser.getFullName());
        dto.setBio(currentUser.getBio());
        dto.setLocation(currentUser.getLocation());
        dto.setWebsite(currentUser.getWebsite());
        dto.setCategory(currentUser.getCategory());
        dto.setContactInfo(currentUser.getContactInfo());
        dto.setBusinessAddress(currentUser.getBusinessAddress());
        dto.setBusinessHours(currentUser.getBusinessHours());
        dto.setPrivacySetting(currentUser.getPrivacySetting().name());
        model.addAttribute("profileDTO", dto);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("privacyOptions", User.PrivacySetting.values());
        model.addAttribute("userLinks", userLinkRepository.findByUserIdOrderByIdAsc(currentUser.getId()));
        model.addAttribute("products", productRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId()));
        model.addAttribute("linkTypes", UserLink.LinkType.values());
        model.addAttribute("unreadCount", notificationService.getUnreadCount(currentUser.getId()));
        return "profile-edit";
    }

    @PostMapping("/edit")
    public String updateProfile(@AuthenticationPrincipal UserDetails userDetails,
            @ModelAttribute ProfileUpdateDTO profileDTO,
            RedirectAttributes redirectAttributes) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        userService.updateProfile(currentUser.getId(), profileDTO);
        redirectAttributes.addFlashAttribute("successMessage", "Profile updated!");
        return "redirect:/profile";
    }

    @PostMapping("/picture")
    public String uploadPicture(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes redirectAttributes) {
        try {
            User currentUser = userService.findByUsername(userDetails.getUsername());
            userService.uploadProfilePicture(currentUser.getId(), file, uploadDir);
            redirectAttributes.addFlashAttribute("successMessage", "Profile picture updated!");
        } catch (IOException e) {
            redirectAttributes.addFlashAttribute("errorMessage", "Failed to upload image.");
        }
        return "redirect:/profile";
    }

    // ==================== External Links ====================

    @PostMapping("/links")
    public String addLink(@AuthenticationPrincipal UserDetails userDetails,
            @RequestParam String label,
            @RequestParam String url,
            @RequestParam(defaultValue = "OTHER") UserLink.LinkType linkType,
            RedirectAttributes redirectAttributes) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        UserLink link = new UserLink(currentUser, label, url, linkType);
        userLinkRepository.save(link);
        redirectAttributes.addFlashAttribute("successMessage", "Link added!");
        return "redirect:/profile/edit";
    }

    @PostMapping("/links/{id}/delete")
    public String deleteLink(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        userLinkRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Link removed.");
        return "redirect:/profile/edit";
    }

    // ==================== Products/Services ====================

    @PostMapping("/products")
    public String addProduct(@AuthenticationPrincipal UserDetails userDetails,
            @ModelAttribute ProductDTO productDTO,
            RedirectAttributes redirectAttributes) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        Product product = new Product(currentUser, productDTO.getName(), productDTO.getDescription());
        product.setPrice(productDTO.getPrice());
        product.setExternalUrl(productDTO.getExternalUrl());
        productRepository.save(product);
        redirectAttributes.addFlashAttribute("successMessage", "Product added!");
        return "redirect:/profile/edit";
    }

    @PostMapping("/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            RedirectAttributes redirectAttributes) {
        productRepository.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "Product removed.");
        return "redirect:/profile/edit";
    }

    @GetMapping("/search")
    public String searchUsers(@RequestParam String q,
            @AuthenticationPrincipal UserDetails userDetails,
            Model model) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        model.addAttribute("results", userService.searchUsers(q));
        model.addAttribute("query", q);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("unreadCount", notificationService.getUnreadCount(currentUser.getId()));
        return "search-users";
    }

    @PostMapping("/delete")
    public String deleteAccount(@AuthenticationPrincipal UserDetails userDetails,
            jakarta.servlet.http.HttpServletRequest request) {
        User currentUser = userService.findByUsername(userDetails.getUsername());
        userService.deleteUser(currentUser.getId());
        request.getSession().invalidate();
        return "redirect:/login?deleted";
    }

}
