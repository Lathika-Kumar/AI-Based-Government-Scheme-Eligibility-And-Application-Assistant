package com.schemebridge.scheme.config;

import com.schemebridge.scheme.document.EligibilityCondition;
import com.schemebridge.scheme.document.RuleGroup;
import com.schemebridge.scheme.document.RuleOperator;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ReadingConverter
public class RuleGroupListConverter implements Converter<List, RuleGroup> {

    @Override
    public RuleGroup convert(List source) {
        if (source == null) {
            return null;
        }

        List<EligibilityCondition> conditions = new ArrayList<>();
        for (Object item : source) {
            if (item != null) {
                conditions.add(EligibilityCondition.builder()
                        .field("LEGACY_FREE_TEXT")
                        .operator(RuleOperator.EQUALS)
                        .value(item.toString())
                        .dataType("STRING")
                        .required(true)
                        .build());
            }
        }

        return RuleGroup.builder()
                .logicalOperator("ALL")
                .conditions(conditions)
                .groups(new ArrayList<>())
                .build();
    }
}
