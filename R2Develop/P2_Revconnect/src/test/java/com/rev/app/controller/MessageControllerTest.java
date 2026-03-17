package com.rev.app.controller;

import com.rev.app.entity.Message;
import com.rev.app.entity.User;
import com.rev.app.service.MessageService;
import com.rev.app.service.NotificationService;
import com.rev.app.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageService messageService;

    @MockBean
    private UserService userService;

    @MockBean
    private NotificationService notificationService;

    @Test
    @WithMockUser(username = "testuser")
    public void testInbox() throws Exception {
        User user = new User("testuser", "test@example.com", "pass");
        user.setId(1L);
        when(userService.findByUsername(anyString())).thenReturn(user);
        when(messageService.getContacts(anyLong())).thenReturn(Collections.emptyList());
        when(notificationService.getUnreadCount(anyLong())).thenReturn(0L);
        when(messageService.getUnreadCount(anyLong())).thenReturn(0L);

        mockMvc.perform(get("/messages"))
                .andExpect(status().isOk())
                .andExpect(view().name("messages/inbox"));
    }

    @Test
    @WithMockUser(username = "testuser")
    public void testSharePost() throws Exception {
        User sender = new User("testuser", "test@example.com", "pass");
        sender.setId(1L);
        User recipient = new User("otheruser", "other@example.com", "pass");
        recipient.setId(2L);

        when(userService.findByUsername("testuser")).thenReturn(sender);
        when(messageService.sendMessage(any(User.class), eq(2L), anyString()))
                .thenReturn(new Message(sender, recipient, "Check out this post: http://localhost:8082/posts/10"));

        mockMvc.perform(post("/messages/share")
                .with(csrf())
                .param("postId", "10")
                .param("recipientId", "2"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/feed"))
                .andExpect(flash().attribute("successMessage", "Post shared via message successfully!"));

        verify(messageService).sendMessage(any(User.class), eq(2L), contains("posts/10"));
    }
}
