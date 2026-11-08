
package org.example.documind.config;

import org.example.documind.dao.RoleRepository;
import org.example.documind.domain.entity.Role;
import org.example.documind.domain.enums.RoleName;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class RoleDataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;

    public RoleDataInitializer(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public void run(String... args) {

        if (roleRepository.findByName(RoleName.ROLE_USER).isEmpty()) {

            Role userRole = new Role();
            userRole.setName(RoleName.ROLE_USER);

            roleRepository.save(userRole);

            System.out.println("ROLE_USER created successfully.");
        } else {
            System.out.println("ROLE_USER already exists.");
        }
    }
}
