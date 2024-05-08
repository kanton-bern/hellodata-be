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
package ch.bedag.dap.hellodata.cloudbeaver.gateway.repository;

import ch.bedag.dap.hellodata.cloudbeaver.gateway.entities.User;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.r2dbc.convert.R2dbcConverter;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.retry.annotation.Recover;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * Spring Data R2DBC repository for the {@link User} entity.
 */
@Repository
public interface UserRepository extends R2dbcRepository<User, String>, UserRepositoryInternal {

}

interface UserRepositoryInternal {

    Mono<User> findOneWithPermissionsByEmail(String email);

    @Recover
    Mono<User> getUserResponseFallback(Exception e, String email);
}

@Log4j2
class UserRepositoryInternalImpl implements UserRepositoryInternal {

    public static final String ROLE_FIELD_IDENTIFIER = "key";

    private final DatabaseClient db;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final R2dbcConverter r2dbcConverter;

    public UserRepositoryInternalImpl(DatabaseClient db, R2dbcEntityTemplate r2dbcEntityTemplate, R2dbcConverter r2dbcConverter) {
        this.db = db;
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
        this.r2dbcConverter = r2dbcConverter;
    }

    @Override
    public Mono<User> findOneWithPermissionsByEmail(String login) {
        return findOneWithAuthoritiesBy(login);
    }

    @Override
    public Mono<User> getUserResponseFallback(Exception e, String email) {
        log.error("---- Recover method called after max retries - returning a dummy user without any privileges. The exception that caused retries:", e);
        User user = new User();
        user.setAuthorities(Collections.emptySet());
        user.setEmail(email);
        return Mono.just(user);
    }

    private Mono<User> findOneWithAuthoritiesBy(Object fieldValue) {
        return db.sql("SELECT _role.* FROM hd_user _user LEFT JOIN hd_users_roles user_roles ON _user.id=user_roles.hd_user_id " +
                      "         LEFT JOIN hd_role _role on _role.id = user_roles.hd_role_id " +
                      "         LEFT JOIN hd_roles_privileges role_priv on _role.id = role_priv.hd_role_id " +
                      "         LEFT JOIN hd_privilege priv on role_priv.hd_privilege_id = priv.id " + "         WHERE _user.email = :email")
                 .bind("email", fieldValue)
                 .map((row, metadata) -> Tuples.of(r2dbcConverter.read(User.class, row, metadata), Optional.ofNullable(row.get(ROLE_FIELD_IDENTIFIER, String.class))))
                 .all()
                 .collectList()
                 .filter(l -> !l.isEmpty())
                 .map(l -> mapUserWithAuthorities(l.get(0).getT1(), l));
    }

    private User mapUserWithAuthorities(User user, List<Tuple2<User, Optional<String>>> tuples) {
        user.setAuthorities(tuples.stream().filter(t -> t.getT2().isPresent()).map(t -> {
            return t.getT2().orElseThrow();
        }).collect(Collectors.toSet()));

        return user;
    }
}
