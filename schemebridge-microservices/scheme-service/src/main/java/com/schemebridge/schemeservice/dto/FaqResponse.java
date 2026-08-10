package com.schemebridge.schemeservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FaqResponse {
    private String id;
    private String questionEnglish;
    private String questionTamil;
    private String answerEnglish;
    private String answerTamil;
    private Integer displayOrder;
}
