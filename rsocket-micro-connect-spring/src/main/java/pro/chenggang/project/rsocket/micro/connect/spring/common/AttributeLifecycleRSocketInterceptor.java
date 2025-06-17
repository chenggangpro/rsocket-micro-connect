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
package pro.chenggang.project.rsocket.micro.connect.spring.common;

import io.rsocket.Payload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.rsocket.MetadataExtractor;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExchange;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExchangeType;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionAfterInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionBeforeInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketInterceptorChain;
import pro.chenggang.project.rsocket.micro.connect.spring.client.RSocketMicroConnectClientProperties;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.CONNECTOR_HEADER_METADATA_KEY;

/**
 * The rsocket interceptor for attribute lifecycle.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Slf4j
@RequiredArgsConstructor
public class AttributeLifecycleRSocketInterceptor implements RSocketExecutionBeforeInterceptor, RSocketExecutionAfterInterceptor {

    /**
     * The constant EXECUTION_INSTANT_ATTR_KEY for saving the execution instant in the RSocket exchange attributes.
     */
    public static final String EXECUTION_INSTANT_ATTR_KEY = AttributeLifecycleRSocketInterceptor.class.getName() + ".execution-instant";

    /**
     * The constant ROUTE_ATTR_KEY for saving the route in the RSocket exchange attributes.
     */
    public static final String ROUTE_ATTR_KEY = AttributeLifecycleRSocketInterceptor.class.getName() + ".route";

    /**
     * The constant HEADERS_ATTR_KEY for saving the HTTP headers in the RSocket exchange attributes.
     */
    public static final String HEADERS_ATTR_KEY = RSocketMicroConnectClientProperties.class.getName() + ".http-headers";

    private final RSocketStrategies strategies;

    @Override
    public Mono<Void> interceptBefore(RSocketExchange exchange, RSocketInterceptorChain chain) {
        RSocketExchangeType rSocketExchangeType = exchange.getType();
        if (!rSocketExchangeType.isRequest()) {
            return chain.next(exchange);
        }
        Map<String, Object> attributes = exchange.getAttributes();
        if (attributes.containsKey(EXECUTION_INSTANT_ATTR_KEY)) {
            log.debug("Execution instant attribute already exists in the exchange attributes, skipping interception.");
            return chain.next(exchange);
        }
        attributes.put(EXECUTION_INSTANT_ATTR_KEY, Instant.now());
        Optional<Payload> optionalPayload = exchange.getPayload();
        boolean shouldLogging = true;
        if (optionalPayload.isPresent()) {
            Payload payload = optionalPayload.get();
            MimeType metadataMimeType = exchange.getMetadataMimeType(MimeTypeUtils::parseMimeType);
            Map<String, Object> extractedMetadata = strategies.metadataExtractor().extract(payload, metadataMimeType);
            String route = (String) extractedMetadata.get(MetadataExtractor.ROUTE_KEY);
            attributes.put(ROUTE_ATTR_KEY, route);
            HttpHeaders httpHeaders = (HttpHeaders) extractedMetadata.get(CONNECTOR_HEADER_METADATA_KEY);
            if (Objects.nonNull(httpHeaders)) {
                attributes.put(HEADERS_ATTR_KEY, httpHeaders);
            }
        } else {
            attributes.put(ROUTE_ATTR_KEY, "**NO ROUTE FOUND IN PAYLOAD**");
        }
        return chain.next(exchange);
    }

    @Override
    public Mono<Void> interceptAfter(RSocketExchange exchange, RSocketInterceptorChain chain) {
        return Mono.justOrEmpty(exchange.getAttributes())
                .filter(dataMap -> !dataMap.isEmpty())
                .flatMap(dataMap -> Mono.fromRunnable(dataMap::clear))
                .then(chain.next(exchange));
    }

    @Override
    public int order() {
        return Integer.MIN_VALUE;
    }

}
