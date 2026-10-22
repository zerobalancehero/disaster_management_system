package com.disaster.response.dto.team;

import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

public record TeamMembersRequest(
    @NotNull(message = "User IDs list is required")
    List<UUID> userIds
) {}
