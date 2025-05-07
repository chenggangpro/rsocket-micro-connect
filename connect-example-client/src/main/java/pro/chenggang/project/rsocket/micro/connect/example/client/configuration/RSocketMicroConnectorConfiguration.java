package pro.chenggang.project.rsocket.micro.connect.example.client.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.filter.reactive.ServerWebExchangeContextFilter;
import org.springframework.web.server.ServerWebExchange;
import pro.chenggang.project.rsocket.micro.connect.spring.proxy.RSocketMicroConnectorExecutionCustomizer;
import reactor.core.publisher.Mono;

/**
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Configuration(proxyBeanMethods = false)
public class RSocketMicroConnectorConfiguration {

    @Bean
    public RSocketMicroConnectorExecutionCustomizer autoPropagateHeadersCustomizer() {
        return connectorExecution -> {
            return Mono.deferContextual(contextView -> {
                        return Mono.justOrEmpty(contextView.getOrEmpty(ServerWebExchangeContextFilter.EXCHANGE_CONTEXT_ATTRIBUTE))
                                .ofType(ServerWebExchange.class)
                                .switchIfEmpty(Mono.error(new IllegalStateException("No ServerWebExchange found in context")));
                    })
                    .flatMap(serverWebExchange -> {
                        return Mono.fromRunnable(() -> connectorExecution.addHeaders(serverWebExchange.getRequest()
                                .getHeaders())
                        );
                    });
        };
    }
}
