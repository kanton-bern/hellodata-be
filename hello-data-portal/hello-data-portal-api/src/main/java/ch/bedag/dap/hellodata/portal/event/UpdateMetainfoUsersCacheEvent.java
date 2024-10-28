package ch.bedag.dap.hellodata.portal.event;

import org.springframework.context.ApplicationEvent;

public class UpdateMetainfoUsersCacheEvent extends ApplicationEvent {
    public UpdateMetainfoUsersCacheEvent(Object source) {
        super(source);
    }
}
