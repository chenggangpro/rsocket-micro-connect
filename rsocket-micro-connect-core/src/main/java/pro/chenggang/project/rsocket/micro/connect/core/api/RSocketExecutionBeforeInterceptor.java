package pro.chenggang.project.rsocket.micro.connect.core.api;

import reactor.core.publisher.Mono;

/**
 * The RSocket execution before interceptor.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public interface RSocketExecutionBeforeInterceptor extends RSocketExecutionInterceptor {

    /**
     * Intercept the execution of an RSocket <i>before</i> its invocation.
     * This execution will invoke in natural order.
     *
     * @param exchange the current rsocket exchange
     * @param chain    the chain
     * @return the {@code Mono<Void>} to indicate when exchange processing is complete
     */
    Mono<Void> interceptBefore(RSocketExchange exchange, RSocketInterceptorChain chain);

}
