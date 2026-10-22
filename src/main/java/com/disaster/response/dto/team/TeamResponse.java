package com.disaster.response.dto.team;

import com.disaster.response.domain.entity.ResponderTeam;
import com.disaster.response.domain.enums.AvailabilityStatus;
import com.disaster.response.domain.enums.CapabilityType;
import com.disaster.response.domain.enums.TeamType;
import com.disaster.response.dto.common.GeoPointDto;
import com.disaster.response.gis.GisUtil;

import java.util.List;
import java.util.UUID;

public record TeamResponse(
    UUID id,
    String name,
    TeamType teamType,
    AvailabilityStatus availabilityStatus,
    GeoPointDto baseLocation,
    String contactDetails,
    int memberCount,
    List<CapabilityType> capabilities
) {
    public static TeamResponse from(ResponderTeam team) {
        if (team == null) {
            return null;
        }
        GeoPointDto loc = team.getBaseLocation() != null
                ? GeoPointDto.of(GisUtil.getLatitude(team.getBaseLocation()), GisUtil.getLongitude(team.getBaseLocation()))
                : null;

        List<CapabilityType> caps = team.getCapabilities() != null
                ? team.getCapabilities().stream().map(c -> c.getCapability()).toList()
                : List.of();

        int members = team.getMembers() != null ? team.getMembers().size() : 0;

        return new TeamResponse(
            team.getId(),
            team.getName(),
            team.getTeamType(),
            team.getAvailabilityStatus(),
            loc,
            team.getContactDetails(),
            members,
            caps
        );
    }
}
