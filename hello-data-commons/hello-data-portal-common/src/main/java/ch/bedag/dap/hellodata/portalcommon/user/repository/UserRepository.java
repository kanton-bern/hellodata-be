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
package ch.bedag.dap.hellodata.portalcommon.user.repository;

import ch.bedag.dap.hellodata.commons.sidecars.context.role.HdRoleName;
import ch.bedag.dap.hellodata.portalcommon.user.entity.UserEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserRepository extends JpaRepository<UserEntity, UUID> {

    Optional<UserEntity> findUserEntityByEmailIgnoreCase(String email);

    Optional<UserEntity> findUserEntityByEmailIgnoreCaseAndUsernameIgnoreCase(String email, String username);

    /**
     * There is a possibility to remove user directly from keycloak and add it again.
     * Thus, we don't delete one in portal but re-set it's auth_id to have portal <-> keycloak connection
     * That's why search goes
     *
     * @param authId - either id or auth_id kolumn
     * @return
     */
    @Query(nativeQuery = true, value = "SELECT u.* FROM user_ u WHERE u.id = CAST(:param AS uuid) OR u.auth_Id = :param")
    UserEntity getByIdOrAuthId(@Param("param") String authId);

    boolean existsByIdOrAuthId(UUID id, String authId);

    @Query(nativeQuery = true,
            value = "select u.* from user_ u join user_context_role r " + "on u.id = r.user_id and r.id in (" + "select ucr.id from user_context_role ucr " + "join role r " +
                    "on ucr.role_id = r.id " + "where name = :#{#roleName?.name()})")
    List<UserEntity> findUsersByHdRoleName(@Param("roleName") HdRoleName roleName);

    List<UserEntity> findAllByUsernameIsNull();

    @Query("SELECT u.selectedLanguage FROM user_ u WHERE u.email = :email")
    Locale findSelectedLanguageByEmail(@Param("email") String email);

    @Query("SELECT u FROM user_ u WHERE " +
            "LOWER(u.firstName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.lastName) LIKE LOWER(CONCAT('%', :search, '%')) " +
            "OR LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<UserEntity> findAll(Pageable pageable, @Param("search") String search);

    Page<UserEntity> findAll(Pageable pageable);
}
