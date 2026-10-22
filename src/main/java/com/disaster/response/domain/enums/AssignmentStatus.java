package com.disaster.response.domain.enums;

import java.util.Set;

public enum AssignmentStatus {
    PENDING_APPROVAL,
    APPROVED,
    REJECTED,
    ACCEPTED,
    EN_ROUTE,
    ON_SCENE,
    RESOLVED,
    CANCELLED;

    /**
     * Enforces the explicit state machine transition rules.
     */
    public boolean canTransitionTo(AssignmentStatus target) {
        if (target == null) {
            return false;
        }
        if (this == target) {
            return true;
        }

        return switch (this) {
            case PENDING_APPROVAL -> Set.of(APPROVED, REJECTED, CANCELLED).contains(target);
            case APPROVED -> Set.of(ACCEPTED, CANCELLED).contains(target);
            case ACCEPTED -> Set.of(EN_ROUTE, CANCELLED).contains(target);
            case EN_ROUTE -> Set.of(ON_SCENE, CANCELLED).contains(target);
            case ON_SCENE -> Set.of(RESOLVED, CANCELLED).contains(target);
            case RESOLVED, REJECTED, CANCELLED -> false; // Terminal states
        };
    }
}
