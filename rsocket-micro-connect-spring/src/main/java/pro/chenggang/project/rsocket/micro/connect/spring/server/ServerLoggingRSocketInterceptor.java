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
package pro.chenggang.project.rsocket.micro.connect.spring.server;

import io.rsocket.Payload;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.rsocket.MetadataExtractor;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.MimeType;
import org.springframework.util.MimeTypeUtils;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExchange;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExchangeType;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionAfterInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionBeforeInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionUnexpectedInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketInterceptorChain;
import pro.chenggang.project.rsocket.micro.connect.core.defaults.RemoteRSocketInfo;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionInterceptor.InterceptorType.SERVER;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.CONNECTOR_HEADER_METADATA_KEY;

/**
 * The Server side logging rsocket execution interceptor.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Slf4j
@RequiredArgsConstructor
public class ServerLoggingRSocketInterceptor implements RSocketExecutionBeforeInterceptor, RSocketExecutionAfterInterceptor, RSocketExecutionUnexpectedInterceptor, Ordered {

    private final String EXECUTION_INSTANT_ATTR_KEY = ServerLoggingRSocketInterceptor.class.getName() + ".execution-instant";
    private final String ROUTE_ATTR_KEY = ServerLoggingRSocketInterceptor.class.getName() + ".route";
    private final RSocketStrategies strategies;

    @Override
    public Mono<Void> interceptBefore(RSocketExchange exchange, RSocketInterceptorChain chain) {
        RSocketExchangeType rSocketExchangeType = exchange.getType();
        if (!rSocketExchangeType.isRequest()) {
            return chain.next(exchange);
        }
        Map<String, Object> attributes = exchange.getAttributes();
        attributes.put(EXECUTION_INSTANT_ATTR_KEY, Instant.now());
        Optional<Payload> optionalPayload = exchange.getPayload();
        if (optionalPayload.isPresent()) {
            Payload payload = optionalPayload.get();
            MimeType metadataMimeType = exchange.getMetadataMimeType(MimeTypeUtils::parseMimeType);
            Map<String, Object> extractedMetadata = strategies.metadataExtractor().extract(payload, metadataMimeType);
            String route = (String) extractedMetadata.get(MetadataExtractor.ROUTE_KEY);
            attributes.put(ROUTE_ATTR_KEY, route);
            Optional<RemoteRSocketInfo> optionalRemoteRSocketInfo = exchange.getRemoteRSocketInfo();
            if (optionalRemoteRSocketInfo.isPresent()) {
                log.info(" <== RSocket[{}]: {}, client: {}",
                        rSocketExchangeType,
                        route,
                        optionalRemoteRSocketInfo.get().getInfo()
                );
            } else {
                log.info(" <== RSocket[{}]: {}", rSocketExchangeType, route);
            }
            HttpHeaders httpHeaders = (HttpHeaders) extractedMetadata.get(CONNECTOR_HEADER_METADATA_KEY);
            if (Objects.nonNull(httpHeaders)) {
                httpHeaders.forEach((k, v) -> {
                    if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(k)) {
                        log.debug(" <== Connector header: Key -> {}, Value -> ******", k);
                        return;
                    }
                    log.debug(" <== Connector header: Key -> {}, Value -> {}", k, v);
                });
            }
        }
        return chain.next(exchange);
    }

    @Override
    public Mono<Void> interceptAfter(RSocketExchange exchange, RSocketInterceptorChain chain) {
        RSocketExchangeType rSocketExchangeType = exchange.getType();
        Instant executionInstant = exchange.getAttributeOrDefault(EXECUTION_INSTANT_ATTR_KEY, Instant.now());
        Duration costDuration = Duration.between(executionInstant, Instant.now());
        String route = exchange.getAttribute(ROUTE_ATTR_KEY);
        Optional<RemoteRSocketInfo> optionalInfo = exchange.getRemoteRSocketInfo();
        if (optionalInfo.isPresent()) {
            log.info("==> RSocket[{}]: {}, client: {}, Cost: {} ms",
                    rSocketExchangeType,
                    route,
                    optionalInfo.get().getInfo(),
                    costDuration.toMillis()
            );
        } else {
            log.info("==> RSocket[{}]: {}, Cost: {} ms", rSocketExchangeType, route, costDuration.toMillis());
        }
        return chain.next(exchange);
    }

    @Override
    public Mono<Void> interceptUnexpected(RSocketExchange exchange, RSocketInterceptorChain chain) {
        RSocketExchangeType rSocketExchangeType = exchange.getType();
        Instant executionInstant = exchange.getAttributeOrDefault(EXECUTION_INSTANT_ATTR_KEY, Instant.now());
        Duration costDuration = Duration.between(executionInstant, Instant.now());
        String route = exchange.getAttribute(ROUTE_ATTR_KEY);
        Optional<Throwable> optionalThrowable = exchange.getError();
        Optional<RemoteRSocketInfo> optionalInfo = exchange.getRemoteRSocketInfo();
        if (optionalInfo.isPresent()) {
            if (optionalThrowable.isPresent()) {
                log.error("==> RSocket[{}]: {}, client: {}, Cost: {} ms",
                        rSocketExchangeType,
                        route,
                        optionalInfo.get().getInfo(),
                        costDuration.toMillis(),
                        optionalThrowable.get()
                );
            } else {
                log.info("==> RSocket[{}]: {}, client: {}, Cost: {} ms",
                        rSocketExchangeType,
                        route,
                        optionalInfo.get().getInfo(),
                        costDuration.toMillis()
                );
            }
        } else {
            if (optionalThrowable.isPresent()) {
                log.error("==> RSocket[{}]: {}, Cost: {} ms",
                        rSocketExchangeType,
                        route,
                        costDuration.toMillis(),
                        optionalThrowable.get()
                );
            } else {
                log.info("==> RSocket[{}]: {}, Cost: {} ms", rSocketExchangeType, route, costDuration.toMillis());
            }
        }
        return chain.next(exchange);
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }

    @Override
    public InterceptorType interceptorType() {
        return SERVER;
    }
}
