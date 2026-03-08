package com.finatrackapp.service;

import com.finatrackapp.dto.request.UpdateProfileRequest;
import com.finatrackapp.dto.response.UserResponse;
import com.finatrackapp.exception.ResourceNotFoundException;
import com.finatrackapp.model.User;
import com.finatrackapp.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setFullName("John Doe");
        testUser.setEmail("john@example.com");
        testUser.setCreatedAt(LocalDateTime.now());
    }

    @Test
    @DisplayName("getUserByEmail - berhasil mendapatkan user")
    void getUserByEmail_Success() {
        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(testUser));

        User result = userService.getUserByEmail("john@example.com");

        assertThat(result.getEmail()).isEqualTo("john@example.com");
        assertThat(result.getFullName()).isEqualTo("John Doe");
    }

    @Test
    @DisplayName("getUserByEmail - gagal karena user tidak ditemukan")
    void getUserByEmail_NotFound_ThrowsException() {
        when(userRepository.findByEmail("unknown@example.com"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getUserByEmail("unknown@example.com"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessage("User tidak ditemukan");
    }

    @Test
    @DisplayName("getProfile - berhasil mengambil profil")
    void getProfile_Success() {
        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(testUser));

        UserResponse result = userService.getProfile("john@example.com");

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.fullName()).isEqualTo("John Doe");
        assertThat(result.email()).isEqualTo("john@example.com");
    }

    @Test
    @DisplayName("updateProfile - berhasil memperbarui nama")
    void updateProfile_Success() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFullName("Jane Doe");

        when(userRepository.findByEmail("john@example.com"))
                .thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenAnswer(i -> i.getArgument(0));

        UserResponse result = userService.updateProfile("john@example.com", request);

        assertThat(result.fullName()).isEqualTo("Jane Doe");
        verify(userRepository).save(testUser);
    }
}
