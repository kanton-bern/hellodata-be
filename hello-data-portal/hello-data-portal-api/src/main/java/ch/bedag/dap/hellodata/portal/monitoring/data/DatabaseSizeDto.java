package ch.bedag.dap.hellodata.portal.monitoring.data;

import lombok.Data;

@Data
public class DatabaseSizeDto {
    private String name;
    private String usedSize;
    private String totalAvailableSize;
}
