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
package ch.bedag.dap.hellodata.docs.service;

import ch.bedag.dap.hellodata.docs.model.ProjectDoc;
import com.github.benmanes.caffeine.cache.Cache;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;

@Log4j2
@Service
@RequiredArgsConstructor
public class ProjectDocService {

    private final CacheManager cacheManager;
    @Value("${spring.cache.cache-names}")
    private String projectsCacheName;

    public List<ProjectDoc> getAllProjectsDocs() {
        log.info("Get all projects docs from cache: '{}'", projectsCacheName);
        Cache<String, ProjectDoc> nativeCache = getNativeCache();
        return new ArrayList<>(nativeCache.asMap().values());
    }

    public Set<String> getAvailableDataDomainKeys() {
        return getAllProjectsDocs().stream().map(ProjectDoc::contextKey).collect(Collectors.toSet());
    }

    public Optional<ProjectDoc> getByName(String projectName) {
        Cache<String, ProjectDoc> nativeCache = getNativeCache();
        return Optional.ofNullable(nativeCache.asMap().get(projectName));
    }

    public void addProject(ProjectDoc projectDoc) {
        log.info("Adding project {} to cache '{}'", projectDoc, projectsCacheName);
        getProjectsCache().put(projectDoc.contextKey() + "-" + projectDoc.name(), projectDoc);
    }

    public void clearCache() {
        log.info("Clear '{}' cache", projectsCacheName);
        getProjectsCache().clear();
    }

    private org.springframework.cache.Cache getProjectsCache() {
        org.springframework.cache.Cache cache = cacheManager.getCache(projectsCacheName);
        if (cache != null) {
            return cache;
        }
        throw new RuntimeException("Projects cache not found");
    }

    private Cache<String, ProjectDoc> getNativeCache() {
        return (Cache<String, ProjectDoc>) getProjectsCache().getNativeCache();
    }
}
