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
package pro.chenggang.project.rsocket.micro.connect.core.interceptor;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.util.RSocketProxy;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExchangeType;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionAfterInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionBeforeInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionUnexpectedInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.defaults.DefaultRSocketExchange;
import pro.chenggang.project.rsocket.micro.connect.core.defaults.RSocketExecutionAfterInterceptorChain;
import pro.chenggang.project.rsocket.micro.connect.core.defaults.RSocketExecutionBeforeInterceptorChain;
import pro.chenggang.project.rsocket.micro.connect.core.defaults.RSocketExecutionUnexpectedInterceptorChain;
import pro.chenggang.project.rsocket.micro.connect.core.defaults.RemoteRSocketInfo;
import pro.chenggang.project.rsocket.micro.connect.core.util.RSocketMicroConnectUtil;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.util.context.Context;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;

import static pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExchangeType.FIRE_AND_FORGET;
import static pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExchangeType.METADATA_PUSH;
import static pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExchangeType.REQUEST_CHANNEL;
import static pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExchangeType.REQUEST_RESPONSE;
import static pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExchangeType.REQUEST_STREAM;
import static pro.chenggang.project.rsocket.micro.connect.core.defaults.DefaultRSocketExchange.newExchange;

/**
 * The Chained intercepted rsocket.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Slf4j
public class ChainedInterceptedRSocket extends RSocketProxy {

    private static final String CLEANUP_ATTRIBUTE_KEY = ChainedInterceptedRSocket.class.getName() + ".cleanup-attribute-key";

    private final WellKnownMimeType dataMimeType;
    private final WellKnownMimeType metadataMimeType;
    private final RSocketExecutionBeforeInterceptorChain beforeChain;
    private final RSocketExecutionAfterInterceptorChain afterChain;
    private final RSocketExecutionUnexpectedInterceptorChain unexpectedChain;
    private final Cache<RSocket, RemoteRSocketInfo> remoteRSocketInfoCache;

    protected ChainedInterceptedRSocket(RSocket source,
                                        WellKnownMimeType dataMimeType,
                                        WellKnownMimeType metadataMimeType,
                                        List<RSocketExecutionBeforeInterceptor> beforeInterceptors,
                                        List<RSocketExecutionAfterInterceptor> afterInterceptors,
                                        List<RSocketExecutionUnexpectedInterceptor> unexpectedInterceptors) {
        this(source, dataMimeType, metadataMimeType, null, beforeInterceptors, afterInterceptors, unexpectedInterceptors);
    }

    protected ChainedInterceptedRSocket(RSocket source,
                                        WellKnownMimeType dataMimeType,
                                        WellKnownMimeType metadataMimeType,
                                        RemoteRSocketInfo remoteRSocketInfo,
                                        List<RSocketExecutionBeforeInterceptor> beforeInterceptors,
                                        List<RSocketExecutionAfterInterceptor> afterInterceptors,
                                        List<RSocketExecutionUnexpectedInterceptor> unexpectedInterceptors) {
        super(source);
        this.dataMimeType = dataMimeType;
        this.metadataMimeType = metadataMimeType;
        if (Objects.nonNull(remoteRSocketInfo)) {
            this.remoteRSocketInfoCache = Caffeine.newBuilder()
                    .initialCapacity(1)
                    .weakKeys()
                    .build();
            this.remoteRSocketInfoCache.put(source, remoteRSocketInfo);
        } else {
            this.remoteRSocketInfoCache = Caffeine.newBuilder()
                    .initialCapacity(3)
                    .weakKeys()
                    .build();
        }
        this.beforeChain = new RSocketExecutionBeforeInterceptorChain(beforeInterceptors);
        this.afterChain = new RSocketExecutionAfterInterceptorChain(afterInterceptors);
        this.unexpectedChain = new RSocketExecutionUnexpectedInterceptorChain(unexpectedInterceptors);
    }

    @Override
    public Mono<Void> fireAndForget(Payload payload) {
        return this.interceptMono(payload, FIRE_AND_FORGET, Mono.defer(() -> super.fireAndForget(payload)));
    }

    @Override
    public Mono<Payload> requestResponse(Payload payload) {
        return this.interceptMono(payload, REQUEST_RESPONSE, Mono.defer(() -> super.requestResponse(payload)));
    }

    @Override
    public Flux<Payload> requestStream(Payload payload) {
        return this.interceptFlux(payload, REQUEST_STREAM, Flux.defer(() -> super.requestStream(payload)));
    }

    @Override
    public Flux<Payload> requestChannel(Publisher<Payload> payloads) {
        return Flux.deferContextual(contextView -> Mono.justOrEmpty(contextView)
                .defaultIfEmpty(Context.empty())
                .flatMapMany(context -> {
                    return Flux.usingWhen(
                                    initializeAttributes(),
                                    attributes -> {
                                        return Mono.just(this.beforeChain)
                                                .flatMapMany(chain -> {
                                                    return Flux.from(payloads)
                                                            .concatMap(payload -> {
                                                                DefaultRSocketExchange exchange = newExchange(
                                                                        REQUEST_CHANNEL,
                                                                        payload,
                                                                        dataMimeType,
                                                                        metadataMimeType,
                                                                        attributes
                                                                );
                                                                return chain.next(exchange)
                                                                        .thenReturn(payload);
                                                            });
                                                })
                                                .as(super::requestChannel);
                                    },
                                    attributes -> invokeOnComplete(REQUEST_CHANNEL, attributes),
                                    (attributes, err) -> invokeOnError(REQUEST_CHANNEL, attributes, err),
                                    attributes -> invokeOnCancel(REQUEST_CHANNEL, attributes)
                            )
                            .contextWrite(context);
                })
        );
    }

    @Override
    public Mono<Void> metadataPush(Payload payload) {
        return this.interceptMono(payload, METADATA_PUSH, Mono.defer(() -> super.metadataPush(payload)));
    }

    protected <T> Mono<T> interceptMono(Payload payload, RSocketExchangeType rSocketExchangeType, Mono<T> monoExecution) {
        return Mono.deferContextual(contextView -> Mono.justOrEmpty(contextView)
                .defaultIfEmpty(Context.empty())
                .flatMap(context -> {
                    return Mono.usingWhen(
                                    initializeAttributes(),
                                    attributes -> {
                                        return Mono.just(this.beforeChain)
                                                .flatMap(chain -> {
                                                    return Mono.defer(() -> {
                                                        DefaultRSocketExchange exchange = newExchange(rSocketExchangeType,
                                                                payload,
                                                                dataMimeType,
                                                                metadataMimeType,
                                                                attributes
                                                        );
                                                        return chain.next(exchange);
                                                    });
                                                })
                                                .then(monoExecution);
                                    },
                                    attributes -> invokeOnComplete(rSocketExchangeType, attributes),
                                    (attributes, err) -> invokeOnError(rSocketExchangeType, attributes, err),
                                    attributes -> invokeOnCancel(rSocketExchangeType, attributes)
                            )
                            .contextWrite(context);
                })
        );
    }

    protected <T> Flux<T> interceptFlux(Payload payload, RSocketExchangeType rSocketExchangeType, Flux<T> fluxExecution) {
        return Flux.deferContextual(contextView -> Mono.justOrEmpty(contextView)
                .defaultIfEmpty(Context.empty())
                .flatMapMany(context -> {
                    return Flux.usingWhen(
                                    initializeAttributes(),
                                    attributes -> {
                                        return Mono.just(this.beforeChain)
                                                .flatMap(chain -> {
                                                    return Mono.defer(() -> {
                                                        DefaultRSocketExchange exchange = newExchange(rSocketExchangeType,
                                                                payload,
                                                                dataMimeType,
                                                                metadataMimeType,
                                                                attributes
                                                        );
                                                        return chain.next(exchange);
                                                    });
                                                })
                                                .thenMany(fluxExecution);
                                    },
                                    attributes -> invokeOnComplete(rSocketExchangeType, attributes),
                                    (attributes, err) -> invokeOnError(rSocketExchangeType, attributes, err),
                                    attributes -> invokeOnCancel(rSocketExchangeType, attributes)
                            )
                            .contextWrite(context);
                })
        );
    }

    private Mono<Map<String, Object>> initializeAttributes() {
        return Mono.fromSupplier(() -> {
            final ConcurrentHashMap<String, Object> attributes = new ConcurrentHashMap<>();
            attributes.put(CLEANUP_ATTRIBUTE_KEY, new AtomicBoolean(false));
            RemoteRSocketInfo remoteRSocketInfo = this.remoteRSocketInfoCache.get(source,
                    rsocket -> {
                        Optional<RemoteRSocketInfo> optionalInfo = RSocketMicroConnectUtil.getRemoteRSocketInfo(source);
                        if (optionalInfo.isEmpty()) {
                            log.debug("Can not get remote rsocket info from RSocket instance :{}", source);
                            return null;
                        }
                        return optionalInfo.get();
                    }
            );
            if (Objects.nonNull(remoteRSocketInfo)) {
                attributes.putIfAbsent(RemoteRSocketInfo.class.getName(), remoteRSocketInfo);
            }
            return attributes;
        });
    }

    private Mono<Void> invokeOnComplete(RSocketExchangeType rSocketExchangeType, Map<String, Object> attributes) {
        return this.checkCleanupAttribute(attributes, true)
                .filter(Boolean::booleanValue)
                .flatMap(__ -> {
                    DefaultRSocketExchange exchange = newExchange(rSocketExchangeType,
                            dataMimeType,
                            metadataMimeType,
                            attributes
                    );
                    return afterChain.next(exchange);
                })
                .then(Mono.defer(() -> this.cleanupAttributes(attributes)));
    }

    private Mono<Void> invokeOnError(RSocketExchangeType rSocketExchangeType,
                                     Map<String, Object> attributes,
                                     Throwable err) {
        return this.checkCleanupAttribute(attributes, true)
                .filter(Boolean::booleanValue)
                .flatMap(__ -> {
                    DefaultRSocketExchange exchange = newExchange(rSocketExchangeType,
                            dataMimeType,
                            metadataMimeType,
                            attributes,
                            err
                    );
                    return unexpectedChain.next(exchange);
                })
                .then(Mono.defer(() -> this.cleanupAttributes(attributes)));
    }

    private Mono<?> invokeOnCancel(RSocketExchangeType rSocketExchangeType, Map<String, Object> attributes) {
        return this.checkCleanupAttribute(attributes, false)
                .filter(Boolean::booleanValue)
                .flatMap(__ -> {
                    DefaultRSocketExchange exchange = newExchange(rSocketExchangeType,
                            dataMimeType,
                            metadataMimeType,
                            attributes
                    );
                    return afterChain.next(exchange);
                })
                .then(Mono.defer(() -> this.cleanupAttributes(attributes)));
    }

    private Mono<Void> cleanupAttributes(Map<String, Object> attributes) {
        return Mono.justOrEmpty(attributes)
                .filter(dataMap -> !dataMap.isEmpty())
                .flatMap(dataMap -> Mono.fromRunnable(dataMap::clear))
                .then();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName()
                + "[source=" + this.source
                + ", BeforeChain=" + this.beforeChain
                + ", AfterChain=" + this.afterChain
                + ", FinallyChain=" + this.unexpectedChain
                + "]";
    }

    /**
     * Check cleanup attribute.
     *
     * @param attributes the attributes
     * @return the cleanup flag
     */
    protected Mono<Boolean> checkCleanupAttribute(@NonNull Map<String, Object> attributes, boolean checkUnCleanup) {
        Object cleanupAttribute = attributes.get(CLEANUP_ATTRIBUTE_KEY);
        if (Objects.isNull(cleanupAttribute) || !(cleanupAttribute instanceof AtomicBoolean)) {
            return Mono.error(new IllegalStateException("The cleanup attribute can not be removed manually"));
        }
        if (checkUnCleanup && ((AtomicBoolean) cleanupAttribute).get()) {
            return Mono.error(new IllegalStateException("The cleanup attribute can not be modified manually"));
        }
        return Mono.just(((AtomicBoolean) cleanupAttribute).compareAndSet(false, true));
    }
}
