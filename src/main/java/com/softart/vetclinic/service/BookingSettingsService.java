package com.softart.vetclinic.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.softart.vetclinic.dto.BookingSettingsResponse;
import com.softart.vetclinic.dto.UpdateBookingSettingsRequest;
import com.softart.vetclinic.entity.BookingSettings;
import com.softart.vetclinic.repository.BookingSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingSettingsService {

    private final BookingSettingsRepository repository;
    private final ObjectMapper objectMapper;

    public BookingSettingsResponse getOrCreate(UUID clinicId) {
        BookingSettings settings = repository.findByClinicId(clinicId)
                .orElseGet(() -> {
                    BookingSettings defaults = new BookingSettings();
                    defaults.setClinicId(clinicId);
                    return repository.save(defaults);
                });
        return toResponse(settings);
    }

    @Transactional
    public BookingSettingsResponse update(UUID clinicId, UpdateBookingSettingsRequest request) {
        BookingSettings settings = repository.findByClinicId(clinicId)
                .orElseGet(() -> {
                    BookingSettings defaults = new BookingSettings();
                    defaults.setClinicId(clinicId);
                    return repository.save(defaults);
                });

        if (request.enabled() != null) settings.setEnabled(request.enabled());
        if (request.slotDurationMinutes() != null) settings.setSlotDurationMinutes(request.slotDurationMinutes());
        if (request.bufferMinutes() != null) settings.setBufferMinutes(request.bufferMinutes());
        if (request.maxAdvanceDays() != null) settings.setMaxAdvanceDays(request.maxAdvanceDays());
        if (request.autoConfirm() != null) settings.setAutoConfirm(request.autoConfirm());
        if (request.allowVetSelection() != null) settings.setAllowVetSelection(request.allowVetSelection());
        if (request.cancellationHours() != null) settings.setCancellationHours(request.cancellationHours());
        if (request.timezone() != null) settings.setTimezone(request.timezone());

        if (request.allowedTypes() != null) {
            try {
                settings.setAllowedTypes(objectMapper.writeValueAsString(request.allowedTypes()));
            } catch (Exception e) {
                throw new IllegalArgumentException("Nevažeći format za dozvoljene tipove");
            }
        }

        BookingSettings saved = repository.save(settings);
        return toResponse(saved);
    }

    private BookingSettingsResponse toResponse(BookingSettings s) {
        List<String> types;
        try {
            types = objectMapper.readValue(s.getAllowedTypes(), new TypeReference<List<String>>() {});
        } catch (Exception e) {
            types = List.of("CHECKUP", "VACCINATION", "GROOMING");
        }

        return new BookingSettingsResponse(
                s.getId(),
                s.getClinicId(),
                s.getEnabled(),
                s.getSlotDurationMinutes(),
                s.getBufferMinutes(),
                s.getMaxAdvanceDays(),
                types,
                s.getAutoConfirm(),
                s.getAllowVetSelection(),
                s.getCancellationHours(),
                s.getTimezone()
        );
    }
}
