package ch.bedag.dap.hellodata.portal.event;

import org.springframework.context.ApplicationEvent;

public class UpdateKeycloakUsersCacheEvent extends ApplicationEvent {
    public UpdateKeycloakUsersCacheEvent(Object source) {
        super(source);
    }
}
