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
package ch.bedag.dap.hellodata.jupyterhub.gateway.repository;

import ch.bedag.dap.hellodata.jupyterhub.gateway.entities.PortalRole;
import ch.bedag.dap.hellodata.jupyterhub.gateway.entities.User;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.retry.annotation.Recover;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Spring Data R2DBC repository for the {@link User} entity.
 */
@Repository
public interface UserRepository extends R2dbcRepository<User, String>, UserRepositoryInternal {

}

interface UserRepositoryInternal {

    Mono<User> findOneWithPermissionsByEmail(String login, String contextKey);

    @Recover
    Mono<User> getUserResponseFallback(Exception e, String email, String contextKey);
}

@Log4j2
class UserRepositoryInternalImpl implements UserRepositoryInternal {

    private final DatabaseClient db;
    private final R2dbcConverter r2dbcConverter;

    public UserRepositoryInternalImpl(DatabaseClient db, R2dbcConverter r2dbcConverter) {
        this.db = db;
        this.r2dbcConverter = r2dbcConverter;
    }

    @Override
    public Mono<User> findOneWithPermissionsByEmail(String login, String contextKey) {
        return findOneWithAuthoritiesBy(login, contextKey);
    }

    @Override
    public Mono<User> getUserResponseFallback(Exception e, String email, String contextKey) {
        log.error("---- Recover method called after max retries - returning a dummy user without any privileges. The exception that caused retries:", e);
        User user = new User();
        user.setPortalPermissions(Collections.emptySet());
        user.setEmail(email);
        return Mono.just(user);
    }

    private Mono<User> findOneWithAuthoritiesBy(Object email, Object contextKey) {
        return db.sql(
                        "SELECT _user.*, _role.*, _user_portal_role.* " + "FROM user_ _user " + "LEFT JOIN user_portal_role _user_portal_role ON _user.id = _user_portal_role.user_id " +
                                "LEFT JOIN portal_role _role ON _role.id = _user_portal_role.portal_role_id " + "WHERE _user.email = :email AND _user_portal_role.context_key = :contextKey")
                .bind("email", email)
                .bind("contextKey", contextKey)
                .map((row, metadata) -> Tuples.of(r2dbcConverter.read(User.class, row, metadata), Optional.ofNullable(r2dbcConverter.read(PortalRole.class, row, metadata))))
                .all()
                .collectList()
                .filter(l -> !l.isEmpty())
                .map(l -> mapUserWithAuthorities(l.get(0).getT1(), l));
    }

    private User mapUserWithAuthorities(User user, List<Tuple2<User, Optional<PortalRole>>> tuples) {
        log.info("mapping user {} with authorities {}", user.getEmail(), tuples);
        user.setPortalPermissions(
                tuples.stream().filter(t -> t.getT2().isPresent()).flatMap(t -> t.getT2().get().getPermissions().getPortalPermissions().stream()).collect(Collectors.toSet()));
        return user;
    }
}
