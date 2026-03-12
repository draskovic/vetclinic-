package com.softart.vetclinic.service;

import com.softart.vetclinic.dto.UpdateProfileRequest;
import com.softart.vetclinic.entity.User;
import com.softart.vetclinic.exception.DuplicateResourceException;
import com.softart.vetclinic.exception.ResourceNotFoundException;
import com.softart.vetclinic.repository.RoleRepository;
import com.softart.vetclinic.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService extends AbstractCrudService<User, UserRepository> {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    public UserService(UserRepository userRepository, RoleRepository roleRepository) {
        super(userRepository);
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
    }

    @Override
    protected String getEntityName() {
        return "User";
    }

    @Override
    protected Optional<User> findByIdAndClinicId(UUID id, UUID clinicId) {
        return userRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<User> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return userRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return userRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected void validateForCreate(User entity) {
        requireExists(
                roleRepository.existsByIdAndClinicIdAndDeletedFalse(entity.getRoleId(), entity.getClinicId()),
                "Role", "id", entity.getRoleId()
        );
        if (userRepository.existsByClinicIdAndEmailAndDeletedFalse(entity.getClinicId(), entity.getEmail())) {
            throw new DuplicateResourceException("User", "email", entity.getEmail());
        }
    }

    @Transactional(readOnly = true)
    public Optional<User> findByEmail(UUID clinicId, String email) {
        return userRepository.findByClinicIdAndEmail(clinicId, email);
    }
    
    public User updateProfile(UUID userId, UUID clinicId, UpdateProfileRequest request) {
        User user = repository.findByIdAndClinicIdAndDeletedFalse(userId, clinicId)

                .orElseThrow(() -> new ResourceNotFoundException("Korisnik nije pronađen"));
        
        user.setFirstName(request.firstName());
        user.setLastName(request.lastName());
        user.setPhone(request.phone());
        user.setLicenseNumber(request.licenseNumber());
        user.setSpecialization(request.specialization());
        
        return repository.save(user);
    }

    public void changePassword(UUID userId, UUID clinicId, String currentPassword, String newPassword, PasswordEncoder passwordEncoder) {
        User user = repository.findByIdAndClinicIdAndDeletedFalse(userId, clinicId)
                .orElseThrow(() -> new ResourceNotFoundException("Korisnik nije pronađen"));
        
        if (!passwordEncoder.matches(currentPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("Trenutna lozinka nije ispravna");
        }
        
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        repository.save(user);
    }

}
