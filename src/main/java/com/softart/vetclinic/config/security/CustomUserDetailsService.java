package com.softart.vetclinic.config.security;

import com.softart.vetclinic.entity.User;
import com.softart.vetclinic.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        throw new UsernameNotFoundException("Use loadUserByClinicIdAndEmail instead");
    }

    @Transactional(readOnly = true)
    public CustomUserDetails loadUserByClinicIdAndEmail(UUID clinicId, String email) {
        User user = userRepository.findByClinicIdAndEmail(clinicId, email)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email: " + email + " in clinic: " + clinicId));

        if (user.getDeleted() != null && user.getDeleted()) {
            throw new UsernameNotFoundException("User account has been deleted");
        }

        return new CustomUserDetails(user);
    }

    @Transactional(readOnly = true)
    public CustomUserDetails loadUserById(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        if (user.getDeleted() != null && user.getDeleted()) {
            throw new UsernameNotFoundException("User account has been deleted");
        }

        return new CustomUserDetails(user);
    }
}
