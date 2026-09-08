package com.schemebridge.scheme.repository;

import com.schemebridge.scheme.document.AdminSettings;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AdminSettingsRepository extends MongoRepository<AdminSettings, String> {
}
