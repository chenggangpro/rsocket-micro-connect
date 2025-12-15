/*
 *    Copyright 2025 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package pro.chenggang.project.rsocket.micro.connect.example.client.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
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
        // this will auto propagate ServerWebExchange from WebFlux's Context to RSocket Connector
        return connectorExecution -> {
            return Mono.deferContextual(contextView -> {
                        return Mono.justOrEmpty(contextView.getOrEmpty(ServerWebExchangeContextFilter.EXCHANGE_CONTEXT_ATTRIBUTE))
                                .ofType(ServerWebExchange.class)
                                .switchIfEmpty(Mono.error(new IllegalStateException("No ServerWebExchange found in context")));
                    })
                    .flatMap(serverWebExchange -> {
                        return Mono.fromRunnable(() -> {
                            HttpHeaders headers = serverWebExchange.getRequest().getHeaders();
                            connectorExecution.addHeaders(headers);
                        });
                    });
        };
    }
}
