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
package pro.chenggang.project.rsocket.micro.connect.server;

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
import org.springframework.boot.autoconfigure.rsocket.RSocketMessageHandlerCustomizer;
import org.springframework.boot.autoconfigure.rsocket.RSocketMessagingAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketStrategiesAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.cbor.Jackson2CborDecoder;
import org.springframework.http.codec.cbor.Jackson2CborEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.server.PathContainer.Options;
import org.springframework.messaging.handler.invocation.reactive.ArgumentResolverConfigurer;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.validation.Validator;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionAfterInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionBeforeInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionInterceptor.InterceptorType;
import pro.chenggang.project.rsocket.micro.connect.core.interceptor.SetupSocketAcceptorInterceptor;
import pro.chenggang.project.rsocket.micro.connect.spring.common.AttributeLifecycleRSocketInterceptor;
import pro.chenggang.project.rsocket.micro.connect.spring.server.EnhancedRSocketMessageHandler;
import pro.chenggang.project.rsocket.micro.connect.spring.server.RSocketMicroConnectServerProperties;
import pro.chenggang.project.rsocket.micro.connect.spring.server.ServerLoggingRSocketInterceptor;
import pro.chenggang.project.rsocket.micro.connect.spring.server.argument.ConnectorHeaderHandlerMethodArgumentResolver;
import pro.chenggang.project.rsocket.micro.connect.spring.server.argument.ConnectorHeaderMapHandlerMethodArgumentResolver;
import pro.chenggang.project.rsocket.micro.connect.spring.server.argument.HttpQueryHandlerMethodArgumentResolver;
import pro.chenggang.project.rsocket.micro.connect.spring.server.argument.HttpQueryMapHandlerMethodArgumentResolver;
import pro.chenggang.project.rsocket.micro.connect.spring.server.argument.PathVariableMethodArgumentResolver;
import pro.chenggang.project.rsocket.micro.connect.spring.server.argument.RequestBodyMethodArgumentResolver;
import pro.chenggang.project.rsocket.micro.connect.spring.server.argument.RequestPartNameMethodArgumentResolver;
import pro.chenggang.project.rsocket.micro.connect.spring.server.argument.RequestPartPayloadMethodArgumentResolver;

import java.util.Comparator;

import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.CONNECTOR_FILE_PART_NAME_MEDIA_TYPE;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.CONNECTOR_FILE_PART_NAME_METADATA_KEY;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.CONNECTOR_HEADER_MEDIA_TYPE;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.CONNECTOR_HEADER_METADATA_KEY;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.CONNECTOR_QUERY_MEDIA_TYPE;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.CONNECTOR_QUERY_METADATA_KEY;
import static pro.chenggang.project.rsocket.micro.connect.spring.server.RSocketMicroConnectServerProperties.PROPERTIES_PREFIX;

/**
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration(before = {RSocketStrategiesAutoConfiguration.class, RSocketMessagingAutoConfiguration.class})
@ConditionalOnClass({RSocket.class, RSocketStrategies.class, PooledByteBufAllocator.class})
@ConditionalOnProperty(prefix = PROPERTIES_PREFIX, value = "enabled", havingValue = "true", matchIfMissing = true)
public class RSocketMicroConnectServerAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = PROPERTIES_PREFIX)
    public RSocketMicroConnectServerProperties rSocketMicroConnectServerProperties() {
        return new RSocketMicroConnectServerProperties();
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
    @ConditionalOnBean(Validator.class)
    public RSocketMessageHandlerCustomizer rSocketMessageHandlerCustomizer(Validator validator) {
        return messageHandler -> messageHandler.setValidator(validator);
    }

    @Bean
    public RSocketMessageHandlerCustomizer httpHeaderHandlerMethodArgumentResolverCustomizer() {
        return messageHandler -> {
            ArgumentResolverConfigurer argumentResolverConfigurer = messageHandler.getArgumentResolverConfigurer();
            argumentResolverConfigurer.addCustomResolver(new ConnectorHeaderHandlerMethodArgumentResolver(messageHandler.getConversionService()));
            argumentResolverConfigurer.addCustomResolver(new ConnectorHeaderMapHandlerMethodArgumentResolver());
            argumentResolverConfigurer.addCustomResolver(new PathVariableMethodArgumentResolver(messageHandler.getConversionService()));
            argumentResolverConfigurer.addCustomResolver(new HttpQueryHandlerMethodArgumentResolver(messageHandler.getConversionService()));
            argumentResolverConfigurer.addCustomResolver(new HttpQueryMapHandlerMethodArgumentResolver());
            argumentResolverConfigurer.addCustomResolver(new RequestPartNameMethodArgumentResolver());
            argumentResolverConfigurer.addCustomResolver(new RequestPartPayloadMethodArgumentResolver(messageHandler.getDecoders(),
                    messageHandler.getValidator(),
                    messageHandler.getReactiveAdapterRegistry()
            ));
            argumentResolverConfigurer.addCustomResolver(new RequestBodyMethodArgumentResolver(messageHandler.getDecoders(),
                    messageHandler.getValidator(),
                    messageHandler.getReactiveAdapterRegistry()
            ));
        };
    }

    @Bean
    public RSocketMessageHandler rsocketMessageHandler(RSocketStrategies rSocketStrategies,
                                                       ObjectProvider<RSocketMessageHandlerCustomizer> customizers) {
        EnhancedRSocketMessageHandler handler = new EnhancedRSocketMessageHandler();
        handler.setRouteMatcher(rSocketStrategies.routeMatcher());
        handler.setRSocketStrategies(rSocketStrategies);
        customizers.orderedStream().forEach(customizer -> customizer.customize(handler));
        return handler;
    }

    @Bean
    public AttributeLifecycleRSocketInterceptor attributeLifecycleRSocketInterceptor(RSocketStrategies rSocketStrategies) {
        return new AttributeLifecycleRSocketInterceptor(rSocketStrategies);
    }

    @Bean
    public ServerLoggingRSocketInterceptor serverSideLoggingRSocketExecutionInterceptor(RSocketMicroConnectServerProperties rSocketMicroConnectServerProperties) {
        return new ServerLoggingRSocketInterceptor(rSocketMicroConnectServerProperties);
    }

    @Bean
    @ConditionalOnMissingBean(SetupSocketAcceptorInterceptor.class)
    public SetupSocketAcceptorInterceptor setupSocketAcceptorInterceptor(RSocketMicroConnectServerProperties rSocketMicroConnectServerProperties,
                                                                         ObjectProvider<RSocketExecutionBeforeInterceptor> beforeInterceptors,
                                                                         ObjectProvider<RSocketExecutionAfterInterceptor> afterInterceptors) {
        return new SetupSocketAcceptorInterceptor(rSocketMicroConnectServerProperties.getDefaultDataMimeType(),
                rSocketMicroConnectServerProperties.getDefaultMetadataMimeType(),
                beforeInterceptors.stream()
                        .sorted(Comparator.comparing(RSocketExecutionInterceptor::order))
                        .filter(InterceptorType::isServerSide)
                        .toList(),
                afterInterceptors.stream()
                        .sorted(Comparator.comparing(RSocketExecutionInterceptor::order))
                        .filter(InterceptorType::isServerSide)
                        .toList()
        );
    }

    @Bean
    public RSocketServerCustomizer rSocketMicroConnectServerCustomizer(SetupSocketAcceptorInterceptor setupSocketAcceptorInterceptor) {
        return rSocketServer -> {
            rSocketServer.interceptors(interceptorRegistry -> {
                interceptorRegistry.forSocketAcceptor(setupSocketAcceptorInterceptor);
            });
        };
    }
}
