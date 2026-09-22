package org.akira.ladux.identity.security;

import lombok.experimental.UtilityClass;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import org.akira.ladux.shared.exception.UnauthenticatedException; // Hoặc exception tự định nghĩa của Khang

@UtilityClass
public class SecurityUtils {

    /**
     * Lấy ID của User đang đăng nhập từ Security Context
     * @return Integer userId
     * @throws UnauthenticatedException nếu chưa đăng nhập
     */
    public static Integer getCurrentUserId() {
       Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
       if (authentication == null || !authentication.isAuthenticated() || authentication instanceof AnonymousAuthenticationToken) {
           throw new UnauthenticatedException("Người dùng chưa đăng nhập!");
       }
       Object principal = authentication.getPrincipal();
       if (principal instanceof UserPrincipal) {
           UserPrincipal userPrincipal = (UserPrincipal) principal;
           return userPrincipal.getId();
       }
       else {
           throw new UnauthenticatedException("Người dùng chưa đăng nhập!");
       }
    }
}