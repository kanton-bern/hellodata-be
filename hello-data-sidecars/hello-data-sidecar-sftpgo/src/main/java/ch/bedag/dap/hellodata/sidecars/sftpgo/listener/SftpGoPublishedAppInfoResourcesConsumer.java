package ch.bedag.dap.hellodata.sidecars.sftpgo.listener;

import ch.bedag.dap.hellodata.commons.SlugifyUtil;
import ch.bedag.dap.hellodata.commons.nats.annotation.JetStreamSubscribe;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdBusinessContextInfo;
import ch.bedag.dap.hellodata.commons.sidecars.context.HdContextType;
import ch.bedag.dap.hellodata.commons.sidecars.resources.v1.appinfo.AppInfoResource;
import ch.bedag.dap.hellodata.sidecars.sftpgo.client.model.Permission;
import ch.bedag.dap.hellodata.sidecars.sftpgo.service.SftpGoService;
import lombok.AllArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.util.List;

import static ch.bedag.dap.hellodata.commons.sidecars.events.HDEvent.PUBLISH_APP_INFO_RESOURCES;

@Log4j2
@Service
@AllArgsConstructor
public class SftpGoPublishedAppInfoResourcesConsumer {

    public static final String ADMIN_GROUP_POSTFIX = "-admin";
    public static final String EDITOR_GROUP_POSTFIX = "-editor";
    public static final String VIEWER_GROUP_POSTFIX = "-viewer";

    private final SftpGoService sftpGoService;

    @SuppressWarnings("unused")
    @JetStreamSubscribe(event = PUBLISH_APP_INFO_RESOURCES)
    public void subscribe(AppInfoResource appInfoResource) {
        HdBusinessContextInfo businessContextInfo = appInfoResource.getBusinessContextInfo();
        HdBusinessContextInfo subContext = businessContextInfo.getSubContext();
        if (subContext != null && subContext.getType().equalsIgnoreCase(HdContextType.DATA_DOMAIN.getTypeName())) {
            log.info("------- Received appInfo resource {}, for the following context config {}", appInfoResource, businessContextInfo);
            String dataDomainKey = subContext.getKey();
            log.info("--> Creating missing groups with virtual folders for the data domain: {} ", dataDomainKey);
            String groupName = SlugifyUtil.slugify(dataDomainKey, "");
            sftpGoService.createGroup(dataDomainKey, subContext.getName(), groupName + ADMIN_GROUP_POSTFIX, List.of(Permission.STAR));
            sftpGoService.createGroup(dataDomainKey, subContext.getName(), groupName + EDITOR_GROUP_POSTFIX,
                    List.of(Permission.LIST,
                            Permission.DOWNLOAD,
                            Permission.RENAME,
                            Permission.OVERWRITE,
                            Permission.RENAME_DIRS,
                            Permission.COPY,
                            Permission.CREATE_DIRS));
            sftpGoService.createGroup(dataDomainKey, subContext.getName(), groupName + VIEWER_GROUP_POSTFIX,
                    List.of(Permission.LIST, Permission.DOWNLOAD));
        }
    }
}
