package com.disaster.response.dto.team;

import com.disaster.response.domain.enums.TeamType;
import com.disaster.response.dto.common.GeoPointDto;
import jakarta.validation.Valid;

public record UpdateTeamRequest(
    String name,
    TeamType teamType,
    @Valid GeoPointDto baseLocation,
    String contactDetails
) {}
