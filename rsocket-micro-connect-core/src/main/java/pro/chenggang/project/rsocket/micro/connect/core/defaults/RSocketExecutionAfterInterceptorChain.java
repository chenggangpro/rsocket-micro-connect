package pro.chenggang.project.rsocket.micro.connect.core.defaults;

import lombok.extern.slf4j.Slf4j;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExchange;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionAfterInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketInterceptorChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

/**
 * The RSocket execution after interceptor chain.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Slf4j
public class RSocketExecutionAfterInterceptorChain implements RSocketInterceptorChain {

    private final boolean isEmpty;
    private final RSocketExecutionAfterInterceptor currentInterceptor;
    private final RSocketExecutionAfterInterceptorChain next;

    public RSocketExecutionAfterInterceptorChain(List<RSocketExecutionAfterInterceptor> interceptors) {
        if (Objects.isNull(interceptors) || interceptors.isEmpty()) {
            log.debug("RSocket execution after interceptors is empty");
            this.isEmpty = true;
            this.currentInterceptor = null;
            this.next = null;
            return;
        }
        this.isEmpty = false;
        RSocketExecutionAfterInterceptorChain interceptor = init(interceptors);
        this.currentInterceptor = interceptor.currentInterceptor;
        this.next = interceptor.next;
    }

    private RSocketExecutionAfterInterceptorChain init(List<RSocketExecutionAfterInterceptor> interceptors) {
        RSocketExecutionAfterInterceptorChain interceptor = new RSocketExecutionAfterInterceptorChain(null, null);
        ListIterator<? extends RSocketExecutionAfterInterceptor> iterator = interceptors.listIterator(0);
        while (iterator.hasNext()) {
            interceptor = new RSocketExecutionAfterInterceptorChain(iterator.next(), interceptor);
        }
        return interceptor;
    }

    private RSocketExecutionAfterInterceptorChain(RSocketExecutionAfterInterceptor currentInterceptor,
                                                  RSocketExecutionAfterInterceptorChain next) {
        this.isEmpty = false;
        this.currentInterceptor = currentInterceptor;
        this.next = next;
    }

    @Override
    public boolean isEmpty() {
        return this.isEmpty;
    }

    @Override
    public Mono<Void> next(RSocketExchange exchange) {
        if (this.isEmpty) {
            return Mono.empty();
        }
        return Mono.defer(() -> {
            if (shouldIntercept()) {
                return this.currentInterceptor.interceptAfter(exchange, this.next);
            }
            return Mono.empty();
        });
    }


    private boolean shouldIntercept() {
        return this.currentInterceptor != null && this.next != null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[isEmpty=" + this.isEmpty + ",currentInterceptor=" + this.currentInterceptor + "]";
    }
}
