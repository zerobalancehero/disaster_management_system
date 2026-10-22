package com.disaster.response.config;

import com.disaster.response.domain.entity.User;
import com.disaster.response.domain.enums.UserRole;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class UserPrincipal implements UserDetails {

    private final UUID id;
    private final String email;
    private final String password;
    private final UserRole role;
    private final boolean active;
    private final Collection<? extends GrantedAuthority> authorities;

    public UserPrincipal(UUID id, String email, String password, UserRole role, boolean active) {
        this.id = id;
        this.email = email;
        this.password = password;
        this.role = role;
        this.active = active;
        this.authorities = List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    public static UserPrincipal from(User user) {
        return new UserPrincipal(
                user.getId(),
                user.getEmail(),
                user.getPasswordHash(),
                user.getRole(),
                user.isActive()
        );
    }

    public UUID getId() {
        return id;
    }

    public UserRole getRole() {
        return role;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return active;
    }
}
