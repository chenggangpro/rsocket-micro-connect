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

import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.HTTP_HEADER_METADATA_KEY;

/**
 * The Server side logging rsocket execution interceptor.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Slf4j
@RequiredArgsConstructor
public class ServerSideLoggingRSocketExecutionInterceptor implements RSocketExecutionBeforeInterceptor, RSocketExecutionAfterInterceptor, RSocketExecutionUnexpectedInterceptor, Ordered {

    private final String EXECUTION_INSTANT_ATTR_KEY = ServerSideLoggingRSocketExecutionInterceptor.class.getName() + ".execution-instant";
    private final String ROUTE_ATTR_KEY = ServerSideLoggingRSocketExecutionInterceptor.class.getName() + ".route";
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
            log.info("<== RSocket[{}] route: {}", rSocketExchangeType, route);
            attributes.put(ROUTE_ATTR_KEY, route);
            Optional<RemoteRSocketInfo> optionalRemoteRSocketInfo = exchange.getRemoteRSocketInfo();
            optionalRemoteRSocketInfo.ifPresent(remoteRSocketInfo -> {
                log.info("<== RSocket client address: {}", remoteRSocketInfo.getInfo());
            });
            HttpHeaders httpHeaders = (HttpHeaders) extractedMetadata.get(HTTP_HEADER_METADATA_KEY);
            if (Objects.nonNull(httpHeaders)) {
                httpHeaders.forEach((k, v) -> {
                    if (HttpHeaders.AUTHORIZATION.equalsIgnoreCase(k)) {
                        log.debug("<== Http header: Key -> {}, Value -> ******", k);
                        return;
                    }
                    log.debug("<== Http header: Key -> {}, Value -> {}", k, v);
                });
            }
        }
        return chain.next(exchange);
    }

    @Override
    public Mono<Void> interceptAfter(RSocketExchange exchange, RSocketInterceptorChain chain) {
        Instant executionInstant = exchange.getAttributeOrDefault(EXECUTION_INSTANT_ATTR_KEY, Instant.now());
        String route = exchange.getAttribute(ROUTE_ATTR_KEY);
        log.info("==> RSocket route: {}, Cost: {}", route, Duration.between(executionInstant, Instant.now()));
        return chain.next(exchange);
    }

    @Override
    public Mono<Void> interceptUnexpected(RSocketExchange exchange, RSocketInterceptorChain chain) {
        Instant executionInstant = exchange.getAttributeOrDefault(EXECUTION_INSTANT_ATTR_KEY, Instant.now());
        String route = exchange.getAttribute(ROUTE_ATTR_KEY);
        Optional<Throwable> optionalThrowable = exchange.getError();
        optionalThrowable.ifPresent(throwable -> log.error("==> RSocket route: {}", route, throwable));
        log.info("==> RSocket route: {}, Cost: {}", route, Duration.between(executionInstant, Instant.now()));
        return chain.next(exchange);
    }

    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
}
