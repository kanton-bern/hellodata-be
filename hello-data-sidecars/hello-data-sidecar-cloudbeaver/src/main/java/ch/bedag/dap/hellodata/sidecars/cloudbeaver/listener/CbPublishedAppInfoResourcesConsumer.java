package ch.bedag.dap.hellodata.sidecars.cloudbeaver.listener;

import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdBusinessContextInfo;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.appinfo.AppInfoResource;
import ch.bedag.dap.hellodata.sidecars.cloudbeaver.service.SecurityService;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.PUBLISH_APP_INFO_RESOURCES;

@Log4j2
@Service
@Transactional
@AllArgsConstructor
public class CbPublishedAppInfoResourcesConsumer {

    private final SecurityService securityService;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = PUBLISH_APP_INFO_RESOURCES)
    public CompletableFuture<Void> subscribe(AppInfoResource appInfoResource) {
        HdBusinessContextInfo businessContextInfo = appInfoResource.getBusinessContextInfo();
        HdBusinessContextInfo subContext = businessContextInfo.getSubContext();
        if (subContext != null && subContext.getType().equalsIgnoreCase(HdContextType.DATA_DOMAIN.getTypeName())) {
            log.info("------- Received appInfo resource {}, for the following context config {}", appInfoResource, businessContextInfo);
            String dataDomainKey = subContext.getKey();
            log.info("--> Creating missing roles and privileges for data domain {} ", dataDomainKey);
            securityService.createDataDomainRoles(Set.of(dataDomainKey));
        }
        return null;
    }
}
