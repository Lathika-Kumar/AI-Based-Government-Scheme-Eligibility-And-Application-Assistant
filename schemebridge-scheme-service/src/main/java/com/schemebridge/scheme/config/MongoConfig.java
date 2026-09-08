package com.schemebridge.scheme.config;

import com.mongodb.client.MongoClient;
import com.mongodb.client.gridfs.GridFSBucket;
import com.mongodb.client.gridfs.GridFSBuckets;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoCustomConversions;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;

import java.util.List;

@Configuration
public class MongoConfig {

    @Value("${spring.data.mongodb.database:schemebridge}")
    private String databaseName;

    @Bean
    public MongoCustomConversions mongoCustomConversions(
            RuleGroupDocumentConverter documentConverter,
            RuleGroupListConverter listConverter) {
        return new MongoCustomConversions(List.of(documentConverter, listConverter));
    }

    @Bean
    public GridFsTemplate gridFsTemplate(MongoDatabaseFactory dbFactory, MappingMongoConverter converter) {
        return new GridFsTemplate(dbFactory, converter);
    }

    @Bean
    public GridFSBucket gridFSBucket(MongoClient mongoClient) {
        return GridFSBuckets.create(mongoClient.getDatabase(databaseName));
    }
}

