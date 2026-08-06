package com.schemebridge.config;

import com.schemebridge.entity.Scheme;
import com.schemebridge.repository.SchemeRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SchemeDataSeederTest {

    @Mock
    private SchemeRepository schemeRepository;

    @InjectMocks
    private SchemeDataSeeder schemeDataSeeder;

    @Test
    void run_shouldSeedMissingSchemesEvenWhenCollectionAlreadyHasData() {
        when(schemeRepository.findBySchemeCode(anyString())).thenReturn(Optional.empty());

        schemeDataSeeder.run();

        verify(schemeRepository).findBySchemeCode("PM-KISAN-001");
        verify(schemeRepository).saveAll(anyList());
    }
}
