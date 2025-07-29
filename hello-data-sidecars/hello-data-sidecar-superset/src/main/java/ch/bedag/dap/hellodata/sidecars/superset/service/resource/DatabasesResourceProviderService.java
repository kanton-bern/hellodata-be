package ch.bedag.dap.hellodata.sidecars.superset.service.resource;


import ch.bedag.dap.hellodata.commons.nats.service.NatsSenderService;
import ch.bedag.dap.hellodata.commons.sidecars.modules.ModuleType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.database.DatabaseResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.database.response.superset.SupersetDatabaseResponse;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.permission.PermissionResource;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.permission.response.superset.SupersetPermissionResponse;
import ch.bedag.dap.hellodata.sidecars.superset.service.client.SupersetClientProvider;
import ch.bedag.dap.hellodata.sidecars.superset.service.cloud.PodUtilsProvider;
import io.kubernetes.client.openapi.models.V1Pod;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.kubernetes.commons.PodUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.concurrent.TimeUnit;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.PUBLISH_PERMISSION_RESOURCES;

@Log4j2
@Service
@RequiredArgsConstructor
public class DatabasesResourceProviderService {
    private final NatsSenderService natsSenderService;
    private final SupersetClientProvider supersetClientProvider;
    private final PodUtilsProvider podUtilsProvider;
    @Value("${hello-data.instance.name}")
    private String instanceName;

    @Scheduled(fixedDelayString = "${hello-data.sidecar.publish-interval-minutes:10}", timeUnit = TimeUnit.MINUTES)
    public void publishDatabases() throws URISyntaxException, IOException {
        log.info("--> publishDatabases()");
        SupersetDatabaseResponse response = supersetClientProvider.getSupersetClientInstance().databases();

        PodUtils<V1Pod> podUtils = podUtilsProvider.getIfAvailable();
        if (podUtils != null) {
            V1Pod current = podUtils.currentPod().get();
            DatabaseResource permissionResource = new DatabaseResource(ModuleType.SUPERSET, this.instanceName, current.getMetadata().getNamespace(), response.getResult());
            natsSenderService.publishMessageToJetStream(PUBLISH_PERMISSION_RESOURCES, permissionResource);
        } else {
            //dummy info for tests
            DatabaseResource permissionResource = new DatabaseResource(ModuleType.SUPERSET, this.instanceName, "local", response.getResult());
            natsSenderService.publishMessageToJetStream(PUBLISH_PERMISSION_RESOURCES, permissionResource);
        }
    }
}
