/*
 * Copyright © 2024, Kanton Bern
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the <organization> nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL <COPYRIGHT HOLDER> BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package ch.bedag.dap.hellodata.portal.metainfo.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.ResourceRepository;
import ch.bedag.dap.hellodata.commons.metainfomodel.service.MetaInfoResourceService;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.HDVersions;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.HdResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.appinfo.AppInfoResource;
import lombok.extern.log4j.Log4j2;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.modelmapper.ModelMapper;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@Log4j2
@SuppressWarnings("unused")
@ExtendWith(MockitoExtension.class)
public class MetaInfoResourceServiceTest {

    @InjectMocks
    private MetaInfoResourceService metaInfoResourceService;

    @Mock
    private ResourceRepository resourceRepository;

    @Spy
    private ModelMapper modelMapper = new ModelMapper();

    @Test
    public void testFindAll() {
        // given
        MetaInfoResourceEntity entity = new MetaInfoResourceEntity();
        HdResource hdResource = new AppInfoResource();
        entity.setMetainfo(hdResource);
        when(resourceRepository.findAll()).thenReturn(Collections.singletonList(entity));

        // when
        List<HdResource> resources = metaInfoResourceService.findAll();

        // then
        assertEquals(1, resources.size());
        assertEquals(hdResource, resources.get(0));
    }

    @Test
    public void testFindAllByModuleType() {
        // given
        MetaInfoResourceEntity entity = new MetaInfoResourceEntity();
        HdResource hdResource = new AppInfoResource();
        entity.setMetainfo(hdResource);
        when(resourceRepository.findAllByModuleType(ModuleType.SUPERSET)).thenReturn(Collections.singletonList(entity));

        // when
        List<HdResource> resources = metaInfoResourceService.findAllByModuleType(ModuleType.SUPERSET);

        // then
        assertEquals(1, resources.size());
        assertEquals(hdResource, resources.get(0));
    }

    @Test
    public void testFindAllByModuleTypeAndKind() {
        // given
        MetaInfoResourceEntity entity = new MetaInfoResourceEntity();
        AppInfoResource appInfoResource = new AppInfoResource();
        entity.setMetainfo(appInfoResource);
        when(resourceRepository.findAllByModuleTypeAndKind(ModuleType.SUPERSET, "kind")).thenReturn(Collections.singletonList(entity));

        // when
        List<AppInfoResource> resources = metaInfoResourceService.findAllByModuleTypeAndKind(ModuleType.SUPERSET, "kind", AppInfoResource.class);

        // then
        assertEquals(1, resources.size());
        assertEquals(appInfoResource, resources.get(0));
    }

    @Test
    public void testFindAllByKind() {
        // given
        MetaInfoResourceEntity entity = new MetaInfoResourceEntity();
        HdResource hdResource = new AppInfoResource();
        entity.setMetainfo(hdResource);
        when(resourceRepository.findAllByKind("kind")).thenReturn(Collections.singletonList(entity));

        // when
        List<HdResource> resources = metaInfoResourceService.findAllByKind("kind");

        // then
        assertEquals(1, resources.size());
        assertEquals(hdResource, resources.get(0));
    }

    @Test
    public void testFindAllByAppInfo() {
        // given
        MetaInfoResourceEntity entity = new MetaInfoResourceEntity();
        HdResource hdResource = new AppInfoResource();
        entity.setMetainfo(hdResource);
        when(resourceRepository.findAllFilteredByAppInfo(HDVersions.V1, "instanceName", ModuleType.SUPERSET)).thenReturn(Collections.singletonList(entity));

        // when
        List<HdResource> resources = metaInfoResourceService.findAllByAppInfo(HDVersions.V1, "instanceName", ModuleType.SUPERSET);

        // then
        assertEquals(1, resources.size());
        assertEquals(hdResource, resources.get(0));
    }

    @Test
    public void testFindInstanceUrl() {
        // given
        MetaInfoResourceEntity entity = new MetaInfoResourceEntity();
        AppInfoResource appInfoResource = new AppInfoResource(null, "", ModuleType.SUPERSET, "");
        Map<String, Object> data = new HashMap<>();
        data.put(AppInfoResource.URL_KEY, "https://example.com");
        appInfoResource.getData().putAll(data);
        entity.setMetainfo(appInfoResource);
        when(resourceRepository.findByApiVersionAndModuleTypeAndKindAndInstanceName(HDVersions.V1, ModuleType.SUPERSET, "hellodata/AppInfo", "instanceName")).thenReturn(
                Optional.of(entity));

        // when
        String url = metaInfoResourceService.findInstanceUrl(ModuleType.SUPERSET, "instanceName");

        // then
        assertEquals("https://example.com", url);
    }
}
