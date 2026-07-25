package com.intellimail.mail.mapper;

import com.intellimail.mail.dto.user.UpdateProfileRequest;
import com.intellimail.mail.dto.user.UserProfileResponse;
import com.intellimail.mail.entity.Role;
import com.intellimail.mail.entity.User;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.Named;

import java.util.Set;
import java.util.stream.Collectors;

@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(target = "roles", source = "roles", qualifiedByName = "rolesToNames")
    UserProfileResponse toProfileResponse(User user);

    void updateFromRequest(UpdateProfileRequest request, @MappingTarget User user);

    @Named("rolesToNames")
    default Set<String> rolesToNames(Set<Role> roles) {
        if (roles == null) {
            return Set.of();
        }
        return roles.stream()
                .map(role -> role.getName().name())
                .collect(Collectors.toUnmodifiableSet());
    }
}
