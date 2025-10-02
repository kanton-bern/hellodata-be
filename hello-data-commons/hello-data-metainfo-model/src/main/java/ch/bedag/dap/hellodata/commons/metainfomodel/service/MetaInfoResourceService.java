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
package ch.bedag.dap.hellodata.commons.metainfomodel.service;

import ch.bedag.dap.hellodata.commons.metainfomodel.entity.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.ResourceRepository;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleResourceKind;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.HDVersions;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.HdResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.appinfo.AppInfoResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static ch.bedag.dap.hellodata.commons.sidecars.resources.v1.HdResource.URL_KEY;

@Service
@RequiredArgsConstructor
public class MetaInfoResourceService {
    private final ResourceRepository resourceRepository;

    @Transactional(readOnly = true)
    public List<HdResource> findAll() {
        return resourceRepository.findAll().stream().map(MetaInfoResourceEntity::getMetainfo).toList();
    }

    @Transactional(readOnly = true)
    public List<HdResource> findAllByModuleType(ModuleType moduleType) {
        return resourceRepository.findAllByModuleType(moduleType).stream().map(MetaInfoResourceEntity::getMetainfo).toList();
    }

    @Transactional(readOnly = true)
    public <T extends HdResource> List<T> findAllByModuleTypeAndKind(ModuleType moduleType, String kind, Class<T> concreteClass) {
        return resourceRepository.findAllByModuleTypeAndKind(moduleType, kind)
                .stream()
                .map(MetaInfoResourceEntity::getMetainfo)
                .filter(concreteClass::isInstance)
                .map(concreteClass::cast)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MetaInfoResourceEntity> findAllByModuleTypeAndKind(ModuleType moduleType, String kind) {
        return resourceRepository.findAllByModuleTypeAndKind(moduleType, kind);
    }

    @Transactional(readOnly = true)
    public <T extends HdResource> T findAllByModuleTypeAndKindAndContextKey(ModuleType moduleType, String kind, String contextKey, Class<T> concreteClass) {
        Optional<MetaInfoResourceEntity> result = resourceRepository.getByModuleTypeAndKindAndContextKey(moduleType, kind, contextKey);
        if (result.isPresent()) {
            HdResource metainfo = result.get().getMetainfo();
            if (concreteClass.isInstance(metainfo)) {
                return concreteClass.cast(metainfo);
            }
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<MetaInfoResourceEntity> findAllByKindWithContext(String kind) {
        return resourceRepository.findAllByKind(kind);
    }

    @Transactional(readOnly = true)
    public <T extends HdResource> T findByInstanceNameAndKind(String instanceName, String kind, Class<T> concreteClass) {
        MetaInfoResourceEntity metaInfoResourceEntity = resourceRepository.getByInstanceNameAndKind(instanceName, kind);
        if (metaInfoResourceEntity != null) {
            HdResource metainfo = metaInfoResourceEntity.getMetainfo();
            if (concreteClass.isInstance(metainfo)) {
                return concreteClass.cast(metainfo);
            }
        }
        return null;
    }

    @Transactional(readOnly = true)
    public <T extends HdResource> T findByModuleTypeInstanceNameAndKind(ModuleType moduleType, String instanceName, String kind, Class<T> concreteClass) {
        MetaInfoResourceEntity metaInfoResourceEntity = resourceRepository.getByModuleTypeAndInstanceNameAndKind(moduleType, instanceName, kind);
        if (metaInfoResourceEntity != null) {
            HdResource metainfo = metaInfoResourceEntity.getMetainfo();
            if (concreteClass.isInstance(metainfo)) {
                return concreteClass.cast(metainfo);
            }
        }
        return null;
    }

    @Transactional(readOnly = true)
    public List<HdResource> findAllByKind(String kind) {
        return resourceRepository.findAllByKind(kind).stream().map(MetaInfoResourceEntity::getMetainfo).toList();
    }

    @Transactional(readOnly = true)
    public List<HdResource> findAllByAppInfo(String apiVersion, String instanceName, ModuleType moduleType) {
        return resourceRepository.findAllFilteredByAppInfo(apiVersion, instanceName, moduleType).stream().map(MetaInfoResourceEntity::getMetainfo).toList();
    }

    /**
     * Get the URL of a given instance, provided by the AppInfo resource.
     *
     * @return the instance's URL or null if not found.
     */
    @Transactional(readOnly = true)
    public String findInstanceUrl(ModuleType moduleType, String instanceName) {
        Optional<MetaInfoResourceEntity> metaInfoResource =
                this.resourceRepository.findByApiVersionAndModuleTypeAndKindAndInstanceName(HDVersions.V1, moduleType, ModuleResourceKind.HELLO_DATA_APP_INFO, instanceName);
        if (metaInfoResource.isPresent()) {
            AppInfoResource resource = (AppInfoResource) metaInfoResource.get().getMetainfo();
            Map<String, Object> data = resource.getData();
            return (String) data.get(URL_KEY);
        }
        return null;
    }

    @Transactional(readOnly = true)
    public String findSupersetInstanceNameByContextKey(String contextKey) {
        return findAllByModuleTypeAndKind(ModuleType.SUPERSET, ModuleResourceKind.HELLO_DATA_APP_INFO,
                ch.bedag.dap.hellodata.commons.sidecars.resources.v1.appinfo.AppInfoResource.class).stream()
                .filter(appInfoResource ->
                        appInfoResource.getBusinessContextInfo()
                                .getSubContext() !=
                                null)
                .filter(appInfoResource -> appInfoResource.getBusinessContextInfo()
                        .getSubContext()
                        .getKey()
                        .equalsIgnoreCase(
                                contextKey))
                .findFirst()
                .map(AppInfoResource::getInstanceName)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public SubsystemUser findUserInInstance(String email, String instanceName) {
        return Optional.ofNullable(
                        findByInstanceNameAndKind(instanceName, ModuleResourceKind.HELLO_DATA_USERS, ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.UserResource.class))
                .stream()
                .map(ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.UserResource::getData)
                .flatMap(Collection::stream)
                .filter(userResource -> userResource.getEmail().equalsIgnoreCase(email))
                .findFirst()
                .orElse(null);
    }

}
