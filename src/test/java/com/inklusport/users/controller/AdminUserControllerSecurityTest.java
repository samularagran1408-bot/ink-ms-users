package com.inklusport.users.controller;

import com.inklusport.users.config.JwtAuthenticationFilter;
import com.inklusport.users.config.SecurityConfig;
import com.inklusport.users.dto.UserProfileResponse;
import com.inklusport.users.exception.GlobalExceptionHandler;
import com.inklusport.users.security.JwtTokenProvider;
import com.inklusport.users.service.RoleService;
import com.inklusport.users.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AdminUserController.class)
@Import({SecurityConfig.class, JwtAuthenticationFilter.class, JwtTokenProvider.class,
        GlobalExceptionHandler.class, AdminUserControllerSecurityTest.JacksonTestConfig.class})
@TestPropertySource(properties = {
        "jwt.secret=inklusport2024superSecretKeyForJWTtokenGenerationWith512bitsAlgorithmHS512",
        "jwt.expiration=86400000"
})
class AdminUserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private UserService userService;

    @MockBean
    private RoleService roleService;

    @Test
    void getAllUsers_conTokenAdmin_devuelve200() throws Exception {
        when(userService.getAllUsers()).thenReturn(List.of(
                UserProfileResponse.builder().email("a@test.com").fullName("Admin User").build()
        ));

        String token = jwtTokenProvider.generateToken("admin@test.com", List.of("ADMIN"));

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("a@test.com"));
    }

    @Test
    void getAllUsers_conTokenUsuario_devuelve403() throws Exception {
        String token = jwtTokenProvider.generateToken("user@test.com", List.of("USUARIO"));

        mockMvc.perform(get("/api/admin/users")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("Acceso denegado"));
    }

    @Test
    void getAllUsers_sinToken_devuelve401() throws Exception {
        mockMvc.perform(get("/api/admin/users"))
                .andExpect(status().isUnauthorized());
    }

    @TestConfiguration
    static class JacksonTestConfig {
        @Bean
        ObjectMapper objectMapper() {
            return new ObjectMapper().registerModule(new JavaTimeModule());
        }
    }
}
