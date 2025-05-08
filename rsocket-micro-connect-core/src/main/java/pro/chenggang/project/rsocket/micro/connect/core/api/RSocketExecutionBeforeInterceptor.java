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
     * <p/>
     * <li>This execution will invoke in natural order.</li>
     * <li><b>This execution only execute ONCE with first payload when using REQUEST-CHANNEL of RSocket such as in file-uploading scenario.</b></li>
     *
     * @param exchange the current rsocket exchange
     * @param chain    the chain
     * @return the {@code Mono<Void>} to indicate when exchange processing is complete
     */
    Mono<Void> interceptBefore(RSocketExchange exchange, RSocketInterceptorChain chain);

}
