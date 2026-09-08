package com.schemebridge.scheme.service;

import com.schemebridge.scheme.document.*;
import com.schemebridge.scheme.dto.request.SchemeCreateRequest;
import com.schemebridge.scheme.dto.request.SchemeUpdateRequest;
import com.schemebridge.scheme.dto.response.SchemeResponse;
import com.schemebridge.scheme.exception.DuplicateResourceException;
import com.schemebridge.scheme.exception.ResourceNotFoundException;
import com.schemebridge.scheme.repository.SchemeCategoryRepository;
import com.schemebridge.scheme.repository.SchemeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.util.Optional;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class SchemeServiceTest {

    @Mock
    private SchemeRepository schemeRepository;

    @Mock
    private SchemeCategoryRepository categoryRepository;

    @InjectMocks
    private SchemeService schemeService;

    private SchemeCategory category;
    private Scheme scheme;

    @BeforeEach
    public void setUp() {
        category = SchemeCategory.builder()
                .id("cat1")
                .code("EDU")
                .name("Education")
                .status("ACTIVE")
                .build();

        scheme = Scheme.builder()
                .id("sch1")
                .schemeCode("SCH001")
                .slug("sch-slug")
                .title(MultilingualText.builder().english("English Title").tamil("Tamil Title").build())
                .description(MultilingualText.builder().english("English Desc").tamil("Tamil Desc").build())
                .category(SchemeCategoryRef.builder().code("EDU").name("Education").build())
                .status(SchemeStatus.ACTIVE)
                .build();
    }

    @Test
    public void testCreateScheme_Success() {
        SchemeCreateRequest request = SchemeCreateRequest.builder()
                .schemeCode("SCH001")
                .slug("sch-slug")
                .categoryCode("EDU")
                .title(scheme.getTitle())
                .description(scheme.getDescription())
                .build();

        when(schemeRepository.findBySchemeCode("SCH001")).thenReturn(Optional.empty());
        when(schemeRepository.findBySlug("sch-slug")).thenReturn(Optional.empty());
        when(categoryRepository.findByCode("EDU")).thenReturn(Optional.of(category));
        when(schemeRepository.save(any(Scheme.class))).thenReturn(scheme);

        SchemeResponse response = schemeService.createScheme(request);

        assertNotNull(response);
        assertEquals("SCH001", response.getSchemeCode());
        assertEquals("ACTIVE", response.getStatus().name());
    }

    @Test
    public void testCreateScheme_DuplicateCode_ThrowsException() {
        SchemeCreateRequest request = SchemeCreateRequest.builder()
                .schemeCode("SCH001")
                .build();

        when(schemeRepository.findBySchemeCode("SCH001")).thenReturn(Optional.of(scheme));

        assertThrows(DuplicateResourceException.class, () -> schemeService.createScheme(request));
    }

    @Test
    public void testCreateScheme_DuplicateSlug_ThrowsException() {
        SchemeCreateRequest request = SchemeCreateRequest.builder()
                .schemeCode("SCH001")
                .slug("sch-slug")
                .build();

        when(schemeRepository.findBySchemeCode("SCH001")).thenReturn(Optional.empty());
        when(schemeRepository.findBySlug("sch-slug")).thenReturn(Optional.of(scheme));

        assertThrows(DuplicateResourceException.class, () -> schemeService.createScheme(request));
    }

    @Test
    public void testCreateScheme_CategoryNotFound_ThrowsException() {
        SchemeCreateRequest request = SchemeCreateRequest.builder()
                .schemeCode("SCH001")
                .slug("sch-slug")
                .categoryCode("NONEXISTENT")
                .title(scheme.getTitle())
                .description(scheme.getDescription())
                .build();

        when(schemeRepository.findBySchemeCode("SCH001")).thenReturn(Optional.empty());
        when(schemeRepository.findBySlug("sch-slug")).thenReturn(Optional.empty());
        when(categoryRepository.findByCode("NONEXISTENT")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> schemeService.createScheme(request));
    }

    @Test
    public void testGetSchemeById_Success() {
        when(schemeRepository.findById("sch1")).thenReturn(Optional.of(scheme));

        SchemeResponse response = schemeService.getSchemeById("sch1");

        assertNotNull(response);
        assertEquals("sch1", response.getId());
    }

    @Test
    public void testGetSchemeById_NotFound_ThrowsException() {
        when(schemeRepository.findById("missing")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> schemeService.getSchemeById("missing"));
    }

    @Test
    public void testGetSchemeByCode_Success() {
        when(schemeRepository.findBySchemeCode("SCH001")).thenReturn(Optional.of(scheme));

        SchemeResponse response = schemeService.getSchemeByCode("SCH001");

        assertNotNull(response);
        assertEquals("SCH001", response.getSchemeCode());
    }

    @Test
    public void testUpdateStatus_Success() {
        when(schemeRepository.findById("sch1")).thenReturn(Optional.of(scheme));
        when(schemeRepository.save(any(Scheme.class))).thenReturn(scheme);

        SchemeResponse response = schemeService.updateStatus("sch1", "inactive");

        assertNotNull(response);
        verify(schemeRepository).save(scheme);
    }
}
