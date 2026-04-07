package com.softart.vetclinic.config.security;

import com.softart.vetclinic.entity.User;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Getter
public class CustomUserDetails implements UserDetails {

    /**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private final UUID userId;
    private final UUID clinicId;
    private final String email;
    private final String passwordHash;
    private final boolean active;
    private final String roleName;
    private final String permissions;

    public CustomUserDetails(User user) {
        this.userId = user.getId();
        this.clinicId = user.getClinicId();
        this.email = user.getEmail();
        this.passwordHash = user.getPasswordHash();
        this.active = user.getActive() != null && user.getActive();
        this.roleName = user.getRole() != null ? user.getRole().getName() : "";
        this.permissions = user.getRole() != null ? user.getRole().getPermissions() : "[]";
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleName));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
