package com.incidentbbrain.alertservice.repository;

import com.incidentbbrain.alertservice.enums.Severity;
import com.incidentbbrain.alertservice.model.Alert;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AlertRepository extends JpaRepository<Alert, UUID> {

    Page<Alert> findByServiceNameAndSeverity(
            String serviceName,
            Severity severity,
            Pageable pageable
    );
}