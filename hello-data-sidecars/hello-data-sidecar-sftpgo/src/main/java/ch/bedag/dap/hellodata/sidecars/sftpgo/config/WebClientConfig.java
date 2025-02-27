package ch.bedag.dap.hellodata.sidecars.sftpgo.config;

import ch.bedag.dap.hellodata.sidecars.sftpgo.client.invoker.ApiClient;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;

@Log4j2
@Configuration
public class WebClientConfig {

    @Value("${hello-data.sftpgo.base-url}")
    private String baseUrl;

    private static ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("Request: {} {}", clientRequest.method(), clientRequest.url());
            clientRequest.headers().forEach((name, values) -> values.forEach(value -> log.info("{}={}", name, value)));
            return Mono.just(clientRequest);
        });
    }

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
