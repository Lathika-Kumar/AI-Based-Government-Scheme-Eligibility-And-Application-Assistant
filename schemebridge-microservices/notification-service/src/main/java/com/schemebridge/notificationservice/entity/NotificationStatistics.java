package com.schemebridge.notificationservice.entity;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;

@Document(collection = "notification_statistics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationStatistics extends BaseEntity {

    @Id
    private String id;

    @Indexed(unique = true)
    private LocalDate date;

    @Builder.Default
    private Long totalSent = 0L;

    @Builder.Default
    private Long totalDelivered = 0L;

    @Builder.Default
    private Long totalFailed = 0L;

    @Builder.Default
    private Long totalRead = 0L;

    @Builder.Default
    private Long emailCount = 0L;

    @Builder.Default
    private Long smsCount = 0L;

    @Builder.Default
    private Long pushCount = 0L;

    @Builder.Default
    private Long inAppCount = 0L;
}
