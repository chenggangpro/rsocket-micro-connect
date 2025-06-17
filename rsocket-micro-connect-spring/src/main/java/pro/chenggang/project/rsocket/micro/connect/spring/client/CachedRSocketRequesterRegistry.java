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
package pro.chenggang.project.rsocket.micro.connect.spring.client;

import io.rsocket.transport.ClientTransport;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.messaging.rsocket.RSocketRequester;

import java.net.URI;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The default rsocket requester registry.
 *
 * @author Gang Cheng
 * @version 0.2.0
 * @since 0.1.0
 */
@Slf4j
@RequiredArgsConstructor
public class CachedRSocketRequesterRegistry implements RSocketRequesterRegistry, DisposableBean {

    protected final RSocketRequester.Builder builder;
    protected final Map<URI, RSocketRequester> rSocketRequesterCache = new ConcurrentHashMap<>();

    @Override
    public RSocketRequester getRSocketRequester(@NonNull URI transportURI) {
        return rSocketRequesterCache.compute(transportURI, this::initialize);
    }

    /**
     * Initialize RSocketRequester from uri
     *
     * @param transportURI     the transport uri
     * @param rsocketRequester the rsocket requester , null value means didn't be initialized
     * @return cached or new rsocket requester
     */
    private RSocketRequester initialize(@NonNull URI transportURI, RSocketRequester rsocketRequester) {
        if (Objects.isNull(rsocketRequester)) {
            log.info("RSocketRequester cache doesn't exist, creating a new one for {}", transportURI);
            return this.newRSocketRequester(transportURI);
        }
        if (rsocketRequester.isDisposed()) {
            log.info("Cached rSocketRequester is disposed, creating a new one for {}", transportURI);
            return this.newRSocketRequester(transportURI);
        }
        return rsocketRequester;
    }

    /**
     * New rsocket requester.
     *
     * @param transportURI the transport uri
     * @return the rsocket requester
     */
    protected RSocketRequester newRSocketRequester(@NonNull URI transportURI) {
        ClientTransport clientTransport = getClientTransport(transportURI)
                .orElseThrow(() -> new IllegalArgumentException("Un supported rsocket transport uri: " + transportURI));
        return builder.transport(clientTransport);
    }

    @Override
    public void destroy() throws Exception {
        this.rSocketRequesterCache.forEach((uri, rSocketRequester) -> {
            if (rSocketRequester.isDisposed()) {
                return;
            }
            rSocketRequester.dispose();
            log.info("Dispose rsocket requester for uri: {}", uri);
        });
        this.rSocketRequesterCache.clear();
    }
}
