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
import org.springframework.boot.autoconfigure.rsocket.RSocketMessageHandlerCustomizer;
import org.springframework.boot.autoconfigure.rsocket.RSocketMessagingAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketStrategiesAutoConfiguration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.cbor.Jackson2CborDecoder;
import org.springframework.http.codec.cbor.Jackson2CborEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.server.PathContainer.Options;
import org.springframework.messaging.handler.invocation.reactive.ArgumentResolverConfigurer;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.validation.Validator;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionAfterInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionBeforeInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionInterceptor.InterceptorType;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionUnexpectedInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.interceptor.SetupSocketAcceptorInterceptor;
import pro.chenggang.project.rsocket.micro.connect.spring.server.EnhancedRSocketMessageHandler;
import pro.chenggang.project.rsocket.micro.connect.spring.server.RSocketMicroConnectServerProperties;
import pro.chenggang.project.rsocket.micro.connect.spring.server.ServerLoggingRSocketInterceptor;
import pro.chenggang.project.rsocket.micro.connect.spring.server.argument.HttpHeaderHandlerMethodArgumentResolver;
import pro.chenggang.project.rsocket.micro.connect.spring.server.argument.HttpQueryHandlerMethodArgumentResolver;
import pro.chenggang.project.rsocket.micro.connect.spring.server.argument.HttpQueryMapHandlerMethodArgumentResolver;
import pro.chenggang.project.rsocket.micro.connect.spring.server.argument.PathVariableMethodArgumentResolver;
import pro.chenggang.project.rsocket.micro.connect.spring.server.argument.RequestBodyMethodArgumentResolver;

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
@AutoConfiguration(before = {RSocketStrategiesAutoConfiguration.class, RSocketMessagingAutoConfiguration.class})
@ConditionalOnClass({RSocket.class, RSocketStrategies.class, PooledByteBufAllocator.class})
public class RSocketMicroConnectServerAutoConfiguration {

    @Bean
    @ConfigurationProperties(prefix = RSocketMicroConnectServerProperties.PREFIX)
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
    @ConditionalOnBean(Validator.class)
    public RSocketMessageHandlerCustomizer rSocketMessageHandlerCustomizer(Validator validator) {
        return messageHandler -> messageHandler.setValidator(validator);
    }

    @Bean
    public RSocketMessageHandlerCustomizer httpHeaderHandlerMethodArgumentResolverCustomizer() {
        return messageHandler -> {
            ArgumentResolverConfigurer argumentResolverConfigurer = messageHandler.getArgumentResolverConfigurer();
            argumentResolverConfigurer.addCustomResolver(new HttpHeaderHandlerMethodArgumentResolver());
            argumentResolverConfigurer.addCustomResolver(new PathVariableMethodArgumentResolver(messageHandler.getConversionService()));
            argumentResolverConfigurer.addCustomResolver(new HttpQueryHandlerMethodArgumentResolver(messageHandler.getConversionService()));
            argumentResolverConfigurer.addCustomResolver(new HttpQueryMapHandlerMethodArgumentResolver());
            argumentResolverConfigurer.addCustomResolver(new RequestBodyMethodArgumentResolver(messageHandler.getDecoders(),
                            messageHandler.getValidator(),
                            messageHandler.getReactiveAdapterRegistry()
                    )
            );
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
    public ServerLoggingRSocketInterceptor serverSideLoggingRSocketExecutionInterceptor(RSocketStrategies rSocketStrategies) {
        return new ServerLoggingRSocketInterceptor(rSocketStrategies);
    }

    @Bean
    @ConditionalOnMissingBean(SetupSocketAcceptorInterceptor.class)
    public SetupSocketAcceptorInterceptor setupSocketAcceptorInterceptor(RSocketMicroConnectServerProperties rSocketMicroConnectServerProperties,
                                                                         ObjectProvider<RSocketExecutionBeforeInterceptor> beforeInterceptors,
                                                                         ObjectProvider<RSocketExecutionAfterInterceptor> afterInterceptors,
                                                                         ObjectProvider<RSocketExecutionUnexpectedInterceptor> unexpectedInterceptors) {
        return new SetupSocketAcceptorInterceptor(rSocketMicroConnectServerProperties.getDefaultDataMimeType(),
                rSocketMicroConnectServerProperties.getDefaultMetadataMimeType(),
                beforeInterceptors.orderedStream().filter(InterceptorType::isServerSide).toList(),
                afterInterceptors.orderedStream().filter(InterceptorType::isServerSide).toList(),
                unexpectedInterceptors.orderedStream().filter(InterceptorType::isServerSide).toList()
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
