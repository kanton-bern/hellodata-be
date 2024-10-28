package ch.bedag.dap.hellodata.portal.event;

import org.springframework.context.ApplicationEvent;

public class UpdatePortalUsersCacheEvent extends ApplicationEvent {
    public UpdatePortalUsersCacheEvent(Object source) {
        super(source);
    }
}
