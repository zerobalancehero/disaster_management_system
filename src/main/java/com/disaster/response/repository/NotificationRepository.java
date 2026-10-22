package com.disaster.response.repository;

import com.disaster.response.domain.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, UUID> {
    Page<Notification> findByRecipientIdOrderByCreatedAtDesc(UUID recipientId, Pageable pageable);
    List<Notification> findByRecipientIdAndReadAtIsNullOrderByCreatedAtDesc(UUID recipientId);
    Optional<Notification> findByIdAndRecipientId(UUID id, UUID recipientId);
    long countByRecipientIdAndReadAtIsNull(UUID recipientId);
}
