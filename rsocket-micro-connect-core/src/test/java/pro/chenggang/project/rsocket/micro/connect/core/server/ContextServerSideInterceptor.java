package pro.chenggang.project.rsocket.micro.connect.core.server;

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
public class ContextServerSideInterceptor implements RSocketExecutionBeforeInterceptor, RSocketExecutionAfterInterceptor {

    @Override
    public Mono<Void> interceptBefore(RSocketExchange exchange, RSocketInterceptorChain chain) {
        return Mono.deferContextual(contextView -> {
            log.info("[Context-Server-Side](Before):{},Context:{}", exchange, contextView);
            return chain.next(exchange);
        });
    }

    @Override
    public Mono<Void> interceptAfter(RSocketExchange exchange, RSocketInterceptorChain chain) {
        return Mono.deferContextual(contextView -> {
            log.info("[Context-Server-Side](After):{},Context:{}", exchange, contextView);
            return chain.next(exchange);
        });
    }
}
