package ch.bedag.dap.hellodata.commons.sidecars.resources.v1.dashboard;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.io.Serializable;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class DashboardUpload implements Serializable {
    private byte[] content;
    private int chunkNumber;
    private String filename;
    private String binaryFileId;
    private boolean lastChunk;
    private long fileSize;
}
