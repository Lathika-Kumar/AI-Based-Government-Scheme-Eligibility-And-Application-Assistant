package com.schemebridge.adminservice.config;

import com.schemebridge.adminservice.entity.Admin;
import com.schemebridge.adminservice.entity.FeatureFlag;
import com.schemebridge.adminservice.entity.Officer;
import com.schemebridge.adminservice.entity.SystemSettings;
import com.schemebridge.adminservice.enums.OfficerRole;
import com.schemebridge.adminservice.enums.OfficerStatus;
import com.schemebridge.adminservice.repository.AdminRepository;
import com.schemebridge.adminservice.repository.FeatureFlagRepository;
import com.schemebridge.adminservice.repository.OfficerRepository;
import com.schemebridge.adminservice.repository.SystemSettingsRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final AdminRepository adminRepository;
    private final OfficerRepository officerRepository;
    private final SystemSettingsRepository settingsRepository;
    private final FeatureFlagRepository flagRepository;

    @Override
    public void run(String... args) {
        log.info("Seeding default Admin Service data into Database...");

        // 1. Seed Super Admin
        String defaultAdminId = "ADMIN-001";
        String defaultAdminEmail = "admin@schemebridge.gov.in";

        if (adminRepository.existsByAdminId(defaultAdminId) || adminRepository.existsByEmail(defaultAdminEmail)) {
            log.info("Default Super Admin already exists (adminId: {}, email: {}). Skipping seeding.", defaultAdminId, defaultAdminEmail);
        } else {
            Admin admin = Admin.builder()
                    .adminId(defaultAdminId)
                    .fullName("Principal System Administrator")
                    .email(defaultAdminEmail)
                    .phoneNumber("+919999900000")
                    .role(OfficerRole.SUPER_ADMIN)
                    .status(OfficerStatus.ACTIVE)
                    .department("Ministry of Electronics & IT")
                    .build();
            adminRepository.save(admin);
            log.info("Seeded default Super Admin.");
        }

        // 2. Seed Sample Officers
        String defaultOfficerId = "OFFICER-101";
        String defaultOfficerEmail = "verification.officer@schemebridge.gov.in";

        if (officerRepository.existsByOfficerId(defaultOfficerId) || officerRepository.existsByEmail(defaultOfficerEmail)) {
            log.info("Default sample Officer already exists (officerId: {}, email: {}). Skipping seeding.", defaultOfficerId, defaultOfficerEmail);
        } else {
            Officer officer = Officer.builder()
                    .officerId(defaultOfficerId)
                    .fullName("Ramesh Sharma")
                    .email(defaultOfficerEmail)
                    .phoneNumber("+919876543211")
                    .role(OfficerRole.VERIFICATION_OFFICER)
                    .status(OfficerStatus.ACTIVE)
                    .department("Department of Social Welfare")
                    .jurisdictionState("Maharashtra")
                    .jurisdictionDistrict("Pune")
                    .build();
            officerRepository.save(officer);
            log.info("Seeded sample Officer.");
        }

        // 3. Seed System Settings
        List<SystemSettings> settings = List.of(
                SystemSettings.builder().settingKey("PLATFORM_NAME").settingValue("SchemeBridge Government Portal").category("GENERAL").description("Official portal title").build(),
                SystemSettings.builder().settingKey("MAX_FILE_UPLOAD_SIZE_MB").settingValue("10").category("STORAGE").description("Maximum document upload size in MB").build(),
                SystemSettings.builder().settingKey("AUTO_APPROVE_VERIFIED_DOCS").settingValue("true").category("AUTOMATION").description("Auto-approve document verification when OCR matches 100%").build()
        );

        for (SystemSettings s : settings) {
            if (settingsRepository.findBySettingKey(s.getSettingKey()).isEmpty()) {
                settingsRepository.save(s);
            }
        }

        // 4. Seed Feature Flags
        List<FeatureFlag> flags = List.of(
                FeatureFlag.builder().flagName("ENABLE_DIGILOCKER_INTEGRATION").enabled(true).description("Toggle DigiLocker OAuth sync feature").build(),
                FeatureFlag.builder().flagName("ENABLE_AI_RECOMMENDATIONS").enabled(true).description("Toggle AI LLM recommendation engine").build()
        );

        for (FeatureFlag f : flags) {
            if (flagRepository.findByFlagName(f.getFlagName()).isEmpty()) {
                flagRepository.save(f);
            }
        }

        log.info("Admin Service Data Seeding completed.");
    }
}
