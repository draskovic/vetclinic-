package com.softart.vetclinic.dto;

import java.util.List;

public record MedicationQuickPicksResponse(
        List<MedicationQuickPickItem> recent,
        List<MedicationQuickPickItem> frequent
) {}