package pro.chenggang.project.rsocket.micro.connect.spring.client.loadbalance;

import io.rsocket.DuplexConnection;
import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.transport.ClientTransport;
import lombok.Getter;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketRequester.Builder;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.util.UriComponentsBuilder;
import pro.chenggang.project.rsocket.micro.connect.core.exception.RSocketInstanceNotFoundException;
import pro.chenggang.project.rsocket.micro.connect.core.util.RSocketMicroConnectUtil;
import pro.chenggang.project.rsocket.micro.connect.spring.client.CachedRSocketRequesterRegistry;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.util.function.Tuples;

import java.net.URI;
import java.time.Duration;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import static pro.chenggang.project.rsocket.micro.connect.spring.client.loadbalance.DiscoverRSocketRequesterRegistry.RSocketServiceInstanceData.CURRENT_REFRESHER_RUNNING_CONTEXT_KEY;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.DISCOVER_ENABLE_RSOCKET_METADATA_KEY;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.DISCOVER_RSOCKET_PORT_METADATA_KEY;
import static reactor.core.publisher.Sinks.EmitResult.FAIL_NON_SERIALIZED;

/**
 * The discover rsocket requester registry for spring cloud.
 * This get a rsocket requester with load-balancing service from a discovery like eureka or nacos
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Slf4j
public class DiscoverRSocketRequesterRegistry extends CachedRSocketRequesterRegistry {

    private final Map<URI, RSocketServiceInstanceData> rSocketServiceInstanceDataCache = new ConcurrentHashMap<>();
    private final Map<URI, Disposable> rSocketServiceInstanceRefresherCache = new ConcurrentHashMap<>();
    private final ReactiveDiscoveryClient reactiveDiscoveryClient;
    private final RSocketLoadbalanceStrategies rSocketLoadbalanceStrategies;
    private final Duration refreshInterval;

    public DiscoverRSocketRequesterRegistry(Builder builder,
                                            ReactiveDiscoveryClient reactiveDiscoveryClient,
                                            RSocketLoadbalanceStrategies rSocketLoadbalanceStrategies,
                                            Duration refreshInterval) {
        super(builder);
        this.reactiveDiscoveryClient = reactiveDiscoveryClient;
        this.rSocketLoadbalanceStrategies = rSocketLoadbalanceStrategies;
        this.refreshInterval = refreshInterval;
    }

    @Override
    protected RSocketRequester newRSocketRequester(@NonNull URI transportURI) {
        String host = transportURI.getHost();
        boolean anIpAddress = RSocketMicroConnectUtil.isAnIpAddress(host);
        if (anIpAddress) {
            return super.newRSocketRequester(transportURI);
        }
        RSocketServiceInstanceData rSocketServiceInstanceData = rSocketServiceInstanceDataCache.computeIfAbsent(transportURI,
                uri -> new RSocketServiceInstanceData(transportURI)
        );
        rSocketServiceInstanceRefresherCache.computeIfAbsent(transportURI, this::newRSocketServiceInstanceRefresher);
        return builder.transports(rSocketServiceInstanceData.getInstances(),
                rSocketLoadbalanceStrategies.getLoadbalanceStrategy(transportURI.getHost())
        );
    }

    @Override
    public void destroy() throws Exception {
        rSocketServiceInstanceRefresherCache.forEach((uri, disposable) -> {
            if (!disposable.isDisposed()) {
                disposable.dispose();
                log.info("Dispose sinks refresher for uri:{}", uri);
            }
        });
        rSocketServiceInstanceRefresherCache.clear();
        rSocketServiceInstanceDataCache.forEach((uri, rSocketServiceInstanceData) -> {
            rSocketServiceInstanceData.emitComplete();
            log.info("Complete sinks for uri:{}", uri);
        });
        rSocketServiceInstanceDataCache.clear();
        super.destroy();
    }

    private Disposable newRSocketServiceInstanceRefresher(URI transportURI) {
        return Flux.interval(Duration.ZERO, refreshInterval)
                .flatMap(__ -> {
                    if (!rSocketServiceInstanceDataCache.containsKey(transportURI)) {
                        return Mono.error(new IllegalStateException("Can not found RSocketServiceInstanceData from cache"));
                    }
                    return this.loadRSocketServiceInstance(transportURI);
                })
                .onErrorResume(Throwable.class, throwable -> {
                    log.error("Exception occurred while getting LoadBalanceTarget from discover client", throwable);
                    return Mono.empty();
                })
                .subscribe();
    }

    private Mono<Boolean> loadRSocketServiceInstance(URI transportURI) {
        return Mono.usingWhen(
                        Mono.fromSupplier(() -> rSocketServiceInstanceDataCache.get(transportURI))
                                .switchIfEmpty(Mono.error(new IllegalStateException(
                                        "Can not found RSocketServiceInstanceData from cache for" + transportURI))
                                ),
                        rSocketServiceInstanceData -> {
                            if (rSocketServiceInstanceData.getRefreshRunning().compareAndSet(false, true)) {
                                return Mono.deferContextual(contextView -> Mono.justOrEmpty(contextView.getOrEmpty(
                                                        CURRENT_REFRESHER_RUNNING_CONTEXT_KEY))
                                                .ofType(AtomicBoolean.class)
                                                .switchIfEmpty(Mono.error(new IllegalStateException(
                                                        "Can not find current refresher running flag from context"))
                                                )
                                        )
                                        .flatMap(currentRunningFlag -> {
                                            if (currentRunningFlag.compareAndSet(false, true)) {
                                                return Mono.zip(
                                                                this.getServiceInstancesFromDiscover(rSocketServiceInstanceData.getTransportURI()),
                                                                this.getExistServiceInstance(rSocketServiceInstanceData)
                                                        )
                                                        .flatMap(tuple2 -> this.refreshLoadBalanceTarget(tuple2.getT1(),
                                                                tuple2.getT2(),
                                                                rSocketServiceInstanceData.getTransportURI(),
                                                                rSocketServiceInstanceData
                                                        ));
                                            }
                                            return Mono.just(false);
                                        });
                            }
                            return Mono.just(false);
                        },
                        DiscoverRSocketRequesterRegistry::resetRunningFlag,
                        (rSocketServiceInstanceData, throwable) -> resetRunningFlag(rSocketServiceInstanceData),
                        DiscoverRSocketRequesterRegistry::resetRunningFlag
                )
                .contextWrite(context -> context.put(CURRENT_REFRESHER_RUNNING_CONTEXT_KEY, new AtomicBoolean(false)));
    }

    private static Mono<Object> resetRunningFlag(RSocketServiceInstanceData rSocketServiceInstanceData) {
        return Mono.deferContextual(contextView -> Mono.justOrEmpty(contextView.getOrEmpty(
                                CURRENT_REFRESHER_RUNNING_CONTEXT_KEY))
                        .ofType(AtomicBoolean.class)
                        .switchIfEmpty(Mono.error(new IllegalStateException(
                                "Can not find current refresher running flag from context"))
                        )
                )
                .filter(AtomicBoolean::get)
                .flatMap(currentRunningFlag -> {
                    if (currentRunningFlag.compareAndSet(true, false)) {
                        rSocketServiceInstanceData.getRefreshRunning().compareAndSet(true, false);
                        return Mono.empty();
                    }
                    return Mono.empty();
                });
    }

    private Mono<List<LoadbalanceTarget>> getExistServiceInstance(RSocketServiceInstanceData rSocketServiceInstanceData) {
        return rSocketServiceInstanceData.getInstances().next().defaultIfEmpty(Collections.emptyList());
    }

    private Mono<List<URI>> getServiceInstancesFromDiscover(URI transportURI) {
        return reactiveDiscoveryClient.getInstances(transportURI.getHost())
                .mapNotNull(serviceInstance -> {
                    Map<String, String> metadata = serviceInstance.getMetadata();
                    if (Objects.nonNull(metadata) && !metadata.isEmpty()) {
                        String discoverEnableRSocket = metadata.get(DISCOVER_ENABLE_RSOCKET_METADATA_KEY);
                        if ("false".equalsIgnoreCase(discoverEnableRSocket)) {
                            log.debug("Disabled rsocket service {} from metadata: {}",
                                    serviceInstance.getUri(),
                                    DISCOVER_ENABLE_RSOCKET_METADATA_KEY
                            );
                            return null;
                        }
                        String rsocketServerPortFromMetadata = metadata.get(DISCOVER_RSOCKET_PORT_METADATA_KEY);
                        if (StringUtils.hasText(rsocketServerPortFromMetadata)) {
                            int port = -1;
                            try {
                                port = Integer.parseInt(rsocketServerPortFromMetadata);
                            } catch (NumberFormatException e) {
                                log.debug(
                                        "Failed to parse custom rsocket service port {} from metadata: {}",
                                        rsocketServerPortFromMetadata,
                                        DISCOVER_RSOCKET_PORT_METADATA_KEY
                                );
                            }
                            if (port > 0) {
                                return UriComponentsBuilder.fromUri(transportURI)
                                        .host(serviceInstance.getHost())
                                        .port(port)
                                        .build()
                                        .toUri();
                            }
                            return null;
                        }
                    }
                    return UriComponentsBuilder.fromUri(transportURI)
                            .host(serviceInstance.getHost())
                            .build()
                            .toUri();
                })
                .distinct()
                .collectList();
    }

    private Mono<Boolean> refreshLoadBalanceTarget(List<URI> uriList,
                                                   List<LoadbalanceTarget> existsLoadBalanceTargetList,
                                                   URI transportURI,
                                                   RSocketServiceInstanceData rSocketServiceInstanceData) {
        Map<String, LoadbalanceTarget> newLoadBalancedTargetData = uriList.stream()
                .map(uri -> {
                    Optional<ClientTransport> optionalClientTransport = getClientTransport(uri);
                    if (optionalClientTransport.isEmpty()) {
                        log.warn("Un supported rsocket transport uri: {}", uri);
                        return null;
                    }
                    return Tuples.of(uri, optionalClientTransport.get());
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(
                        tuple2 -> tuple2.getT1().toString(),
                        tuple2 -> LoadbalanceTarget.from(tuple2.getT1().toString(), tuple2.getT2()),
                        (o1, o2) -> o2,
                        LinkedHashMap::new
                ));
        boolean allMatch = existsLoadBalanceTargetList.stream()
                .allMatch(loadbalanceTarget -> newLoadBalancedTargetData.containsKey(loadbalanceTarget.getKey()));
        if (newLoadBalancedTargetData.isEmpty()) {
            if (!CollectionUtils.isEmpty(existsLoadBalanceTargetList)) {
                LoadbalanceTarget firstTarget= existsLoadBalanceTargetList.get(0);
                if(RSocketInstanceNotFoundTransport.class.equals(firstTarget.getTransport().getClass())){
                    return Mono.just(false);
                }
                log.info("Clear load-balanced target for {} since there was no rsocket instance be found",
                        transportURI
                );
            }
            return rSocketServiceInstanceData
                    .tryEmitNext(Collections.singletonList(LoadbalanceTarget.from(transportURI.toString(),
                                    new RSocketInstanceNotFoundTransport(transportURI)
                            )
                    ));
        }
        if (allMatch && newLoadBalancedTargetData.size() == existsLoadBalanceTargetList.size()) {
            log.trace("Refresh load-balanced target for {}, rsocket target list didn't change: {}",
                    transportURI,
                    newLoadBalancedTargetData.size()
            );
            return Mono.just(false);
        }
        log.info("Load load-balanced target for {}, new rsocket service instance size: {}",
                transportURI,
                newLoadBalancedTargetData.size()
        );
        return rSocketServiceInstanceData
                .tryEmitNext(newLoadBalancedTargetData.values().stream().toList());
    }

    @Slf4j
    static class RSocketInstanceNotFoundTransport implements ClientTransport {

        private final URI transportURI;

        RSocketInstanceNotFoundTransport(URI transportURI) {
            this.transportURI = transportURI;
            log.warn("No available rsocket service was found for {}", transportURI);
        }

        @Override
        public Mono<DuplexConnection> connect() {
            return Mono.error(new RSocketInstanceNotFoundException(transportURI));
        }
    }

    static class RSocketServiceInstanceData {

        static final String CURRENT_REFRESHER_RUNNING_CONTEXT_KEY = RSocketServiceInstanceData.class + ".current-refresher-running-flag";
        @Getter
        private final AtomicBoolean refreshRunning = new AtomicBoolean(false);
        @Getter
        private final URI transportURI;
        private final Sinks.Many<List<LoadbalanceTarget>> sinks;

        public RSocketServiceInstanceData(URI transportURI) {
            this.transportURI = transportURI;
            this.sinks = Sinks.many().replay().latest();
            this.sinks.tryEmitNext(Collections.emptyList());
        }

        public Mono<Boolean> tryEmitNext(@NonNull List<LoadbalanceTarget> loadbalanceTargetList) {
            return Mono.just(this.sinks.tryEmitNext(loadbalanceTargetList))
                    .map(Sinks.EmitResult::isSuccess);
        }

        public void emitComplete() {
            this.sinks.emitComplete((s, e) -> FAIL_NON_SERIALIZED.equals(e));
        }

        public Flux<List<LoadbalanceTarget>> getInstances() {
            return this.sinks.asFlux();
        }

    }

}
