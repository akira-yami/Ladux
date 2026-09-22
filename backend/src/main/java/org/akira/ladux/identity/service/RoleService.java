package org.akira.ladux.identity.service;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.akira.ladux.identity.dto.response.RoleResponse;
import org.akira.ladux.identity.mapper.RoleMapper;
import org.akira.ladux.identity.repository.RoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RoleService {
    private final RoleRepository roleRepository;

    @Transactional(readOnly = true)
    public List<RoleResponse> getAllRoles() {
        return roleRepository.findAll().stream().map(RoleMapper::fromEntity).toList();
    }
}
