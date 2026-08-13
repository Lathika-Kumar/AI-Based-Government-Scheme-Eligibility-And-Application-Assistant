package com.schemebridge.coreservice.application.service;

import com.schemebridge.coreservice.application.model.AppSequenceCounter;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;

import java.time.Year;

/**
 * ApplicationNumberGeneratorService generates unique application numbers in the format:
 * SB-APP-YYYY-NNNNNN (e.g., SB-APP-2026-000001)
 *
 * Uses MongoDB atomic findAndModify to ensure thread-safe sequential numbering.
 * Each calendar year gets its own counter document in app_sequence_counters collection.
 */
@Service
@RequiredArgsConstructor
public class ApplicationNumberGeneratorService {

    private final MongoTemplate mongoTemplate;

    public String generateApplicationNumber() {
        int year = Year.now().getValue();
        String counterId = "seq_" + year;

        Query query = new Query(Criteria.where("_id").is(counterId));
        Update update = new Update().inc("sequence", 1);
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true).upsert(true);

        AppSequenceCounter counter = mongoTemplate.findAndModify(
                query, update, options, AppSequenceCounter.class);

        long seq = (counter != null) ? counter.getSequence() : 1;
        return String.format("SB-APP-%d-%06d", year, seq);
    }
}
