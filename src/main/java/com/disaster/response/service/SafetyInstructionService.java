package com.disaster.response.service;

import com.disaster.response.domain.entity.SafetyInstruction;
import com.disaster.response.domain.entity.User;
import com.disaster.response.domain.enums.AlertSeverity;
import com.disaster.response.domain.enums.DisasterType;
import com.disaster.response.domain.enums.TargetAudience;
import com.disaster.response.dto.instruction.SafetyInstructionRequest;
import com.disaster.response.dto.instruction.SafetyInstructionResponse;
import com.disaster.response.exception.ResourceNotFoundException;
import com.disaster.response.repository.SafetyInstructionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class SafetyInstructionService {

    private final SafetyInstructionRepository safetyInstructionRepository;
    private final AuditService auditService;

    public SafetyInstructionService(SafetyInstructionRepository safetyInstructionRepository, AuditService auditService) {
        this.safetyInstructionRepository = safetyInstructionRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<SafetyInstructionResponse> getApplicableInstructions(DisasterType disasterType, AlertSeverity currentSeverity, String lang) {
        String language = (lang != null && !lang.isBlank()) ? lang : "en";
        List<SafetyInstruction> instructions = safetyInstructionRepository.findApplicableInstructions(disasterType, language);

        // Filter by minimum severity threshold:
        // Instruction is applicable if its minimumSeverity ordinal <= current alert severity ordinal
        int currentOrdinal = currentSeverity != null ? currentSeverity.ordinal() : AlertSeverity.LOW.ordinal();

        return instructions.stream()
                .filter(i -> i.getMinimumSeverity().ordinal() <= currentOrdinal)
                .sorted(Comparator.comparing(SafetyInstruction::getTitle))
                .map(SafetyInstructionResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public Page<SafetyInstructionResponse> getAllInstructions(Pageable pageable) {
        return safetyInstructionRepository.findAll(pageable)
                .map(SafetyInstructionResponse::from);
    }

    @Transactional
    public SafetyInstructionResponse createInstruction(SafetyInstructionRequest request, User creator) {
        SafetyInstruction instruction = new SafetyInstruction(
                request.title(),
                request.disasterType(),
                request.minimumSeverity() != null ? request.minimumSeverity() : AlertSeverity.LOW,
                request.instructionText(),
                request.targetAudience() != null ? request.targetAudience() : TargetAudience.GENERAL_PUBLIC,
                request.language() != null ? request.language() : "en",
                creator
        );
        if (request.active() != null) {
            instruction.setActive(request.active());
        }

        SafetyInstruction saved = safetyInstructionRepository.save(instruction);
        auditService.record(creator, "CREATE_SAFETY_INSTRUCTION", "SafetyInstruction", saved.getId().toString(), null, saved.getTitle());
        return SafetyInstructionResponse.from(saved);
    }

    @Transactional
    public SafetyInstructionResponse updateInstruction(UUID id, SafetyInstructionRequest request, User updater) {
        SafetyInstruction instruction = safetyInstructionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Safety instruction not found with ID: " + id));

        String oldValues = "title=" + instruction.getTitle() + ", active=" + instruction.isActive();

        if (request.title() != null) instruction.setTitle(request.title());
        if (request.disasterType() != null) instruction.setDisasterType(request.disasterType());
        if (request.minimumSeverity() != null) instruction.setMinimumSeverity(request.minimumSeverity());
        if (request.instructionText() != null) instruction.setInstructionText(request.instructionText());
        if (request.targetAudience() != null) instruction.setTargetAudience(request.targetAudience());
        if (request.language() != null) instruction.setLanguage(request.language());
        if (request.active() != null) instruction.setActive(request.active());
        instruction.setUpdater(updater);

        SafetyInstruction saved = safetyInstructionRepository.save(instruction);
        String newValues = "title=" + saved.getTitle() + ", active=" + saved.isActive();
        auditService.record(updater, "UPDATE_SAFETY_INSTRUCTION", "SafetyInstruction", saved.getId().toString(), oldValues, newValues);

        return SafetyInstructionResponse.from(saved);
    }
}
