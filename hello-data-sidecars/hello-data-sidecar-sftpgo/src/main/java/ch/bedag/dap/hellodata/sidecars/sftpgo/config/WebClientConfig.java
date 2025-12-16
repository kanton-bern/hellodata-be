package ch.bedag.dap.hellodata.sidecars.sftpgo.config;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.ApiClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

@Log4j2
@Configuration
public class WebClientConfig {

    @Value("${hello-data.sftpgo.base-url}")
    private String baseUrl;

    @Bean
    public ApiClient sftpGoApiClient() {
        WebClient webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(
                        HttpClient.create().wiretap(true)
                ))
                .build();
        ApiClient apiClient = new ApiClient(webClient);
        apiClient.setBasePath(baseUrl + apiClient.getBasePath());
        return apiClient;
    }
}
