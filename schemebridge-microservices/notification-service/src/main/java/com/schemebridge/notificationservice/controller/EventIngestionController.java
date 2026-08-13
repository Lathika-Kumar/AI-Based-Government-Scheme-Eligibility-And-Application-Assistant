package com.schemebridge.notificationservice.controller;

import com.schemebridge.common.dto.ApiResponse;
import com.schemebridge.common.event.DomainEvent;
import com.schemebridge.notificationservice.dto.NotificationResponse;
import com.schemebridge.notificationservice.service.NotificationEventIngestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/notifications/events")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Business Event Ingestion", description = "Asynchronous Domain Event Consumer APIs for Core and Admin Services")
public class EventIngestionController {

    private final NotificationEventIngestionService eventIngestionService;

    @PostMapping
    @Operation(summary = "Ingest Business Domain Event", description = "Consumes domain events emitted by Core Service or Admin Service and triggers end-to-end notification lifecycle")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> ingestEvent(@Valid @RequestBody DomainEvent event) {
        log.info("Received Domain Event POST: type={}, source={}, user={}",
                event.getEventType(), event.getSourceModule(), event.getAuthUserId());
        List<NotificationResponse> responses = eventIngestionService.processDomainEvent(event);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Domain event ingested and processed successfully", responses));
    }
}
