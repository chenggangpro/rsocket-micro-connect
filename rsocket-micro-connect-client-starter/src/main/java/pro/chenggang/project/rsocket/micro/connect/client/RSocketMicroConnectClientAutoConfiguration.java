package pro.chenggang.project.rsocket.micro.connect.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;
import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.RSocket;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.rsocket.RSocketMessagingAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketRequesterAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketStrategiesAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Scope;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.cbor.Jackson2CborDecoder;
import org.springframework.http.codec.cbor.Jackson2CborEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.server.PathContainer.Options;
import org.springframework.messaging.rsocket.RSocketConnectorConfigurer;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketRequester.Builder;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionAfterInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionBeforeInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionInterceptor.InterceptorType;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionUnexpectedInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.interceptor.ChainedRSocketInterceptor;
import pro.chenggang.project.rsocket.micro.connect.spring.client.CachedRSocketRequesterRegistry;
import pro.chenggang.project.rsocket.micro.connect.spring.client.ClientLoggingRSocketInterceptor;
import pro.chenggang.project.rsocket.micro.connect.spring.client.RSocketMicroConnectClientProperties;
import pro.chenggang.project.rsocket.micro.connect.spring.client.RSocketRequesterRegistry;
import pro.chenggang.project.rsocket.micro.connect.spring.client.loadbalance.DefaultRSocketLoadbalanceStrategies;
import pro.chenggang.project.rsocket.micro.connect.spring.client.loadbalance.DiscoverRSocketRequesterRegistry;
import pro.chenggang.project.rsocket.micro.connect.spring.client.loadbalance.RSocketLoadbalanceStrategies;
import pro.chenggang.project.rsocket.micro.connect.spring.proxy.DefaultRSocketMicroConnectorRegistry;
import pro.chenggang.project.rsocket.micro.connect.spring.proxy.RSocketMicroConnectorRegistry;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.HTTP_HEADER_MEDIA_TYPE;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.HTTP_HEADER_METADATA_KEY;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.HTTP_QUERY_MEDIA_TYPE;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.HTTP_QUERY_METADATA_KEY;

/**
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration(before = {RSocketStrategiesAutoConfiguration.class, RSocketMessagingAutoConfiguration.class, RSocketRequesterAutoConfiguration.class})
@ConditionalOnClass({RSocket.class, RSocketStrategies.class, PooledByteBufAllocator.class})
public class RSocketMicroConnectClientAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = RSocketMicroConnectClientProperties.PREFIX)
    public RSocketMicroConnectClientProperties rSocketMicroConnectClientProperties() {
        return new RSocketMicroConnectClientProperties();
    }

    @Bean
    @ConditionalOnClass(PathPatternRouteMatcher.class)
    @ConditionalOnMissingBean(PathPatternRouteMatcher.class)
    public PathPatternRouteMatcher pathPatternRouteMatcher() {
        PathPatternParser pathPatternParser = new PathPatternParser();
        pathPatternParser.setPathOptions(Options.HTTP_PATH);
        return new PathPatternRouteMatcher(pathPatternParser);
    }

    @Bean
    @ConditionalOnClass({ObjectMapper.class, CBORFactory.class})
    @ConditionalOnBean(Jackson2ObjectMapperBuilder.class)
    public RSocketStrategiesCustomizer jacksonCborHttpHeaderRSocketStrategyCustomizer(Jackson2ObjectMapperBuilder builder) {
        return strategies -> {
            ObjectMapper objectMapper = builder.createXmlMapper(false).factory(new CBORFactory()).build();
            strategies.decoder(new Jackson2CborDecoder(objectMapper, HTTP_HEADER_MEDIA_TYPE));
            strategies.encoder(new Jackson2CborEncoder(objectMapper, HTTP_HEADER_MEDIA_TYPE));
            strategies.metadataExtractorRegistry(metadataExtractorRegistry -> {
                metadataExtractorRegistry.metadataToExtract(HTTP_HEADER_MEDIA_TYPE,
                        HttpHeaders.class,
                        HTTP_HEADER_METADATA_KEY
                );
            });
        };
    }

    @Bean
    @ConditionalOnClass({ObjectMapper.class, CBORFactory.class})
    @ConditionalOnBean(Jackson2ObjectMapperBuilder.class)
    public RSocketStrategiesCustomizer jacksonCborHttpQueryRSocketStrategyCustomizer(Jackson2ObjectMapperBuilder builder) {
        return strategies -> {
            ObjectMapper objectMapper = builder.createXmlMapper(false).factory(new CBORFactory()).build();
            strategies.decoder(new Jackson2CborDecoder(objectMapper, HTTP_QUERY_MEDIA_TYPE));
            strategies.encoder(new Jackson2CborEncoder(objectMapper, HTTP_QUERY_MEDIA_TYPE));
            strategies.metadataExtractorRegistry(metadataExtractorRegistry -> {
                metadataExtractorRegistry.metadataToExtract(HTTP_QUERY_MEDIA_TYPE,
                        new ParameterizedTypeReference<LinkedMultiValueMap<String, String>>() {
                        },
                        HTTP_QUERY_METADATA_KEY
                );
            });
        };
    }

    @Bean
    @ConditionalOnBean(PathPatternRouteMatcher.class)
    public RSocketStrategiesCustomizer routeMatcherRSocketStrategyCustomizer(PathPatternRouteMatcher pathPatternRouteMatcher) {
        return strategies -> strategies.routeMatcher(pathPatternRouteMatcher);
    }

    @Bean
    public RSocketStrategies rSocketStrategies(ObjectProvider<RSocketStrategiesCustomizer> customizers) {
        RSocketStrategies.Builder builder = RSocketStrategies.builder();
        customizers.orderedStream().forEach((customizer) -> customizer.customize(builder));
        return builder.build();
    }

    @Bean
    public ClientLoggingRSocketInterceptor clientSideLoggingRSocketExecutionInterceptor(RSocketStrategies rSocketStrategies) {
        return new ClientLoggingRSocketInterceptor(rSocketStrategies);
    }

    @Bean
    public RSocketConnectorConfigurer clientLoggingRSocketConnectorConfigurer(RSocketMicroConnectClientProperties rSocketMicroConnectClientProperties,
                                                                              ObjectProvider<RSocketExecutionBeforeInterceptor> beforeInterceptors,
                                                                              ObjectProvider<RSocketExecutionAfterInterceptor> afterInterceptors,
                                                                              ObjectProvider<RSocketExecutionUnexpectedInterceptor> unexpectedInterceptors) {
        return connector -> connector
                .interceptors(interceptorRegistry -> interceptorRegistry
                        .forRequester(new ChainedRSocketInterceptor(rSocketMicroConnectClientProperties.getDefaultDataMimeType(),
                                rSocketMicroConnectClientProperties.getDefaultMetadataMimeType(),
                                beforeInterceptors.orderedStream().filter(InterceptorType::isClientSide).toList(),
                                afterInterceptors.orderedStream().filter(InterceptorType::isClientSide).toList(),
                                unexpectedInterceptors.orderedStream().filter(InterceptorType::isClientSide).toList()
                        ))
                );
    }

    @Bean
    @Scope(SCOPE_PROTOTYPE)
    public RSocketRequester.Builder rSocketRequesterBuilder(RSocketStrategies strategies,
                                                            ObjectProvider<RSocketConnectorConfigurer> connectorConfigurers) {
        Builder builder = RSocketRequester.builder().rsocketStrategies(strategies);
        connectorConfigurers.orderedStream().forEach(builder::rsocketConnector);
        return builder;
    }

    @Bean
    @ConditionalOnMissingBean(RSocketRequesterRegistry.class)
    @ConditionalOnProperty(prefix = RSocketMicroConnectClientProperties.PREFIX, value = "enable-discover", havingValue = "false", matchIfMissing = true)
    public RSocketRequesterRegistry cachedRSocketRequesterRegistry(RSocketRequester.Builder rSocketRequesterBuilder) {
        return new CachedRSocketRequesterRegistry(rSocketRequesterBuilder);
    }

    @Bean
    @ConditionalOnBean(ReactiveDiscoveryClient.class)
    @ConditionalOnMissingBean(RSocketLoadbalanceStrategies.class)
    @ConditionalOnProperty(prefix = RSocketMicroConnectClientProperties.PREFIX, value = "enable-discover", havingValue = "true")
    public RSocketLoadbalanceStrategies rSocketLoadbalanceStrategies() {
        return new DefaultRSocketLoadbalanceStrategies();
    }

    @Bean
    @ConditionalOnBean(ReactiveDiscoveryClient.class)
    @ConditionalOnMissingBean(RSocketRequesterRegistry.class)
    @ConditionalOnProperty(prefix = RSocketMicroConnectClientProperties.PREFIX, value = "enable-discover", havingValue = "true")
    public RSocketRequesterRegistry discoverRSocketRequesterRegistry(RSocketRequester.Builder rSocketRequesterBuilder,
                                                                     ReactiveDiscoveryClient reactiveDiscoveryClient,
                                                                     RSocketLoadbalanceStrategies rSocketLoadbalanceStrategies,
                                                                     RSocketMicroConnectClientProperties rSocketMicroConnectClientProperties) {
        return new DiscoverRSocketRequesterRegistry(rSocketRequesterBuilder,
                reactiveDiscoveryClient,
                rSocketLoadbalanceStrategies,
                rSocketMicroConnectClientProperties.getRefreshDiscoverInterval()
        );
    }

    @Bean
    @ConditionalOnMissingBean(RSocketMicroConnectorRegistry.class)
    public RSocketMicroConnectorRegistry rSocketMicroConnectorRegistry(RSocketRequesterRegistry rSocketRequesterRegistry) {
        return new DefaultRSocketMicroConnectorRegistry(rSocketRequesterRegistry);
    }
}
