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
package ch.bedag.dap.hellodata.portal.postgres;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.ResourceRepository;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdBusinessContextInfo;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.appinfo.AppInfoResource;
import ch.bedag.dap.hellodata.portal.base.config.PersistenceConfig;
import io.nats.client.Connection;
import jakarta.persistence.EntityManager;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.IsEqual.equalTo;

@DataJpaTest
@ActiveProfiles("tc-postgres")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ContextConfiguration(classes = {PersistenceConfig.class})
public class PostgresTestContainerTest {

    @Autowired
    private ResourceRepository resourceRepository;
    @Autowired
    private EntityManager entityManager;
    @MockBean
    private SecurityFilterChain securityFilterChain;
    @MockBean
    private Connection connection;

    @Test
    void should_save_metainfo_to_metainfo_DB() {
        //given

        MetaInfoResourceEntity metaInfoResourceEntity = new MetaInfoResourceEntity();
        UUID uuid = UUID.randomUUID();
        metaInfoResourceEntity.setId(uuid);
        metaInfoResourceEntity.setApiVersion("v1");
        metaInfoResourceEntity.setModuleType(ModuleType.SUPERSET);
        metaInfoResourceEntity.setKind(ModuleResourceKind.HELLO_DATA_APP_INFO);
        metaInfoResourceEntity.setInstanceName("someInstanceName");

        AppInfoResource appInfoResource = new AppInfoResource(new HdBusinessContextInfo(), "someInstanceName", "someNamespace", ModuleType.SUPERSET, "http://localhost/superset");
        metaInfoResourceEntity.setMetainfo(appInfoResource);

        //when
        entityManager.persist(metaInfoResourceEntity);
        List<MetaInfoResourceEntity> all = resourceRepository.findAll();

        //then
        assertThat(all.size(), equalTo(1));
        assertThat(all.get(0).getApiVersion(), equalTo(metaInfoResourceEntity.getApiVersion()));
        assertThat(all.get(0).getModuleType(), equalTo(metaInfoResourceEntity.getModuleType()));
        assertThat(all.get(0).getKind(), equalTo(metaInfoResourceEntity.getKind()));
        assertThat(all.get(0).getInstanceName(), equalTo(metaInfoResourceEntity.getInstanceName()));
        assertThat(all.get(0).getMetainfo(), equalTo(appInfoResource));
    }
}

