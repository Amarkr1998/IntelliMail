package com.intellimail.mail.repository;

import com.intellimail.mail.entity.Role;
import com.intellimail.mail.enums.RoleName;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface RoleRepository extends JpaRepository<Role, UUID> {

    Optional<Role> findByName(RoleName name);
}
