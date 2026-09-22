package org.akira.ladux.identity.service.impl;

import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.entity.User;
import org.akira.ladux.identity.repository.UserRepository;
import org.akira.ladux.identity.security.UserPrincipal;
import org.akira.ladux.identity.service.MyUserDetailsService;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// Load user tu DB cho Spring Security — goi boi JwtFilter va AuthenticationManager khi login.
@RequiredArgsConstructor
@Service
public class MyUserDetailsServiceImpl implements MyUserDetailsService {
    private final UserRepository repo;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = repo.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        return new UserPrincipal(user);
    }
}
