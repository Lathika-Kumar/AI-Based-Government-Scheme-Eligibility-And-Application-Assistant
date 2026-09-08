package com.schemebridge.scheme.document;

import lombok.*;
import org.springframework.data.mongodb.core.index.Indexed;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SchemeCategoryRef {
    @Indexed
    private String code;
    private String name;
}
