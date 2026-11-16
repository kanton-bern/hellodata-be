package ch.bedag.dap.hellodata.portal.metainfo.service;

import ch.bedag.dap.hellodata.portal.metainfo.data.UserSubsystemRolesDto;
import ch.bedag.dap.hellodata.portalcommon.userscache.UsersCacheBuilder;
import ch.bedag.dap.hellodata.portalcommon.userscache.data.DashboardUsersResultDto;
import ch.bedag.dap.hellodata.portalcommon.userscache.data.SubsystemUserDto;
import ch.bedag.dap.hellodata.portalcommon.userscache.data.SubsystemUsersResultDto;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ch.bedag.dap.hellodata.portalcommon.userscache.UsersCacheNames.SUBSYSTEM_USERS_CACHE;
import static ch.bedag.dap.hellodata.portalcommon.userscache.UsersCacheNames.USERS_WITH_DASHBOARD_CACHE;

@Log4j2
@Service
public class MetaInfoUsersService {
    private final UsersCacheBuilder usersCacheBuilder;
    private final MetaInfoUsersService self;

    public MetaInfoUsersService(UsersCacheBuilder usersCacheBuilder, @Lazy MetaInfoUsersService self) {
        this.usersCacheBuilder = usersCacheBuilder;
        this.self = self;
    }

    /**
     * The cache is refreshed by the sidecar for portal each time the subsystem users get published, here it is only built on a cache miss.
     */
    @Cacheable(value = SUBSYSTEM_USERS_CACHE)
    public List<SubsystemUsersResultDto> getAllUsersWithRoles() {
        return usersCacheBuilder.buildSubsystemUsers();
    }

    @Cacheable(value = USERS_WITH_DASHBOARD_CACHE)
    public List<DashboardUsersResultDto> getAllUsersWithRolesForDashboards() {
        return usersCacheBuilder.buildDashboardUsers();
    }

    /**
     * Returns a paginated, searchable view of subsystem users.
     * Inverts the cached subsystem-centric data into a user-centric structure.
     */
    @Transactional(readOnly = true)
    public Page<UserSubsystemRolesDto> getSubsystemUsersPaginated(Pageable pageable, String search) {
        List<SubsystemUsersResultDto> cached = self.getAllUsersWithRoles();
        List<UserSubsystemRolesDto> userCentric = buildUserCentricList(cached);
        return applySearchSortAndPaginate(userCentric, pageable, search);
    }

    /**
     * Returns a paginated, searchable view of dashboard users.
     * Inverts the cached dashboard-centric data into a user-centric structure.
     */
    @Transactional(readOnly = true)
    public Page<UserSubsystemRolesDto> getDashboardUsersPaginated(Pageable pageable, String search) {
        List<DashboardUsersResultDto> cached = self.getAllUsersWithRolesForDashboards();
        List<UserSubsystemRolesDto> userCentric = buildDashboardUserCentricList(cached);
        return applySearchSortAndPaginate(userCentric, pageable, search);
    }

    /**
     * Returns ALL subsystem users (user-centric, no pagination) for CSV export.
     */
    @Transactional(readOnly = true)
    public List<UserSubsystemRolesDto> getSubsystemUsersForExport(String search) {
        List<SubsystemUsersResultDto> cached = self.getAllUsersWithRoles();
        List<UserSubsystemRolesDto> userCentric = buildUserCentricList(cached);
        return applySearchFilter(userCentric, search);
    }

    /**
     * Returns ALL dashboard users (user-centric, no pagination) for CSV export.
     */
    @Transactional(readOnly = true)
    public List<UserSubsystemRolesDto> getDashboardUsersForExport(String search) {
        List<DashboardUsersResultDto> cached = self.getAllUsersWithRolesForDashboards();
        List<UserSubsystemRolesDto> userCentric = buildDashboardUserCentricList(cached);
        return applySearchFilter(userCentric, search);
    }

    private List<UserSubsystemRolesDto> buildUserCentricList(List<SubsystemUsersResultDto> subsystemData) {
        Map<String, UserSubsystemRolesDto> userMap = new LinkedHashMap<>();
        for (SubsystemUsersResultDto subsystem : subsystemData) {
            for (SubsystemUserDto user : subsystem.users()) {
                userMap.compute(user.email(), (email, existing) -> {
                    Map<String, List<String>> roles = existing != null
                            ? new LinkedHashMap<>(existing.subsystemRoles())
                            : new LinkedHashMap<>();
                    roles.put(subsystem.instanceName(), user.roles());
                    return new UserSubsystemRolesDto(
                            user.email(),
                            user.name(),
                            user.surname(),
                            user.enabled(),
                            user.businessDomainRole(),
                            user.dataDomainRoles(),
                            roles
                    );
                });
            }
        }
        return new ArrayList<>(userMap.values());
    }

    private List<UserSubsystemRolesDto> buildDashboardUserCentricList(List<DashboardUsersResultDto> dashboardData) {
        Map<String, UserSubsystemRolesDto> userMap = new LinkedHashMap<>();
        for (DashboardUsersResultDto subsystem : dashboardData) {
            String columnKey = subsystem.contextName() != null ? subsystem.contextName() : subsystem.instanceName();
            for (SubsystemUserDto user : subsystem.users()) {
                userMap.compute(user.email(), (email, existing) -> {
                    Map<String, List<String>> roles = existing != null
                            ? new LinkedHashMap<>(existing.subsystemRoles())
                            : new LinkedHashMap<>();
                    roles.put(columnKey, user.roles());
                    return new UserSubsystemRolesDto(
                            user.email(),
                            user.name(),
                            user.surname(),
                            user.enabled(),
                            user.businessDomainRole(),
                            user.dataDomainRoles(),
                            roles
                    );
                });
            }
        }
        return new ArrayList<>(userMap.values());
    }

    private Page<UserSubsystemRolesDto> applySearchSortAndPaginate(List<UserSubsystemRolesDto> users, Pageable pageable, String search) {
        List<UserSubsystemRolesDto> filtered = applySearchFilter(users, search);
        List<UserSubsystemRolesDto> sorted = applySort(filtered, pageable.getSort());
        int total = sorted.size();
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), total);
        List<UserSubsystemRolesDto> page = start < total ? sorted.subList(start, end) : List.of();
        return new PageImpl<>(page, pageable, total);
    }

    private List<UserSubsystemRolesDto> applySearchFilter(List<UserSubsystemRolesDto> users, String search) {
        if (search == null || search.isBlank()) {
            return users;
        }
        String[] terms = search.toLowerCase().split("\\s+");
        return users.stream()
                .filter(user -> matchesAllTerms(user, terms))
                .toList();
    }

    private boolean matchesAllTerms(UserSubsystemRolesDto user, String[] terms) {
        String searchableText = buildSearchableText(user);
        for (String term : terms) {
            if (!searchableText.contains(term)) {
                return false;
            }
        }
        return true;
    }

    private String buildSearchableText(UserSubsystemRolesDto user) {
        StringBuilder sb = new StringBuilder();
        sb.append(nullSafe(user.email())).append(' ');
        sb.append(nullSafe(user.firstName())).append(' ');
        sb.append(nullSafe(user.lastName())).append(' ');
        if (user.businessDomainRole() != null) {
            sb.append(user.businessDomainRole().name().replace('_', ' ')).append(' ');
            sb.append(user.businessDomainRole().name()).append(' ');
        }
        if (user.dataDomainRoles() != null) {
            for (var ddr : user.dataDomainRoles()) {
                sb.append(ddr.contextName()).append(' ');
                if (ddr.role() != null) {
                    sb.append(ddr.role().name().replace('_', ' ')).append(' ');
                    sb.append(ddr.role().name()).append(' ');
                }
            }
        }
        if (user.subsystemRoles() != null) {
            for (var entry : user.subsystemRoles().entrySet()) {
                sb.append(entry.getKey()).append(' ');
                for (String role : entry.getValue()) {
                    sb.append(role.replace('_', ' ')).append(' ');
                    sb.append(role).append(' ');
                }
            }
        }
        return sb.toString().toLowerCase();
    }

    private String nullSafe(String value) {
        return value != null ? value : "";
    }

    private List<UserSubsystemRolesDto> applySort(List<UserSubsystemRolesDto> users, Sort sort) {
        if (sort.isUnsorted()) {
            return users;
        }
        Sort.Order order = sort.iterator().next();
        Comparator<UserSubsystemRolesDto> comparator = getComparator(order.getProperty());
        if (order.isDescending()) {
            comparator = comparator.reversed();
        }
        return users.stream().sorted(comparator).toList();
    }

    private Comparator<UserSubsystemRolesDto> getComparator(String field) {
        return switch (field) {
            case "firstName" -> Comparator.comparing(u -> nullSafe(u.firstName()).toLowerCase());
            case "lastName" -> Comparator.comparing(u -> nullSafe(u.lastName()).toLowerCase());
            case "enabled" -> Comparator.comparing(UserSubsystemRolesDto::enabled);
            case "businessDomainRole" -> Comparator.comparing(u -> u.businessDomainRole() != null ? u.businessDomainRole().name() : "");
            default -> Comparator.comparing(u -> nullSafe(u.email()).toLowerCase());
        };
    }
}
