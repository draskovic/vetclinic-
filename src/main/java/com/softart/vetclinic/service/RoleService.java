package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.Role;
import com.softart.vetclinic.exception.DuplicateResourceException;
import com.softart.vetclinic.repository.RoleRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class RoleService extends AbstractCrudService<Role, RoleRepository> {

    private final RoleRepository roleRepository;

    public RoleService(RoleRepository roleRepository) {
        super(roleRepository);
        this.roleRepository = roleRepository;
    }

    @Override
    protected String getEntityName() {
        return "Role";
    }

    @Override
    protected Optional<Role> findByIdAndClinicId(UUID id, UUID clinicId) {
        return roleRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<Role> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return roleRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return roleRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(Role entity) {
        if (roleRepository.existsByClinicIdAndNameAndDeletedFalse(entity.getClinicId(), entity.getName())) {
            throw new DuplicateResourceException("Role", "name", entity.getName());
        }
    }

    @Transactional(readOnly = true)
    public Optional<Role> findByName(UUID clinicId, String name) {
        return roleRepository.findByClinicIdAndName(clinicId, name);
    }
}
