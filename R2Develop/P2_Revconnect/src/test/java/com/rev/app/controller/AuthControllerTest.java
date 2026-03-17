package com.rev.app.controller;

import com.rev.app.dto.RegisterDTO;
import com.rev.app.service.UserService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @Test
    public void testLoginPage() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    public void testRegisterPage() throws Exception {
        mockMvc.perform(get("/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registerDTO"));
    }

    @Test
    public void testRegisterSuccess() throws Exception {
        org.mockito.Mockito.when(userService.register(any(RegisterDTO.class)))
                .thenReturn(new com.rev.app.entity.User());

        mockMvc.perform(post("/register")
                .param("username", "testuser")
                .param("email", "test@example.com")
                .param("password", "password123")
                .param("fullName", "Test User")
                .param("securityQuestion", "What is your favourite sport?")
                .param("securityAnswer", "cricket")
                .param("role", "PERSONAL")
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    public void testRegisterWithNullValues() throws Exception {
        mockMvc.perform(post("/register")
                .param("username", "")
                .param("email", "")
                .param("password", "")
                .param("securityQuestion", "")
                .param("securityAnswer", "")
                .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeHasFieldErrors("registerDTO", "username", "email", "password",
                        "securityQuestion", "securityAnswer"));
    }

    @Test
    @WithMockUser(username = "testuser")
    public void testHomeRedirectWhenLoggedIn() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/feed"));
    }
}
