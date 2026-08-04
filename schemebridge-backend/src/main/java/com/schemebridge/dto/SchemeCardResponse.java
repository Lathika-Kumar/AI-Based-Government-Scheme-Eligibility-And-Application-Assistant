package com.schemebridge.dto;

import com.schemebridge.enums.SchemeType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeCardResponse {

    private String id;
    private String schemeName;
    private String schemeCode;
    private String shortDescription;
    private String category;
    private SchemeType schemeType;
    private Boolean featured;
    private Integer matchPercentage;
    private String thumbnailUrl;
    private List<String> benefits;
    private List<String> tags;
}
