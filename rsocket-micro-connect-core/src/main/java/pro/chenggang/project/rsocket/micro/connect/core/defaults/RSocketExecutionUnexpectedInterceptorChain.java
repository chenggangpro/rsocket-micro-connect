/*
 *    Copyright 2025 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package pro.chenggang.project.rsocket.micro.connect.core.defaults;

import lombok.extern.slf4j.Slf4j;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExchange;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionUnexpectedInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketInterceptorChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.ListIterator;
import java.util.Objects;

/**
 * The RSocket execution unexpected interceptor chain.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Slf4j
public class RSocketExecutionUnexpectedInterceptorChain implements RSocketInterceptorChain {

    private final RSocketExecutionUnexpectedInterceptor currentInterceptor;
    private final RSocketExecutionUnexpectedInterceptorChain next;

    public RSocketExecutionUnexpectedInterceptorChain(List<RSocketExecutionUnexpectedInterceptor> interceptors) {
        RSocketExecutionUnexpectedInterceptorChain interceptor = init(interceptors);
        this.currentInterceptor = interceptor.currentInterceptor;
        this.next = interceptor.next;
    }

    private RSocketExecutionUnexpectedInterceptorChain init(List<RSocketExecutionUnexpectedInterceptor> interceptors) {
        RSocketExecutionUnexpectedInterceptorChain interceptor = new RSocketExecutionUnexpectedInterceptorChain(null, null);
        if (Objects.nonNull(interceptors) && !interceptors.isEmpty()) {
            ListIterator<? extends RSocketExecutionUnexpectedInterceptor> iterator = interceptors.listIterator(interceptors.size());
            while (iterator.hasPrevious()) {
                interceptor = new RSocketExecutionUnexpectedInterceptorChain(iterator.previous(), interceptor);
            }
        } else {
            log.debug("RSocket execution unexpected interceptors is empty");
        }
        return interceptor;
    }

    private RSocketExecutionUnexpectedInterceptorChain(RSocketExecutionUnexpectedInterceptor currentInterceptor,
                                                       RSocketExecutionUnexpectedInterceptorChain next) {
        this.currentInterceptor = currentInterceptor;
        this.next = next;
    }

    @Override
    public Mono<Void> next(RSocketExchange exchange) {
        return Mono.defer(() -> {
            if (shouldIntercept()) {
                return this.currentInterceptor.interceptUnexpected(exchange, this.next);
            }
            return Mono.empty();
        });
    }

    private boolean shouldIntercept() {
        return this.currentInterceptor != null && this.next != null;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[currentInterceptor=" + this.currentInterceptor + "]";
    }
}
