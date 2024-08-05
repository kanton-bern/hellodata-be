package ch.bedag.dap.hellodata.log.cleanup.config.superset;

import java.util.List;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Data
@Validated
@ConfigurationProperties("spring")
public class SupersetDatasourceConfigurationProperties {
    private List<SupersetDataSourceConfigurationProperty> supersets;
}
