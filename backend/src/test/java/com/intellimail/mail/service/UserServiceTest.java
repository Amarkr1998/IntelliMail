package com.intellimail.mail.service;

import com.intellimail.mail.dto.user.UpdateProfileRequest;
import com.intellimail.mail.dto.user.UserProfileResponse;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.exception.UserNotFoundException;
import com.intellimail.mail.mapper.UserMapper;
import com.intellimail.mail.mapper.UserMapperImpl;
import com.intellimail.mail.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    private final UserMapper userMapper = new UserMapperImpl();

    private UserService userService;
    private User user;

    @BeforeEach
    void setUp() {
        userService = new UserService(userRepository, userMapper);
        user = User.builder().fullName("Original Name").email("user@intellimail.com").password("hashed").build();
        user.setId(UUID.randomUUID());
    }

    @Test
    void getProfile_returnsMappedProfile_whenUserExists() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getProfile(user.getId());

        assertThat(response.email()).isEqualTo("user@intellimail.com");
        assertThat(response.fullName()).isEqualTo("Original Name");
    }

    @Test
    void getProfile_throwsUserNotFound_whenUserDoesNotExist() {
        UUID missingId = UUID.randomUUID();
        when(userRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.getProfile(missingId))
                .isInstanceOf(UserNotFoundException.class);
    }

    @Test
    void updateProfile_appliesNewFullName_andPersists() {
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(userRepository.save(user)).thenReturn(user);

        UserProfileResponse response = userService.updateProfile(user.getId(), new UpdateProfileRequest("New Name"));

        assertThat(response.fullName()).isEqualTo("New Name");
        assertThat(user.getFullName()).isEqualTo("New Name");
        assertThat(user.getEmail()).isEqualTo("user@intellimail.com");
    }

    @Test
    void updateProfile_throwsUserNotFound_whenUserDoesNotExist() {
        UUID missingId = UUID.randomUUID();
        when(userRepository.findById(missingId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.updateProfile(missingId, new UpdateProfileRequest("New Name")))
                .isInstanceOf(UserNotFoundException.class);
    }
}
