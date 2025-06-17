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

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.util.StringUtils;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExchange;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExchangeType;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionAfterInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionBeforeInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketInterceptorChain;
import pro.chenggang.project.rsocket.micro.connect.core.defaults.RemoteRSocketInfo;
import pro.chenggang.project.rsocket.micro.connect.spring.common.LoggingProperties;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionInterceptor.InterceptorType.SERVER;
import static pro.chenggang.project.rsocket.micro.connect.spring.common.AttributeLifecycleRSocketInterceptor.EXECUTION_INSTANT_ATTR_KEY;
import static pro.chenggang.project.rsocket.micro.connect.spring.common.AttributeLifecycleRSocketInterceptor.HEADERS_ATTR_KEY;
import static pro.chenggang.project.rsocket.micro.connect.spring.common.AttributeLifecycleRSocketInterceptor.ROUTE_ATTR_KEY;
import static pro.chenggang.project.rsocket.micro.connect.spring.common.LoggingProperties.LOGGING_ATTR_KEY;

/**
 * The Server side logging rsocket execution interceptor.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Slf4j
@RequiredArgsConstructor
public class ServerLoggingRSocketInterceptor implements RSocketExecutionBeforeInterceptor, RSocketExecutionAfterInterceptor {

    private final RSocketMicroConnectServerProperties serverProperties;

    @Override
    public Mono<Void> interceptBefore(RSocketExchange exchange, RSocketInterceptorChain chain) {
        RSocketExchangeType exchangeType = exchange.getType();
        if (!exchangeType.isRequest()) {
            return chain.next(exchange);
        }
        String route = exchange.getAttribute(ROUTE_ATTR_KEY);
        if (!StringUtils.hasText(route)) {
            return Mono.error(new IllegalStateException("Route is missing in the RSocket exchange attributes."));
        }
        LoggingProperties properties = serverProperties.getLogging();
        boolean loggingFlag = Objects.isNull(properties) || properties.isLoggingRoute(route);
        Map<String, Object> attributes = exchange.getAttributes();
        attributes.put(LOGGING_ATTR_KEY, loggingFlag);
        if (!loggingFlag) {
            return chain.next(exchange);
        }
        Optional<RemoteRSocketInfo> optionalRemoteRSocketInfo = exchange.getRemoteRSocketInfo();
        if (optionalRemoteRSocketInfo.isPresent()) {
            log.info(" <== RSocket[{}]: {}, client: {}", exchangeType, route, optionalRemoteRSocketInfo.get().getInfo());
        } else {
            log.info(" <== RSocket[{}]: {}", exchangeType, route);
        }
        HttpHeaders httpHeaders = exchange.getAttribute(HEADERS_ATTR_KEY);
        if (Objects.nonNull(httpHeaders)) {
            httpHeaders.forEach((k, v) -> {
                if (Objects.nonNull(properties) && properties.isLoggingHeader(k)) {
                    log.debug(" <== Connector header: Key -> {}, Value -> {}", k, v);
                }
            });
        }
        return chain.next(exchange);
    }

    @Override
    public Mono<Void> interceptAfter(RSocketExchange exchange, RSocketInterceptorChain chain) {
        RSocketExchangeType exchangeType = exchange.getType();
        if (!exchangeType.isRequest()) {
            return chain.next(exchange);
        }
        String route = exchange.getAttribute(ROUTE_ATTR_KEY);
        if (!StringUtils.hasText(route)) {
            return Mono.error(new IllegalStateException("Route is missing in the RSocket exchange attributes."));
        }
        boolean loggingFlag = exchange.getAttributeOrDefault(LOGGING_ATTR_KEY, true);
        if (!loggingFlag) {
            return chain.next(exchange);
        }
        Instant executionInstant = exchange.getAttributeOrDefault(EXECUTION_INSTANT_ATTR_KEY, Instant.now());
        Duration costDuration = Duration.between(executionInstant, Instant.now());
        Optional<Throwable> optionalThrowable = exchange.getError();
        Optional<RemoteRSocketInfo> optionalInfo = exchange.getRemoteRSocketInfo();
        if (optionalInfo.isPresent()) {
            if (optionalThrowable.isPresent()) {
                log.error("==> RSocket[{}]: {}, client: {}, Cost: {} ms",
                        exchangeType,
                        route,
                        optionalInfo.get().getInfo(),
                        costDuration.toMillis(),
                        optionalThrowable.get()
                );
            } else {
                log.info("==> RSocket[{}]: {}, client: {}, Cost: {} ms",
                        exchangeType,
                        route,
                        optionalInfo.get().getInfo(),
                        costDuration.toMillis()
                );
            }
        } else {
            if (optionalThrowable.isPresent()) {
                log.error("==> RSocket[{}]: {}, Cost: {} ms",
                        exchangeType,
                        route,
                        costDuration.toMillis(),
                        optionalThrowable.get()
                );
            } else {
                log.info("==> RSocket[{}]: {}, Cost: {} ms", exchangeType, route, costDuration.toMillis());
            }
        }
        return chain.next(exchange);
    }

    @Override
    public int order() {
        return Integer.MIN_VALUE + 1;
    }

    @Override
    public InterceptorType interceptorType() {
        return SERVER;
    }
}
