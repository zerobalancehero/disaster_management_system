package com.disaster.response.integration.alert;

import com.disaster.response.domain.entity.DisasterAlert;
import java.util.List;

public interface AlertDataProvider {
    String getProviderName();
    boolean isEnabled();
    List<DisasterAlert> fetchLatestAlerts();
}
