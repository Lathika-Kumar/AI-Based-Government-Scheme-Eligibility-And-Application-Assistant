package com.schemebridge.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.config.EnableMongoAuditing;

@Configuration
@EnableMongoAuditing
public class MongoConfig {
    // Mongo auditing configuration enabled for @CreatedDate and @LastModifiedDate annotations
}
