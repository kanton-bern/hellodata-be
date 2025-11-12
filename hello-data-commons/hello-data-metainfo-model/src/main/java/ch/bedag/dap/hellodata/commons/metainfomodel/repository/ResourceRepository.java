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
package ch.bedag.dap.hellodata.commons.metainfomodel.repository;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ResourceRepository extends JpaRepository<MetaInfoResourceEntity, UUID> {
    Optional<MetaInfoResourceEntity> findByApiVersionAndModuleTypeAndKindAndInstanceName(String apiVersion, ModuleType moduleType, String kind, String instanceName);

    List<MetaInfoResourceEntity> findAllByModuleType(ModuleType moduleType);

    List<MetaInfoResourceEntity> findAllByModuleTypeAndKind(ModuleType moduleType, String kind);

    Optional<MetaInfoResourceEntity> getByModuleTypeAndKindAndContextKey(ModuleType moduleType, String kind, String contextKey);

    List<MetaInfoResourceEntity> findAllByKind(String kind);

    MetaInfoResourceEntity getByInstanceNameAndKind(String instanceName, String kind);

    MetaInfoResourceEntity getByModuleTypeAndInstanceNameAndKind(ModuleType moduleType, String instanceName, String kind);

    @Query("SELECT r from resource r where r.apiVersion = :apiVersion and r.instanceName = :instanceName and r.moduleType = :moduleType and r.kind != ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind.HELLO_DATA_APP_INFO order by r.kind")
    List<MetaInfoResourceEntity> findAllFilteredByAppInfo(@Param("apiVersion") String apiVersion, @Param("instanceName") String instanceName,
                                                          @Param("moduleType") ModuleType moduleType);
}
