package com.schemebridge.scheme.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.repository.SchemeCategoryRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import jakarta.validation.Validator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SchemeSeederTest {

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private SchemeCategoryRepository categoryRepository;

    @Mock
    private Validator validator;

    @InjectMocks
    private SchemeSeeder schemeSeeder;

    private final Path tempSeedPath = Paths.get("data/schemes/schemes_test.seed.json");
    private final ObjectMapper mapper = new ObjectMapper();

    private SchemeCategory category;
    private Scheme scheme;

    @BeforeEach
    public void setUp() throws Exception {
        org.springframework.test.util.ReflectionTestUtils.setField(schemeSeeder, "seedFilePath", tempSeedPath.toString());
        Files.createDirectories(tempSeedPath.getParent());
        
        category = SchemeCategory.builder()
                .code("EDU")
                .name("Education")
                .status("ACTIVE")
                .build();

        scheme = Scheme.builder()
                .id("sch1")
                .schemeCode("SCH-PM-SCHOLAR")
                .slug("pm-scholarship")
                .category(SchemeCategoryRef.builder().code("EDU").name("Education").build())
                .version(1)
                .status(SchemeStatus.ACTIVE)
                .build();
    }

    @AfterEach
    public void tearDown() throws Exception {
        Files.deleteIfExists(tempSeedPath);
    }

    @Test
    public void testSeeder_NoFile_SkipsSilently() throws Exception {
        Files.deleteIfExists(tempSeedPath);
        schemeSeeder.run();
        verifyNoInteractions(schemeRepository);
    }

    @Test
    public void testSeeder_InsertSuccess() throws Exception {
        // Arrange
        String json = """
        [
          {
            "schemeCode": "SCH-PM-SCHOLAR",
            "slug": "pm-scholarship",
            "title": { "english": "PM Scholarship" },
            "description": { "english": "Desc" },
            "categoryCode": "EDU",
            "status": "ACTIVE",
            "version": 1,
            "source": {
              "sourceType": "OFFICIAL_CENTRAL_GOVERNMENT",
              "verificationStatus": "VERIFIED"
            }
          }
        ]
        """;
        Files.writeString(tempSeedPath, json);

        when(validator.validate(any())).thenReturn(Collections.emptySet());
        when(categoryRepository.findByCode("EDU")).thenReturn(Optional.of(category));
        when(schemeRepository.findBySchemeCode("SCH-PM-SCHOLAR")).thenReturn(Optional.empty());
        when(schemeRepository.findBySlug("pm-scholarship")).thenReturn(Optional.empty());

        // Act
        schemeSeeder.run();

        // Assert
        verify(schemeRepository).save(any(Scheme.class));
    }

    @Test
    public void testSeeder_InvalidSeed_Rejected() throws Exception {
        // Arrange
        String json = """
        [
          {
            "schemeCode": "",
            "slug": "pm-scholarship"
          }
        ]
        """;
        Files.writeString(tempSeedPath, json);

        // Act
        schemeSeeder.run();

        // Assert
        verifyNoInteractions(schemeRepository);
    }

    @Test
    public void testSeeder_VersionUpdate_Success() throws Exception {
        // Arrange
        String json = """
        [
          {
            "schemeCode": "SCH-PM-SCHOLAR",
            "slug": "pm-scholarship",
            "title": { "english": "PM Scholarship" },
            "description": { "english": "Desc" },
            "categoryCode": "EDU",
            "status": "ACTIVE",
            "version": 2,
            "source": {
              "sourceType": "OFFICIAL_CENTRAL_GOVERNMENT",
              "verificationStatus": "VERIFIED"
            }
          }
        ]
        """;
        Files.writeString(tempSeedPath, json);

        when(validator.validate(any())).thenReturn(Collections.emptySet());
        when(categoryRepository.findByCode("EDU")).thenReturn(Optional.of(category));
        when(schemeRepository.findBySchemeCode("SCH-PM-SCHOLAR")).thenReturn(Optional.of(scheme));

        // Act
        schemeSeeder.run();

        // Assert
        verify(schemeRepository).save(scheme); // Version 2 > 1, so it updates and saves
    }

    @Test
    public void testSeeder_VersionEqualOrLower_Skipped() throws Exception {
        // Arrange
        String json = """
        [
          {
            "schemeCode": "SCH-PM-SCHOLAR",
            "slug": "pm-scholarship",
            "title": { "english": "PM Scholarship" },
            "description": { "english": "Desc" },
            "categoryCode": "EDU",
            "status": "ACTIVE",
            "version": 1,
            "source": {
              "sourceType": "OFFICIAL_CENTRAL_GOVERNMENT",
              "verificationStatus": "VERIFIED"
            }
          }
        ]
        """;
        Files.writeString(tempSeedPath, json);

        when(validator.validate(any())).thenReturn(Collections.emptySet());
        when(categoryRepository.findByCode("EDU")).thenReturn(Optional.of(category));
        when(schemeRepository.findBySchemeCode("SCH-PM-SCHOLAR")).thenReturn(Optional.of(scheme));

        // Act
        schemeSeeder.run();

        // Assert
        verify(schemeRepository, never()).save(any(Scheme.class)); // Version 1 <= 1, so it skips
    }
}
