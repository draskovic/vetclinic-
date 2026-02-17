package com.softart.vetclinic.service;

import com.softart.vetclinic.entity.Notification;
import com.softart.vetclinic.enums.NotificationStatus;
import com.softart.vetclinic.repository.NotificationRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class NotificationService extends AbstractCrudService<Notification, NotificationRepository> {

    private final NotificationRepository notificationRepository;

    public NotificationService(NotificationRepository notificationRepository) {
        super(notificationRepository);
        this.notificationRepository = notificationRepository;
    }

    @Override
    protected String getEntityName() {
        return "Notification";
    }

    @Override
    protected Optional<Notification> findByIdAndClinicId(UUID id, UUID clinicId) {
        return notificationRepository.findByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Override
    protected Page<Notification> findAllByClinicId(UUID clinicId, Pageable pageable) {
        return notificationRepository.findByClinicIdAndDeletedFalse(clinicId, pageable);
    }

    @Override
    protected boolean existsByIdAndClinicId(UUID id, UUID clinicId) {
        return notificationRepository.existsByIdAndClinicIdAndDeletedFalse(id, clinicId);
    }

    @Transactional(readOnly = true)
    public List<Notification> findPendingBefore(UUID clinicId, OffsetDateTime before) {
        return notificationRepository.findByClinicIdAndDeletedFalseAndStatusAndScheduledAtBefore(
                clinicId, NotificationStatus.PENDING, before);
    }
}
