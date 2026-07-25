package com.intellimail.mail.mapper;

import com.intellimail.mail.dto.user.UpdateProfileRequest;
import com.intellimail.mail.dto.user.UserProfileResponse;
import com.intellimail.mail.entity.Role;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.RoleName;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class UserMapperTest {

    private final UserMapper userMapper = Mappers.getMapper(UserMapper.class);

    @Test
    void toProfileResponse_mapsRolesToNameStrings() {
        User user = User.builder()
                .fullName("Ada Lovelace")
                .email("ada@intellimail.com")
                .password("hashed")
                .roles(Set.of(Role.builder().name(RoleName.ROLE_ADMIN).build()))
                .build();

        UserProfileResponse response = userMapper.toProfileResponse(user);

        assertThat(response.fullName()).isEqualTo("Ada Lovelace");
        assertThat(response.email()).isEqualTo("ada@intellimail.com");
        assertThat(response.roles()).containsExactly("ROLE_ADMIN");
    }

    @Test
    void updateFromRequest_onlyChangesFullName() {
        User user = User.builder()
                .fullName("Old Name")
                .email("keep@intellimail.com")
                .password("hashed")
                .build();

        userMapper.updateFromRequest(new UpdateProfileRequest("New Name"), user);

        assertThat(user.getFullName()).isEqualTo("New Name");
        assertThat(user.getEmail()).isEqualTo("keep@intellimail.com");
        assertThat(user.getPassword()).isEqualTo("hashed");
    }
}
