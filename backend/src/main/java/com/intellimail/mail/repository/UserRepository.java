package com.intellimail.mail.repository;

import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.OrgRole;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    Optional<User> findByGoogleSubjectId(String googleSubjectId);

    Page<User> findByOrganizationId(UUID organizationId, Pageable pageable);

    long countByOrganizationIdAndOrgRole(UUID organizationId, OrgRole orgRole);
}
