package com.finatrackapp.service;

import com.finatrackapp.dto.request.UpdateProfileRequest;
import com.finatrackapp.dto.response.UserResponse;
import com.finatrackapp.exception.ResourceNotFoundException;
import com.finatrackapp.model.User;
import com.finatrackapp.repository.UserRepository;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public User getUserByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User tidak ditemukan"));
    }

    public UserResponse getProfile(String email) {
        User user = getUserByEmail(email);
        return new UserResponse(user.getId(), user.getFullName(),
                user.getEmail(), user.getCreatedAt());
    }

    public UserResponse updateProfile(String email, UpdateProfileRequest request) {
        User user = getUserByEmail(email);
        user.setFullName(request.getFullName());
        userRepository.save(user);
        return new UserResponse(user.getId(), user.getFullName(),
                user.getEmail(), user.getCreatedAt());
    }
}
