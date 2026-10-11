package org.akira.ladux.identity.service;

import jakarta.servlet.http.HttpServletRequest;
import org.akira.ladux.identity.dto.request.LoginRequest;
import org.akira.ladux.identity.entity.User;

public interface LocalLoginService {

    User verifyPassword(LoginRequest request, boolean adminLogin, HttpServletRequest servletRequest);

}
