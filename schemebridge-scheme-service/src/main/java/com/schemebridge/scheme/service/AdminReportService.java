package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminReportService {

    private final MongoTemplate mongoTemplate;
    private final ApplicationRepository applicationRepository;
    private final SchemeRepository schemeRepository;
    private final GrievanceRepository grievanceRepository;

    public String generateApplicationsCsv(String status, String schemeCode) {
        Query query = new Query();
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            try {
                query.addCriteria(Criteria.where("status").is(ApplicationStatus.valueOf(status.toUpperCase())));
            } catch (Exception ignored) {}
        }
        if (schemeCode != null && !schemeCode.isBlank() && !"all".equalsIgnoreCase(schemeCode)) {
            query.addCriteria(Criteria.where("schemeCode").is(schemeCode));
        }
        query.fields().include("applicationNumber", "userId", "schemeCode", "status", "createdAt", "submittedAt");

        List<Application> list = mongoTemplate.find(query, Application.class);
        StringBuilder csv = new StringBuilder();
        csv.append("Application Number,User ID,Scheme Code,Status,Created At,Submitted At\n");
        for (Application app : list) {
            csv.append(escapeCsv(app.getApplicationNumber())).append(",")
                    .append(escapeCsv(app.getUserId())).append(",")
                    .append(escapeCsv(app.getSchemeCode())).append(",")
                    .append(escapeCsv(app.getStatus() != null ? app.getStatus().name() : "")).append(",")
                    .append(escapeCsv(app.getCreatedAt() != null ? app.getCreatedAt().toString() : "")).append(",")
                    .append(escapeCsv(app.getSubmittedAt() != null ? app.getSubmittedAt().toString() : "")).append("\n");
        }
        return csv.toString();
    }

    public String generateSchemesCsv(String status) {
        Query query = new Query();
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            try {
                query.addCriteria(Criteria.where("status").is(SchemeStatus.valueOf(status.toUpperCase())));
            } catch (Exception ignored) {}
        }
        query.fields().include("schemeCode", "title.english", "category.code", "status", "schemeLevel", "stateOrUt");
        List<Scheme> list = mongoTemplate.find(query, Scheme.class);
        StringBuilder csv = new StringBuilder();
        csv.append("Scheme Code,Title,Category,Status,Scheme Level,State\n");
        for (Scheme s : list) {
            String title = s.getTitle() != null ? s.getTitle().getEnglish() : "";
            String cat = s.getCategory() != null ? s.getCategory().getCode() : "";
            String state = s.getStateOrUt() != null ? s.getStateOrUt() : "ALL_INDIA";
            csv.append(escapeCsv(s.getSchemeCode())).append(",")
                    .append(escapeCsv(title)).append(",")
                    .append(escapeCsv(cat)).append(",")
                    .append(escapeCsv(s.getStatus() != null ? s.getStatus().name() : "")).append(",")
                    .append(escapeCsv(s.getSchemeLevel() != null ? s.getSchemeLevel().name() : "")).append(",")
                    .append(escapeCsv(state)).append("\n");
        }
        return csv.toString();
    }


    public String generateGrievancesCsv(String status) {
        Query query = new Query();
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            try {
                query.addCriteria(Criteria.where("status").is(GrievanceStatus.valueOf(status.toUpperCase())));
            } catch (Exception ignored) {}
        }
        query.fields().include("grievanceNumber", "userId", "category", "subject", "priority", "status", "createdAt", "resolvedAt");
        List<Grievance> list = mongoTemplate.find(query, Grievance.class);
        StringBuilder csv = new StringBuilder();
        csv.append("Grievance Number,User ID,Category,Subject,Priority,Status,Created At,Resolved At\n");
        for (Grievance g : list) {
            csv.append(escapeCsv(g.getGrievanceNumber())).append(",")
                    .append(escapeCsv(g.getUserId())).append(",")
                    .append(escapeCsv(g.getCategory())).append(",")
                    .append(escapeCsv(g.getSubject())).append(",")
                    .append(escapeCsv(g.getPriority() != null ? g.getPriority().name() : "")).append(",")
                    .append(escapeCsv(g.getStatus() != null ? g.getStatus().name() : "")).append(",")
                    .append(escapeCsv(g.getCreatedAt() != null ? g.getCreatedAt().toString() : "")).append(",")
                    .append(escapeCsv(g.getResolvedAt() != null ? g.getResolvedAt().toString() : "")).append("\n");
        }
        return csv.toString();
    }

    private String escapeCsv(String val) {
        if (val == null) return "";
        if (val.contains(",") || val.contains("\"") || val.contains("\n")) {
            return "\"" + val.replace("\"", "\"\"") + "\"";
        }
        return val;
    }
}
