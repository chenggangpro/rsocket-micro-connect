package pro.chenggang.project.rsocket.micro.connect.core.api;

import reactor.core.publisher.Mono;

/**
 * The RSocket interceptor chain.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public interface RSocketInterceptorChain {

    /**
     * Is this chain empty.
     *
     * @return true if this chain is empty
     */
    boolean isEmpty();

    /**
     * Delegate to the next {@code RSocketExchangeInterceptor} in the chain.
     *
     * @param exchange the current rsocket exchange
     * @return the {@code Mono<Void>} to indicate when exchange processing is complete
     */
    Mono<Void> next(RSocketExchange exchange);

}