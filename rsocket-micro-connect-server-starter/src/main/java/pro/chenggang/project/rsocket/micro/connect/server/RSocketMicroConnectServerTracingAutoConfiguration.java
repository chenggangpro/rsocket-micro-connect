package pro.chenggang.project.rsocket.micro.connect.server;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.tracing.Tracer;
import io.micrometer.tracing.propagation.Propagator;
import io.netty.buffer.PooledByteBufAllocator;
import io.rsocket.RSocket;
import io.rsocket.micrometer.observation.ByteBufGetter;
import io.rsocket.micrometer.observation.ByteBufSetter;
import io.rsocket.micrometer.observation.ObservationRequesterRSocketProxy;
import io.rsocket.micrometer.observation.ObservationResponderRSocketProxy;
import io.rsocket.micrometer.observation.RSocketRequesterTracingObservationHandler;
import io.rsocket.micrometer.observation.RSocketResponderTracingObservationHandler;
import io.rsocket.plugins.RSocketInterceptor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.autoconfigure.tracing.TracingProperties;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.rsocket.RSocketMessagingAutoConfiguration;
import org.springframework.boot.autoconfigure.rsocket.RSocketStrategiesAutoConfiguration;
import org.springframework.boot.rsocket.server.RSocketServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.messaging.rsocket.RSocketStrategies;

import static org.springframework.boot.actuate.autoconfigure.tracing.MicrometerTracingAutoConfiguration.DEFAULT_TRACING_OBSERVATION_HANDLER_ORDER;
import static org.springframework.boot.actuate.autoconfigure.tracing.TracingProperties.Propagation.PropagationType.B3;

@Slf4j
@AutoConfiguration(before = {RSocketStrategiesAutoConfiguration.class, RSocketMessagingAutoConfiguration.class})
@ConditionalOnClass({RSocket.class, RSocketStrategies.class, PooledByteBufAllocator.class, Tracer.class})
@ConditionalOnBean({Tracer.class, Propagator.class, TracingProperties.class, ObservationRegistry.class})
public class RSocketMicroConnectServerTracingAutoConfiguration {

    @Bean
    @Order(DEFAULT_TRACING_OBSERVATION_HANDLER_ORDER - 1)
    public RSocketResponderTracingObservationHandler rSocketResponderTracingObservationHandler(Tracer tracer,
                                                                                               Propagator propagator,
                                                                                               TracingProperties tracingProperties) {
        return new RSocketResponderTracingObservationHandler(tracer,
                propagator,
                new ByteBufGetter(),
                tracingProperties.getPropagation().getConsume().contains(B3)
        );
    }

    @Bean
    @Order(DEFAULT_TRACING_OBSERVATION_HANDLER_ORDER - 1)
    public RSocketRequesterTracingObservationHandler rSocketRequesterTracingObservationHandler(Tracer tracer,
                                                                                               Propagator propagator,
                                                                                               TracingProperties tracingProperties) {
        return new RSocketRequesterTracingObservationHandler(tracer,
                propagator,
                new ByteBufSetter(),
                tracingProperties.getPropagation().getProduce().contains(B3)
        );
    }

    @Bean
    public RSocketServerCustomizer rSocketServerCustomizer(ObservationRegistry observationRegistry) {
        return rSocketServer -> rSocketServer
                .interceptors(interceptorRegistry -> interceptorRegistry
                        .forResponder((RSocketInterceptor) rSocket -> new ObservationResponderRSocketProxy(
                                rSocket,
                                observationRegistry
                        ))
                        .forRequester((RSocketInterceptor) rSocket -> new ObservationRequesterRSocketProxy(rSocket,
                                observationRegistry
                        ))
                );
    }
}