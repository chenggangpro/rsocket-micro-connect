package pro.chenggang.project.rsocket.micro.connect.core.client;

import lombok.extern.slf4j.Slf4j;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExchange;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionAfterInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionBeforeInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketInterceptorChain;
import reactor.core.publisher.Mono;

/**
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Slf4j
public class SimpleClientSideInterceptor implements RSocketExecutionBeforeInterceptor, RSocketExecutionAfterInterceptor {

    @Override
    public Mono<Void> interceptBefore(RSocketExchange exchange, RSocketInterceptorChain chain) {
        log.info("[Simple-Client-Side](Before):{}",exchange);
        return chain.next(exchange);
    }

    @Override
    public Mono<Void> interceptAfter(RSocketExchange exchange, RSocketInterceptorChain chain) {
        log.info("[Simple-Client-Side](After):{}",exchange);
        return chain.next(exchange);
    }
}
