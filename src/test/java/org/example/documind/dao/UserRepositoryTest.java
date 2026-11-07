package org.example.documind.dao;

import org.example.documind.domain.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
class UserRepositoryTest {

    @Autowired
    private UserRepository userRepository;

    @Test
    void testSaveAndFindUser() {
        User user = User.builder()
                .username("karim")
                .email("karim@example.com")
                .password("hashedpassword")
                .build();

        User savedUser = userRepository.save(user);
        assertThat(savedUser.getId()).isNotNull();

        Optional<User> found = userRepository.findByUsername("karim");
        assertThat(found).isPresent();
        assertThat(found.get().getEmail()).isEqualTo("karim@example.com");
    }
}