package org.akira.ladux.identity.security;

import java.util.Collection;
import java.util.List;
import org.akira.ladux.identity.entity.User;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

// Wrapper Spring Security cho User — dung trong @AuthenticationPrincipal.
// Map role -> ROLE_ADMIN / ROLE_CUSTOMER / ROLE_STAFF_SALES / ROLE_STAFF_INVENTORY. isEnabled() = user.isActive().
// getTokenVersion() so khop claim JWT de thu hoi access token tuc thi.
public class UserPrincipal implements UserDetails {
    private final User user;
    private final List<GrantedAuthority> authorities;

    public UserPrincipal(User user) {
        this.user = user;
        this.authorities = user.getRoles() == null
                ? List.of()
                : user.getRoles().stream()
                        .<GrantedAuthority>map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName().name()))
                        .toList();
    }

    public Integer getId() {
        return user.getId();
    }

    public int getTokenVersion() {
        return user.getTokenVersion();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public @Nullable String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }
    @Override
    public boolean isAccountNonLocked() {
        return user.isActive();
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.isActive();
    }
}
