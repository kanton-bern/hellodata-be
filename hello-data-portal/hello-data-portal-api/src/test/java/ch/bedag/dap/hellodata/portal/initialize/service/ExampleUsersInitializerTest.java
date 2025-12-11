package ch.bedag.dap.hellodata.portal.initialize.service;


import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.portal.initialize.entity.ExampleUsersCreatedEntity;
import ch.bedag.dap.hellodata.portal.initialize.repository.ExampleUsersCreatedRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

class ExampleUsersInitializerTest {

    private ExampleUsersCreatedRepository exampleUsersCreatedRepository;
    private ExampleUsersInitializer initializer;

    @BeforeEach
    void setUp() {
        exampleUsersCreatedRepository = mock(ExampleUsersCreatedRepository.class);
        initializer = Mockito.spy(new ExampleUsersInitializer(
                null, null, null, null, exampleUsersCreatedRepository, null, null, null, null
        ));
    }

    @Test
    void testAreExampleUsersCreated_noEntities_returnsFalse() {
        when(exampleUsersCreatedRepository.findAllByOrderByCreatedDateAsc()).thenReturn(List.of());
        List<HdContextEntity> dataDomains = List.of(mock(HdContextEntity.class));
        boolean result = initializer.areExampleUsersCreated(dataDomains);
        assertFalse(result);
    }

    @Test
    void testAreExampleUsersCreated_oneEntity_allKeysPresent_returnsTrue() {
        HdContextEntity domain1 = mock(HdContextEntity.class);
        when(domain1.getContextKey()).thenReturn("key1");
        ExampleUsersCreatedEntity entity = new ExampleUsersCreatedEntity();
        entity.setDataDomainList("key1");
        when(exampleUsersCreatedRepository.findAllByOrderByCreatedDateAsc()).thenReturn(List.of(entity));
        boolean result = initializer.areExampleUsersCreated(List.of(domain1));
        assertTrue(result);
    }

    @Test
    void testAreExampleUsersCreated_oneEntity_missingKey_returnsFalse() {
        HdContextEntity domain1 = mock(HdContextEntity.class);
        when(domain1.getContextKey()).thenReturn("key2");
        ExampleUsersCreatedEntity entity = new ExampleUsersCreatedEntity();
        entity.setDataDomainList("key1");
        when(exampleUsersCreatedRepository.findAllByOrderByCreatedDateAsc()).thenReturn(List.of(entity));
        boolean result = initializer.areExampleUsersCreated(List.of(domain1));
        assertFalse(result);
    }

    @Test
    void testAreExampleUsersCreated_multipleEntities_deletesOldEntities() {
        HdContextEntity domain1 = mock(HdContextEntity.class);
        when(domain1.getContextKey()).thenReturn("key1");
        ExampleUsersCreatedEntity entity1 = new ExampleUsersCreatedEntity();
        entity1.setDataDomainList("old");
        ExampleUsersCreatedEntity entity2 = new ExampleUsersCreatedEntity();
        entity2.setDataDomainList("key1");
        when(exampleUsersCreatedRepository.findAllByOrderByCreatedDateAsc())
                .thenReturn(List.of(entity1, entity2))
                .thenReturn(List.of(entity2));
        boolean result = initializer.areExampleUsersCreated(List.of(domain1));
        verify(exampleUsersCreatedRepository).delete(entity1);
        assertTrue(result);
    }
}