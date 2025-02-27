package ch.bedag.dap.hellodata.sidecars.sftpgo.service.resource;


import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdBusinessContextInfo;
import ch.bedag.dap.hellodata.commons.sidecars.context.HelloDataContextConfig;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.appinfo.AppInfoResource;
import ch.bedag.dap.hellodata.sidecars.sftpgo.service.cloud.PodUtilsProvider;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.kubernetes.commons.PodUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.PUBLISH_APP_INFO_RESOURCES;

@Log4j2
@Service
@RequiredArgsConstructor
public class SftpGoAppInfoResourceProviderService {
    private final NatsSenderService natsSenderService;
    private final PodUtilsProvider podUtilsProvider;
    private final HelloDataContextConfig hellodataContextConfig;

    @Value("${hello-data.instance.url}")
    private String url;

    @Value("${hello-data.instance.name}")
    private String instanceName;

    @Scheduled(fixedDelayString = "${hello-data.sidecar.pubish-interval-minutes:10}", timeUnit = TimeUnit.MINUTES)
    public void publishAppInfo() {
        log.info("--> publishAppInfo()");

        PodUtils<V1Pod> podUtils = podUtilsProvider.getIfAvailable();
        if (podUtils != null) {
            V1Pod current = podUtils.currentPod().get();
            Map<String, Object> details = getDetails(current);

            AppInfoResource appInfoResource =
                    new AppInfoResource(createBusinessContextInfo(), this.instanceName, current.getMetadata().getNamespace(), ModuleType.SFTPGO, this.url);
            appInfoResource.getData().putAll(details);
            natsSenderService.publishMessageToJetStream(PUBLISH_APP_INFO_RESOURCES, appInfoResource);
        } else {
            //dummy info for tests
            AppInfoResource appInfoResource = new AppInfoResource(createBusinessContextInfo(), this.instanceName, "local", ModuleType.SFTPGO, this.url);
            natsSenderService.publishMessageToJetStream(PUBLISH_APP_INFO_RESOURCES, appInfoResource);
        }
    }

    private Map<String, Object> getDetails(V1Pod current) {
        Map<String, Object> details = new HashMap<>();
        details.put("namespace", current.getMetadata().getNamespace());
        details.put("podName", current.getMetadata().getName());
        details.put("podIp", current.getStatus().getPodIP());
        details.put("serviceAccount", current.getSpec().getServiceAccountName());
        details.put("nodeName", current.getSpec().getNodeName());
        details.put("hostIp", current.getStatus().getHostIP());
        return details;
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
