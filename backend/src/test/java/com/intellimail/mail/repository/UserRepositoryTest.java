package com.intellimail.mail.repository;

import com.intellimail.mail.entity.Role;
import com.intellimail.mail.entity.User;
import com.intellimail.mail.enums.RoleName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Test
    void findByEmail_returnsUserWithRoles_whenEmailExists() {
        Role role = roleRepository.save(Role.builder().name(RoleName.ROLE_USER).description("Standard user").build());
        User user = User.builder()
                .fullName("Ada Lovelace")
                .email("ada@intellimail.com")
                .password("hashed-password")
                .roles(Set.of(role))
                .build();
        userRepository.save(user);

        Optional<User> found = userRepository.findByEmail("ada@intellimail.com");

        assertThat(found).isPresent();
        assertThat(found.get().getFullName()).isEqualTo("Ada Lovelace");
        assertThat(found.get().getRoles()).extracting(Role::getName).containsExactly(RoleName.ROLE_USER);
    }

    @Test
    void findByEmail_returnsEmpty_whenEmailDoesNotExist() {
        assertThat(userRepository.findByEmail("missing@intellimail.com")).isEmpty();
    }

    @Test
    void existsByEmail_reflectsPersistedState() {
        assertThat(userRepository.existsByEmail("new@intellimail.com")).isFalse();

        userRepository.save(User.builder()
                .fullName("Grace Hopper")
                .email("new@intellimail.com")
                .password("hashed-password")
                .build());

        assertThat(userRepository.existsByEmail("new@intellimail.com")).isTrue();
    }
}
