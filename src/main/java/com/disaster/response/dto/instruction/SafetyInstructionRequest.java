package com.disaster.response.dto.instruction;

import com.disaster.response.domain.enums.AlertSeverity;
import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.domain.enums.TargetAudience;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SafetyInstructionRequest(
    @NotBlank(message = "Title is required")
    String title,

    @NotNull(message = "Disaster type is required")
    DisasterType disasterType,

    AlertSeverity minimumSeverity,

    @NotBlank(message = "Instruction text is required")
    String instructionText,

    TargetAudience targetAudience,

    String language,

    Boolean active
) {}
