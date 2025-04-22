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
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.cbor.Jackson2CborDecoder;
import org.springframework.http.codec.cbor.Jackson2CborEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.server.PathContainer.Options;
import org.springframework.messaging.rsocket.RSocketConnectorConfigurer;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketRequester.Builder;
import org.springframework.messaging.rsocket.RSocketStrategies;
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
import pro.chenggang.project.rsocket.micro.connect.spring.client.loadbalance.DefaultRSocketLoadBalanceStrategies;
import pro.chenggang.project.rsocket.micro.connect.spring.client.loadbalance.DiscoverRSocketRequesterRegistry;
import pro.chenggang.project.rsocket.micro.connect.spring.client.loadbalance.RSocketLoadBalanceStrategies;
import pro.chenggang.project.rsocket.micro.connect.spring.proxy.DefaultRSocketMicroConnectorRegistry;
import pro.chenggang.project.rsocket.micro.connect.spring.proxy.RSocketMicroConnectorExecutionCustomizer;
import pro.chenggang.project.rsocket.micro.connect.spring.proxy.RSocketMicroConnectorRegistry;

import static org.springframework.beans.factory.config.ConfigurableBeanFactory.SCOPE_PROTOTYPE;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.CONNECTOR_FILE_PART_NAME_MEDIA_TYPE;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.CONNECTOR_FILE_PART_NAME_METADATA_KEY;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.CONNECTOR_HEADER_MEDIA_TYPE;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.CONNECTOR_HEADER_METADATA_KEY;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.CONNECTOR_QUERY_MEDIA_TYPE;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.CONNECTOR_QUERY_METADATA_KEY;

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
    public RSocketStrategiesCustomizer fileStreamRSocketStrategyCustomizer() {
        return strategies -> {
            strategies.metadataExtractorRegistry(metadataExtractorRegistry -> {
                metadataExtractorRegistry.metadataToExtract(CONNECTOR_FILE_PART_NAME_MEDIA_TYPE,
                        String.class,
                        CONNECTOR_FILE_PART_NAME_METADATA_KEY
                );
            });
        };
    }

    @Bean
    @ConditionalOnClass({ObjectMapper.class, CBORFactory.class})
    @ConditionalOnBean(Jackson2ObjectMapperBuilder.class)
    public RSocketStrategiesCustomizer jacksonCborHttpHeaderRSocketStrategyCustomizer(Jackson2ObjectMapperBuilder builder) {
        return strategies -> {
            ObjectMapper objectMapper = builder.createXmlMapper(false).factory(new CBORFactory()).build();
            strategies.decoder(new Jackson2CborDecoder(objectMapper, CONNECTOR_HEADER_MEDIA_TYPE));
            strategies.encoder(new Jackson2CborEncoder(objectMapper, CONNECTOR_HEADER_MEDIA_TYPE));
            strategies.metadataExtractorRegistry(metadataExtractorRegistry -> {
                metadataExtractorRegistry.metadataToExtract(CONNECTOR_HEADER_MEDIA_TYPE,
                        HttpHeaders.class,
                        CONNECTOR_HEADER_METADATA_KEY
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
            strategies.decoder(new Jackson2CborDecoder(objectMapper, CONNECTOR_QUERY_MEDIA_TYPE));
            strategies.encoder(new Jackson2CborEncoder(objectMapper, CONNECTOR_QUERY_MEDIA_TYPE));
            strategies.metadataExtractorRegistry(metadataExtractorRegistry -> {
                metadataExtractorRegistry.metadataToExtract(CONNECTOR_QUERY_MEDIA_TYPE,
                        HttpHeaders.class,
                        CONNECTOR_QUERY_METADATA_KEY
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
    @ConditionalOnMissingBean(RSocketLoadBalanceStrategies.class)
    @ConditionalOnProperty(prefix = RSocketMicroConnectClientProperties.PREFIX, value = "enable-discover", havingValue = "true")
    public RSocketLoadBalanceStrategies rSocketLoadBalanceStrategies() {
        return new DefaultRSocketLoadBalanceStrategies();
    }

    @Bean
    @ConditionalOnBean(ReactiveDiscoveryClient.class)
    @ConditionalOnMissingBean(RSocketRequesterRegistry.class)
    @ConditionalOnProperty(prefix = RSocketMicroConnectClientProperties.PREFIX, value = "enable-discover", havingValue = "true")
    public RSocketRequesterRegistry discoverRSocketRequesterRegistry(RSocketRequester.Builder rSocketRequesterBuilder,
                                                                     ReactiveDiscoveryClient reactiveDiscoveryClient,
                                                                     RSocketLoadBalanceStrategies rSocketLoadBalanceStrategies,
                                                                     RSocketMicroConnectClientProperties rSocketMicroConnectClientProperties) {
        return new DiscoverRSocketRequesterRegistry(rSocketRequesterBuilder,
                reactiveDiscoveryClient,
                rSocketLoadBalanceStrategies,
                rSocketMicroConnectClientProperties.getRefreshDiscoverInterval()
        );
    }

    @Bean
    @ConditionalOnMissingBean(RSocketMicroConnectorRegistry.class)
    public RSocketMicroConnectorRegistry rSocketMicroConnectorRegistry(RSocketRequesterRegistry rSocketRequesterRegistry,
                                                                       ObjectProvider<RSocketMicroConnectorExecutionCustomizer> connectorExecutionCustomizers) {
        return new DefaultRSocketMicroConnectorRegistry(rSocketRequesterRegistry,
                connectorExecutionCustomizers.orderedStream().toList()
        );
    }
}
