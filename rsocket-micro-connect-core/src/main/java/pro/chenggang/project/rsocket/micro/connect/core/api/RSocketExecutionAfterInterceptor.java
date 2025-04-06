package pro.chenggang.project.rsocket.micro.connect.core.api;

import reactor.core.publisher.Mono;

/**
 * The RSocket execution after interceptor.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public interface RSocketExecutionAfterInterceptor {

    /**
     * Intercept the execution of an RSocket <i>after</i> its successful invocation.
     * This execution will invoke in <b>REVERSE</> order.
     * When using request-stream or request-channel of RSocket, this will invoke in each item of response Flux
     *
     * @param exchange the current rsocket exchange
     * @param chain    the chain
     * @return the {@code Mono<Void>} to indicate when exchange processing is complete
     */
    Mono<Void> interceptAfter(RSocketExchange exchange, RSocketInterceptorChain chain);

}
