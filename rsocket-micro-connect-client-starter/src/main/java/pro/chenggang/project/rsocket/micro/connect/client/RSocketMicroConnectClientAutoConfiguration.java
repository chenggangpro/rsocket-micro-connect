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
import org.springframework.boot.autoconfigure.rsocket.RSocketMessagingAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketStrategiesAutoConfiguration;
import org.springframework.boot.rsocket.messaging.RSocketStrategiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.cbor.Jackson2CborDecoder;
import org.springframework.http.codec.cbor.Jackson2CborEncoder;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.server.PathContainer.Options;
import org.springframework.messaging.rsocket.RSocketStrategies;
import org.springframework.web.util.pattern.PathPatternParser;
import org.springframework.web.util.pattern.PathPatternRouteMatcher;

import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.HTTP_HEADER_MEDIA_TYPE;

/**
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Slf4j
@AutoConfiguration(before = {RSocketStrategiesAutoConfiguration.class, RSocketMessagingAutoConfiguration.class})
@ConditionalOnClass({RSocket.class, RSocketStrategies.class, PooledByteBufAllocator.class})
public class RSocketMicroConnectClientAutoConfiguration {

    @Bean
    @ConditionalOnClass(PathPatternRouteMatcher.class)
    @ConditionalOnMissingBean(PathPatternRouteMatcher.class)
    public PathPatternRouteMatcher pathPatternRouteMatcher() {
        PathPatternParser pathPatternParser = new PathPatternParser();
        pathPatternParser.setPathOptions(Options.HTTP_PATH);
        return new PathPatternRouteMatcher(pathPatternParser);
    }

    @Bean
    @ConditionalOnBean(Jackson2ObjectMapperBuilder.class)
    public RSocketStrategiesCustomizer jacksonCborHttpHeaderRSocketStrategyCustomizer(Jackson2ObjectMapperBuilder builder) {
        return strategies -> {
            ObjectMapper objectMapper = builder.createXmlMapper(false).factory(new CBORFactory()).build();
            strategies.decoder(new Jackson2CborDecoder(objectMapper, HTTP_HEADER_MEDIA_TYPE));
            strategies.encoder(new Jackson2CborEncoder(objectMapper, HTTP_HEADER_MEDIA_TYPE));
            strategies.metadataExtractorRegistry(metadataExtractorRegistry -> {
                metadataExtractorRegistry.metadataToExtract(HTTP_HEADER_MEDIA_TYPE,
                        HttpHeaders.class,
                        HttpHeaders.class.getName()
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
}
