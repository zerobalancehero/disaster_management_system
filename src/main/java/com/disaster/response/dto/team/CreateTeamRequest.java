package com.disaster.response.dto.team;

import com.disaster.response.domain.enums.CapabilityType;
import com.disaster.response.domain.enums.TeamType;
import com.disaster.response.dto.common.GeoPointDto;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateTeamRequest(
    @NotBlank(message = "Team name is required")
    String name,

    @NotNull(message = "Team type is required")
    TeamType teamType,

    @NotNull(message = "Base location is required")
    @Valid
    GeoPointDto baseLocation,

    String contactDetails,

    List<CapabilityType> capabilities
) {}
