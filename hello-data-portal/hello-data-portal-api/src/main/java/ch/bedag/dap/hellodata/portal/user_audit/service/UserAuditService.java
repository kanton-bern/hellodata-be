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
package ch.bedag.dap.hellodata.portal.user_audit.service;

import ch.bedag.dap.hellodata.portal.user_audit.data.*;
import ch.bedag.dap.hellodata.portal.user_audit.entity.UserAuditLogEntity;
import ch.bedag.dap.hellodata.portal.user_audit.repository.UserAuditLogRepository;
import ch.bedag.dap.hellodata.commons.security.SecurityUtils;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.metainfomodel.entity.HdContextEntity;
import ch.bedag.dap.hellodata.commons.metainfomodel.repository.HdContextRepository;
import jakarta.persistence.EntityManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Writes and reads the user audit log.
 * <p>
 * Changes are captured by snapshotting the whole user state before and after the work and storing the diff,
 * in the same transaction as the change. Nested audited calls for the same user are folded into the outermost one,
 * so one admin action produces one audit entry.
 */
@Log4j2
@Service
@RequiredArgsConstructor
public class UserAuditService {

    private static final ThreadLocal<Set<UUID>> USERS_IN_PROGRESS = ThreadLocal.withInitial(HashSet::new);

    private final UserAuditLogRepository userAuditLogRepository;
    private final UserStateSnapshotReader snapshotReader;
    private final HdContextRepository contextRepository;
    private final EntityManager entityManager;

    /**
     * Runs the work and records what it changed on the given user. Nothing is recorded if nothing changed.
     */
    @Transactional
    public <T> T audit(UUID userId, UserAuditAction action, UserAuditSource source, Supplier<T> work) {
        return doAudit(userId, action, source, work);
    }

    @Transactional
    public void audit(UUID userId, UserAuditAction action, UserAuditSource source, Runnable work) {
        doAudit(userId, action, source, () -> {
            work.run();
            return null;
        });
    }

    /**
     * Records a newly created user: the full state after creation is stored as the "after" values.
     */
    @Transactional
    public void recordCreated(UUID userId, UserAuditSource source) {
        UserStateSnapshot after = snapshot(userId);
        saveEntry(userId, after.email(), UserAuditAction.USER_CREATED, source, UserStateSnapshot.EMPTY.diff(after));
    }

    /**
     * Records changes computed by the caller, e.g. when a dashboard group is edited and the diff is known upfront.
     */
    @Transactional
    public void recordChanges(UUID userId, String targetEmail, UserAuditAction action, UserAuditSource source, List<UserAuditChange> changes) {
        saveEntry(userId, targetEmail, action, source, changes);
    }

    @Transactional(readOnly = true)
    public Page<UserAuditLogDto> getAuditLog(UUID userId, Pageable pageable) {
        return getAuditLog(userId, null, pageable);
    }

    /**
     * The audit log of a user, newest first. With a context key only the entries that touch that business / data domain
     * are returned, each with only its changes in that domain.
     */
    @Transactional(readOnly = true)
    public Page<UserAuditLogDto> getAuditLog(UUID userId, String contextKey, Pageable pageable) {
        Map<String, String> contextNames = contextRepository.findAllByTypeIn(List.of(HdContextType.BUSINESS_DOMAIN, HdContextType.DATA_DOMAIN)).stream()
                .collect(Collectors.toMap(HdContextEntity::getContextKey, HdContextEntity::getName, (a, b) -> a));
        if (contextKey == null || contextKey.isBlank()) {
            return userAuditLogRepository.findAllByTargetUserIdOrderByOccurredAtDesc(userId, pageable)
                    .map(entity -> toDto(entity, contextNames, null));
        }
        return userAuditLogRepository.findAllByTargetUserIdAndContextKey(userId, contextKey, pageable)
                .map(entity -> toDto(entity, contextNames, contextKey));
    }

    // private helpers below run in the transaction of the public method that called them

    private <T> T doAudit(UUID userId, UserAuditAction action, UserAuditSource source, Supplier<T> work) {
        if (userId == null) {
            return work.get();
        }
        Set<UUID> inProgress = USERS_IN_PROGRESS.get();
        if (!inProgress.add(userId)) {
            return work.get();
        }
        try {
            UserStateSnapshot before = snapshot(userId);
            T result = work.get();
            UserStateSnapshot after = snapshot(userId);
            saveEntry(userId, before.email() != null ? before.email() : after.email(), action, source, before.diff(after));
            return result;
        } finally {
            inProgress.remove(userId);
            if (inProgress.isEmpty()) {
                USERS_IN_PROGRESS.remove();
            }
        }
    }

    private void saveEntry(UUID userId, String targetEmail, UserAuditAction action, UserAuditSource source, List<UserAuditChange> changes) {
        if (changes == null || changes.isEmpty()) {
            log.debug("No changes for user {} ({}), skipping audit entry", userId, action);
            return;
        }
        UserAuditLogEntity entity = new UserAuditLogEntity();
        entity.setActorId(SecurityUtils.getCurrentUserId());
        entity.setActorEmail(SecurityUtils.getCurrentUserEmail());
        entity.setTargetUserId(userId);
        entity.setTargetEmail(targetEmail);
        entity.setAction(action);
        entity.setSource(source);
        entity.setContextKey(commonContextKey(changes));
        entity.setChanges(new ArrayList<>(changes));
        userAuditLogRepository.save(entity);
        log.debug("Audit entry {} for user {} with {} change(s)", action, userId, changes.size());
    }


    private UserStateSnapshot snapshot(UUID userId) {
        // push pending JPA changes to the DB so the SQL based snapshot sees them
        entityManager.flush();
        return snapshotReader.read(userId);
    }

    private static String commonContextKey(List<UserAuditChange> changes) {
        Set<String> contextKeys = changes.stream().map(UserAuditChange::contextKey).collect(Collectors.toSet());
        return contextKeys.size() == 1 ? contextKeys.iterator().next() : null;
    }

    private static UserAuditLogDto toDto(UserAuditLogEntity entity, Map<String, String> contextNames, String onlyContextKey) {
        List<UserAuditChangeDto> changes = entity.getChanges().stream()
                .filter(change -> onlyContextKey == null || onlyContextKey.equals(change.contextKey()))
                .map(change -> new UserAuditChangeDto(change.field(), change.contextKey(), contextNames.get(change.contextKey()), change.before(), change.after()))
                .toList();
        return new UserAuditLogDto(entity.getId(), entity.getOccurredAt(), entity.getActorId(), entity.getActorEmail(), entity.getTargetUserId(),
                entity.getTargetEmail(), entity.getAction(), entity.getSource(), entity.getContextKey(), contextNames.get(entity.getContextKey()), changes);
    }
}
