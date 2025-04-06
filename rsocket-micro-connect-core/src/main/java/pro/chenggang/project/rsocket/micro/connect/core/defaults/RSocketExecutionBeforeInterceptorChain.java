package pro.chenggang.project.rsocket.micro.connect.core.defaults;

import lombok.extern.slf4j.Slf4j;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExchange;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionBeforeInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketInterceptorChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

/**
 * The RSocket execution before interceptor chain.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Slf4j
public class RSocketExecutionBeforeInterceptorChain implements RSocketInterceptorChain {

    private final boolean isEmpty;
    private final RSocketExecutionBeforeInterceptor currentInterceptor;
    private final RSocketExecutionBeforeInterceptorChain next;

    public RSocketExecutionBeforeInterceptorChain(List<RSocketExecutionBeforeInterceptor> interceptors) {
        if (Objects.isNull(interceptors) || interceptors.isEmpty()) {
            log.debug("RSocket execution before interceptors is empty");
            this.isEmpty = true;
            this.currentInterceptor = null;
            this.next = null;
            return;
        }
        this.isEmpty = false;
        RSocketExecutionBeforeInterceptorChain interceptor = init(interceptors);
        this.currentInterceptor = interceptor.currentInterceptor;
        this.next = interceptor.next;
    }

    private RSocketExecutionBeforeInterceptorChain init(List<RSocketExecutionBeforeInterceptor> interceptors) {
        RSocketExecutionBeforeInterceptorChain interceptor = new RSocketExecutionBeforeInterceptorChain(null, null);
        ListIterator<? extends RSocketExecutionBeforeInterceptor> iterator = interceptors.listIterator(interceptors.size());
        while (iterator.hasPrevious()) {
            interceptor = new RSocketExecutionBeforeInterceptorChain(iterator.previous(), interceptor);
        }
        return interceptor;
    }

    private RSocketExecutionBeforeInterceptorChain(RSocketExecutionBeforeInterceptor currentInterceptor,
                                                   RSocketExecutionBeforeInterceptorChain next) {
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
                return this.currentInterceptor.interceptBefore(exchange, this.next);
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
