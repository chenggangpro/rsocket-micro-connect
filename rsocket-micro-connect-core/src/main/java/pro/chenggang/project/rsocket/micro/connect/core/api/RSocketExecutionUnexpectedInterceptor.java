package pro.chenggang.project.rsocket.micro.connect.core.api;

import reactor.core.publisher.Mono;

/**
 * The RSocket execution unexpected interceptor.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public interface RSocketExecutionUnexpectedInterceptor extends RSocketExecutionInterceptor {

    /**
     * Intercept the execution of an RSocket <i>unexpected</i> its error or cancel invocation.
     * This execution will invoke in resources cleanup
     * This execution will invoke in natural order.
     *
     * @param exchange the current rsocket exchange
     * @param chain    the chain
     * @return the {@code Mono<Void>} to indicate when exchange processing is complete
     */
    Mono<Void> interceptUnexpected(RSocketExchange exchange, RSocketInterceptorChain chain);

}
