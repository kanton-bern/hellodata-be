package ch.bedag.dap.hellodata.sidecars.cloudbeaver.listener;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.nats.service.UsersSyncTriggerService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdBusinessContextInfo;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.appinfo.AppInfoResource;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.service.SecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.PUBLISH_APP_INFO_RESOURCES;

@Log4j2
@Service
@Transactional
@RequiredArgsConstructor
public class CbPublishedAppInfoResourcesConsumer {

    private final SecurityService securityService;
    private final UsersSyncTriggerService usersSyncTriggerService;
    @Value("${hello-data.instance.name}")
    private String instanceName;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = PUBLISH_APP_INFO_RESOURCES)
    public void subscribe(AppInfoResource appInfoResource) {
        HdBusinessContextInfo businessContextInfo = appInfoResource.getBusinessContextInfo();
        HdBusinessContextInfo subContext = businessContextInfo.getSubContext();
        if (subContext != null && subContext.getType().equalsIgnoreCase(HdContextType.DATA_DOMAIN.getTypeName())) {
            log.info("------- Received appInfo resource {}, for the following context config {}", appInfoResource, businessContextInfo);
            String dataDomainKey = subContext.getKey();
            log.info("--> Creating missing roles and privileges for data domain {} ", dataDomainKey);
            securityService.createDataDomainRoles(Set.of(dataDomainKey));
            usersSyncTriggerService.dataDomainRolesReady(ModuleType.CLOUDBEAVER, instanceName, dataDomainKey);
        }
    }
}
