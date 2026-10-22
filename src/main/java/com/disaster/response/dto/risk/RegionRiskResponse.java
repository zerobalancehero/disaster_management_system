package com.disaster.response.dto.risk;

import com.disaster.response.domain.entity.Region;
import com.disaster.response.domain.enums.RegionType;
import com.disaster.response.dto.common.GeoPointDto;
import com.disaster.response.gis.GisUtil;

import java.util.List;
import java.util.UUID;

public record RegionRiskResponse(
    UUID id,
    String name,
    RegionType regionType,
    GeoPointDto centerPoint,
    List<RiskAssessmentResponse> activeRisks
) {
    public static RegionRiskResponse of(Region region, List<RiskAssessmentResponse> activeRisks) {
        GeoPointDto center = region.getCenterPoint() != null
                ? GeoPointDto.of(GisUtil.getLatitude(region.getCenterPoint()), GisUtil.getLongitude(region.getCenterPoint()))
                : null;
        return new RegionRiskResponse(
            region.getId(),
            region.getName(),
            region.getRegionType(),
            center,
            activeRisks
        );
    }
}
