package ch.bedag.dap.hellodata.portal.monitoring.data;

import lombok.Data;

@Data
public class StorageSizeDto {
    private String name;
    private String path;
    private String usedSize;
    private String freeSpaceSize;
    private String totalAvailableSize;
}
