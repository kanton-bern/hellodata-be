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
package ch.bedag.dap.hellodata.sidecars.portal.service.resource.user;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.metainfomodel.entities.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.entities.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.sidecars.cache.admin.UserCache;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.UserResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.user.data.SubsystemUser;
import ch.bedag.dap.hellodata.sidecars.portal.service.resource.GenericPublishedResourceConsumer;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.HashSet;
import java.util.List;

import static ch.bedag.dap.hellodata.commons.sidecars.cache.admin.UserCache.USER_CACHE_PREFIX;
import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.PUBLISH_USER_RESOURCES;
import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.UPDATE_METAINFO_USERS_CACHE;

@Log4j2
@Service
@AllArgsConstructor
public class PublishedUserResourcesConsumer {
    private final GenericPublishedResourceConsumer genericPublishedResourceConsumer;
    private final RedisTemplate<String, UserCache> redisTemplate;
    private final NatsSenderService natsSenderService;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = PUBLISH_USER_RESOURCES)
    public void subscribe(UserResource userResource) {
        log.info("------- Received user resource {}", userResource);
        MetaInfoResourceEntity resource = genericPublishedResourceConsumer.persistResource(userResource);
        HdContextEntity context = genericPublishedResourceConsumer.attachContext(userResource, resource);
        List<SubsystemUser> data = userResource.getData();
        saveUsersToCache(userResource, data);
        natsSenderService.publishMessageToJetStream(UPDATE_METAINFO_USERS_CACHE, new Object());
    }

    private void saveUsersToCache(UserResource userResource, List<SubsystemUser> data) {
        for (SubsystemUser subsystemUser : data) {
            boolean isAdmin = subsystemUser.getRoles().stream().anyMatch(role -> role.getName().equalsIgnoreCase(SlugifyUtil.BI_ADMIN_ROLE_NAME));
            UserCache userCache = redisTemplate.opsForValue().get(USER_CACHE_PREFIX + subsystemUser.getEmail());
            if (userCache == null) {
                userCache = new UserCache();
                userCache.setSupersetInstancesAdmin(new HashSet<>());
            }
            if (isAdmin) {
                userCache.getSupersetInstancesAdmin().add(userResource.getInstanceName());
            } else {
                userCache.getSupersetInstancesAdmin().remove(userResource.getInstanceName());
            }
            redisTemplate.opsForValue().set(subsystemUser.getEmail(), userCache);
        }
    }
}
