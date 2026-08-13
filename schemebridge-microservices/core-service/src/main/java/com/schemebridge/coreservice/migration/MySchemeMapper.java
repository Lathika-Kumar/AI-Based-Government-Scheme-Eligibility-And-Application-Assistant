package com.schemebridge.coreservice.migration;

import com.fasterxml.jackson.databind.JsonNode;
import com.schemebridge.coreservice.scheme.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MySchemeMapper {

    private final MySchemeJsonParser parser;

    public Scheme mapToScheme(JsonNode data, SchemeCategory category, SchemeDepartment department) {
        if (data == null) return null;

        String id = data.has("_id") ? data.get("_id").asText() : null;
        String slug = data.has("slug") ? data.get("slug").asText() : null;

        JsonNode en = data.has("en") ? data.get("en") : null;
        JsonNode basic = (en != null && en.has("basicDetails")) ? en.get("basicDetails") : null;
        JsonNode content = (en != null && en.has("schemeContent")) ? en.get("schemeContent") : null;

        if (slug == null && basic != null && basic.has("slug")) {
            slug = basic.get("slug").asText();
        }

        String shortTitle = (basic != null && basic.has("schemeShortTitle") && !basic.get("schemeShortTitle").isNull())
                ? basic.get("schemeShortTitle").asText().trim() : null;
        
        String schemeCode = (slug != null && !slug.isEmpty())
                ? slug.toUpperCase()
                : (shortTitle != null ? shortTitle.toUpperCase() : "SCHEME-" + id);
        if (schemeCode.length() > 90) schemeCode = schemeCode.substring(0, 90);

        String schemeName = (basic != null && basic.has("schemeName") && !basic.get("schemeName").isNull())
                ? basic.get("schemeName").asText().trim() : "";
        if (schemeName.length() > 990) schemeName = schemeName.substring(0, 990);

        String briefDesc = "";
        if (content != null && content.has("briefDescription") && !content.get("briefDescription").isNull()) {
            briefDesc = parser.cleanHtml(content.get("briefDescription").asText());
        } else if (basic != null && basic.has("briefDescription") && !basic.get("briefDescription").isNull()) {
            briefDesc = parser.cleanHtml(basic.get("briefDescription").asText());
        }

        String detailedDesc = "";
        if (content != null && content.has("detailedDescription_md") && !content.get("detailedDescription_md").isNull()) {
            detailedDesc = content.get("detailedDescription_md").asText();
        } else if (content != null && content.has("detailedDescription") && !content.get("detailedDescription").isNull()) {
            detailedDesc = content.get("detailedDescription").toString();
        }

        // Scheme Level / Type
        String schemeType = "STATE";
        if (basic != null && basic.has("level") && !basic.get("level").isNull()) {
            String lvl = basic.get("level").toString().toUpperCase();
            if (lvl.contains("CENTRAL")) {
                schemeType = "CENTRAL";
            }
        }

        // State
        String applicableStates = "ALL_INDIA";
        if ("STATE".equals(schemeType) && basic != null && basic.has("state") && !basic.get("state").isNull()) {
            JsonNode stNode = basic.get("state");
            if (stNode.has("label")) {
                applicableStates = stNode.get("label").asText();
            } else {
                applicableStates = stNode.asText();
            }
        }

        // Eligibility Text
        String eligibilityText = "";
        if (en != null && en.has("eligibilityCriteria") && !en.get("eligibilityCriteria").isNull()) {
            JsonNode eligNode = en.get("eligibilityCriteria");
            if (eligNode.has("eligibilityDescription_md")) {
                eligibilityText = eligNode.get("eligibilityDescription_md").asText();
            } else {
                eligibilityText = eligNode.toString();
            }
        }

        // Application Process
        String applicationProcess = "";
        if (en != null && en.has("applicationProcess") && !en.get("applicationProcess").isNull()) {
            JsonNode appNode = en.get("applicationProcess");
            if (appNode.isArray()) {
                List<String> modeBlocks = new ArrayList<>();
                for (JsonNode modeItem : appNode) {
                    if (!modeItem.isObject()) {
                        String txt = parser.cleanHtml(parser.extractSlateText(modeItem));
                        if (!txt.trim().isEmpty()) modeBlocks.add(txt.trim());
                        continue;
                    }

                    String modeName = modeItem.has("mode") ? modeItem.get("mode").asText() : "Process";
                    String url = modeItem.has("url") ? modeItem.get("url").asText() : "";

                    String procText = "";
                    if (modeItem.has("process_md") && !modeItem.get("process_md").isNull() && !modeItem.get("process_md").asText().trim().isEmpty()) {
                        procText = modeItem.get("process_md").asText().trim();
                    } else if (modeItem.has("process")) {
                        procText = parser.extractSlateText(modeItem.get("process")).trim();
                    }

                    if (!procText.isEmpty()) {
                        StringBuilder sb = new StringBuilder();
                        sb.append("### Application Mode: ").append(modeName);
                        if (url != null && !url.trim().isEmpty()) {
                            sb.append(" (URL: ").append(url.trim()).append(")");
                        }
                        sb.append("\n").append(procText);
                        modeBlocks.add(sb.toString());
                    }
                }
                applicationProcess = String.join("\n\n", modeBlocks);
            } else {
                applicationProcess = parser.extractSlateText(appNode).trim();
            }
        }

        // DBT Scheme
        Boolean dbtScheme = false;
        if (basic != null && basic.has("dbtScheme") && !basic.get("dbtScheme").isNull()) {
            JsonNode dbtNode = basic.get("dbtScheme");
            if (dbtNode.isBoolean()) {
                dbtScheme = dbtNode.asBoolean();
            } else {
                dbtScheme = "Yes".equalsIgnoreCase(dbtNode.asText()) || "true".equalsIgnoreCase(dbtNode.asText());
            }
        }

        // Target Beneficiaries
        String targetBeneficiaries = "";
        if (basic != null && basic.has("targetBeneficiaries") && !basic.get("targetBeneficiaries").isNull()) {
            JsonNode benNode = basic.get("targetBeneficiaries");
            if (benNode.isArray()) {
                List<String> benList = new ArrayList<>();
                for (JsonNode b : benNode) {
                    if (b.has("label")) benList.add(b.get("label").asText());
                    else benList.add(b.asText());
                }
                targetBeneficiaries = String.join(", ", benList);
            } else {
                targetBeneficiaries = benNode.asText();
            }
        }

        Scheme scheme = Scheme.builder()
                .id(id)
                .schemeCode(schemeCode)
                .slug(slug)
                .titleEnglish(schemeName)
                .descriptionEnglish(briefDesc)
                .detailedDescription(detailedDesc)
                .eligibilityText(eligibilityText)
                .applicationProcess(applicationProcess)
                .category(category)
                .department(department)
                .schemeType(schemeType)
                .applicableStates(applicableStates)
                .dbtScheme(dbtScheme)
                .targetBeneficiaries(targetBeneficiaries)
                .schemeUrl(slug != null ? "https://www.myscheme.gov.in/schemes/" + slug : null)
                .status("ACTIVE")
                .featured(false)
                .newlyAdded(false)
                .stateSpecific("STATE".equals(schemeType))
                .build();

        // Benefits mapping (SlateJS AST bullet points extraction)
        if (content != null && content.has("benefits") && !content.get("benefits").isNull()) {
            JsonNode benNode = content.get("benefits");
            List<String> benefitLines = parser.extractSlateTextLines(benNode);
            int displayOrder = 1;
            for (String bDesc : benefitLines) {
                if (!bDesc.trim().isEmpty()) {
                    SchemeBenefit benefit = SchemeBenefit.builder()
                            .scheme(scheme)
                            .benefitType("GENERAL")
                            .descriptionEnglish(bDesc.trim())
                            .currency("INR")
                            .displayOrder(displayOrder++)
                            .status("ACTIVE")
                            .build();
                    scheme.getBenefits().add(benefit);
                }
            }
        }

        // Tags mapping
        if (basic != null && basic.has("tags") && basic.get("tags").isArray()) {
            for (JsonNode tNode : basic.get("tags")) {
                String tagName = tNode.asText().trim();
                if (!tagName.isEmpty()) {
                    SchemeTag tag = SchemeTag.builder()
                            .scheme(scheme)
                            .tag(tagName)
                            .build();
                    scheme.getTags().add(tag);
                }
            }
        }

        return scheme;
    }
}
