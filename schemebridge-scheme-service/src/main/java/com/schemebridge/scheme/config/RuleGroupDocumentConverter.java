package com.schemebridge.scheme.config;

import com.schemebridge.scheme.document.EligibilityCondition;
import com.schemebridge.scheme.document.MultilingualText;
import com.schemebridge.scheme.document.RuleGroup;
import com.schemebridge.scheme.document.RuleOperator;
import org.bson.Document;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@ReadingConverter
public class RuleGroupDocumentConverter implements Converter<Document, RuleGroup> {

    @Override
    public RuleGroup convert(Document source) {
        if (source == null) {
            return null;
        }

        String logicalOperator = source.getString("logicalOperator");
        if (logicalOperator == null) {
            logicalOperator = "ALL";
        }

        List<EligibilityCondition> conditions = new ArrayList<>();
        List<?> rawConditions = source.get("conditions", List.class);
        if (rawConditions != null) {
            for (Object condObj : rawConditions) {
                if (condObj instanceof Document) {
                    Document condDoc = (Document) condObj;

                    String operatorStr = condDoc.getString("operator");
                    RuleOperator op = null;
                    if (operatorStr != null) {
                        try {
                            op = RuleOperator.valueOf(operatorStr);
                        } catch (IllegalArgumentException ignored) {}
                    }

                    conditions.add(EligibilityCondition.builder()
                            .field(condDoc.getString("field"))
                            .operator(op)
                            .value(condDoc.getString("value"))
                            .dataType(condDoc.getString("dataType"))
                            .question(toMultilingualText(condDoc.get("question")))
                            .description(toMultilingualText(condDoc.get("description")))
                            .required(condDoc.containsKey("required") ? condDoc.getBoolean("required") : true)
                            .sourceReference(condDoc.getString("sourceReference"))
                            .build());
                }
            }
        }

        List<RuleGroup> groups = new ArrayList<>();
        List<?> rawGroups = source.get("groups", List.class);
        if (rawGroups != null) {
            for (Object gObj : rawGroups) {
                if (gObj instanceof Document) {
                    RuleGroup g = convert((Document) gObj);
                    if (g != null) {
                        groups.add(g);
                    }
                }
            }
        }

        return RuleGroup.builder()
                .logicalOperator(logicalOperator)
                .conditions(conditions)
                .groups(groups)
                .build();
    }

    private MultilingualText toMultilingualText(Object obj) {
        if (obj instanceof Document) {
            Document doc = (Document) obj;
            return MultilingualText.builder()
                    .english(doc.getString("english"))
                    .tamil(doc.getString("tamil"))
                    .build();
        }
        return null;
    }
}
