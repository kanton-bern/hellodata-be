package ch.bedag.dap.hellodata.sidecars.sftpgo.service.resource;


import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdBusinessContextInfo;
import ch.bedag.dap.hellodata.commons.sidecars.context.HelloDataContextConfig;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.appinfo.AppInfoResource;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.PUBLISH_APP_INFO_RESOURCES;

@Log4j2
@Service
@RequiredArgsConstructor
public class SftpGoAppInfoResourceProviderService {
    private final NatsSenderService natsSenderService;
    private final HelloDataContextConfig hellodataContextConfig;

    @Value("${hello-data.instance.url}")
    private String url;

    @Value("${hello-data.instance.name}")
    private String instanceName;

    @Scheduled(fixedDelayString = "${hello-data.sidecar.publish-interval-minutes:10}", timeUnit = TimeUnit.MINUTES)
    public void publishAppInfo() {
        log.info("--> publishAppInfo()");

        AppInfoResource appInfoResource = new AppInfoResource(createBusinessContextInfo(), this.instanceName, ModuleType.SFTPGO, this.url);
        natsSenderService.publishMessageToJetStream(PUBLISH_APP_INFO_RESOURCES, appInfoResource);
    }

    private HdBusinessContextInfo createBusinessContextInfo() {
        HdBusinessContextInfo businessContextInfo = new HdBusinessContextInfo();
        HelloDataContextConfig.BusinessContext businessContext = hellodataContextConfig.getBusinessContext();
        businessContextInfo.setType(businessContext.getType());
        businessContextInfo.setName(businessContext.getName());
        businessContextInfo.setKey(businessContext.getKey());
        businessContextInfo.setExtra(false);
        HelloDataContextConfig.Context context = hellodataContextConfig.getContext();
        if (context != null) {
            HdBusinessContextInfo subContext = new HdBusinessContextInfo();
            businessContextInfo.setSubContext(subContext);
            subContext.setType(context.getType());
            subContext.setName(context.getName());
            subContext.setKey(context.getKey());
            subContext.setExtra(context.isExtra());
        }
        return businessContextInfo;
    }
}
