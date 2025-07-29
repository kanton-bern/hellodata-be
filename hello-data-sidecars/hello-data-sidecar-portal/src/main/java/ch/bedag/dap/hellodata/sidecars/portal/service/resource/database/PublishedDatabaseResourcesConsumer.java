package ch.bedag.dap.hellodata.sidecars.portal.service.resource.database;

import ch.bedag.dap.hellodata.commons.metainfomodel.entities.MetaInfoResourceEntity;
import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.database.DatabaseResource;
import ch.bedag.dap.hellodata.sidecars.portal.service.resource.GenericPublishedResourceConsumer;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.PUBLISH_DASHBOARD_RESOURCES;
import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.PUBLISH_DATABASE_RESOURCES;

@Log4j2
@Service
@AllArgsConstructor
public class PublishedDatabaseResourcesConsumer {

    private final GenericPublishedResourceConsumer genericPublishedResourceConsumer;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = PUBLISH_DATABASE_RESOURCES)
    public void subscribe(DatabaseResource databaseResource) {
        log.info("------- Received database resource {}", databaseResource);
        MetaInfoResourceEntity resource = genericPublishedResourceConsumer.persistResource(databaseResource);
        genericPublishedResourceConsumer.attachContext(databaseResource, resource);
    }
}
