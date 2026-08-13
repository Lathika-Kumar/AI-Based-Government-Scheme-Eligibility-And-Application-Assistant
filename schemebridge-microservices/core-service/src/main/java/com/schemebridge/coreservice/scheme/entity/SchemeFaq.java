package com.schemebridge.coreservice.scheme.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

import java.time.LocalDateTime;

@Entity
@Table(name = "SCHEME_FAQS")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeFaq {

    @Id
    @GeneratedValue(generator = "uuid2")
    @GenericGenerator(name = "uuid2", strategy = "uuid2")
    @Column(name = "ID", length = 36)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "SCHEME_ID", nullable = false)
    private Scheme scheme;

    @Column(name = "QUESTION_ENGLISH", length = 1000)
    private String questionEnglish;

    @Column(name = "QUESTION_TAMIL", length = 1000)
    private String questionTamil;

    @Column(name = "ANSWER_ENGLISH", columnDefinition = "CLOB")
    private String answerEnglish;

    @Column(name = "ANSWER_TAMIL", columnDefinition = "CLOB")
    private String answerTamil;

    @Column(name = "DISPLAY_ORDER")
    private Integer displayOrder;

    @Column(name = "STATUS", length = 20)
    private String status;

    @Column(name = "CREATED_AT")
    private LocalDateTime createdAt;

    @Column(name = "UPDATED_AT")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) this.status = "ACTIVE";
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
