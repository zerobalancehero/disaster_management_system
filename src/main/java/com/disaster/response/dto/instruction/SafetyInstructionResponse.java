package com.disaster.response.dto.instruction;

import com.disaster.response.domain.entity.SafetyInstruction;
import com.disaster.response.domain.enums.AlertSeverity;
import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.domain.enums.TargetAudience;

import java.time.Instant;
import java.util.UUID;

public record SafetyInstructionResponse(
    UUID id,
    String title,
    DisasterType disasterType,
    AlertSeverity minimumSeverity,
    String instructionText,
    TargetAudience targetAudience,
    String language,
    boolean active,
    Instant updatedAt
) {
    public static SafetyInstructionResponse from(SafetyInstruction si) {
        if (si == null) {
            return null;
        }
        return new SafetyInstructionResponse(
            si.getId(),
            si.getTitle(),
            si.getDisasterType(),
            si.getMinimumSeverity(),
            si.getInstructionText(),
            si.getTargetAudience(),
            si.getLanguage(),
            si.isActive(),
            si.getUpdatedAt()
        );
    }
}
