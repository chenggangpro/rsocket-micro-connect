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
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.client.WebsocketClientTransport;
import lombok.NonNull;
import org.springframework.messaging.rsocket.RSocketRequester;

import java.net.URI;
import java.util.Optional;

/**
 * The interface RSocket requester registry.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public interface RSocketRequesterRegistry {

    /**
     * Gets rsocket requester.
     *
     * @param transportURI the transport uri
     * @return the rsocket requester
     */
    RSocketRequester getRSocketRequester(@NonNull URI transportURI);

    /**
     * Gets client transport.
     *
     * @param transportURI the transport uri
     * @return the client transport
     */
    default Optional<ClientTransport> getClientTransport(@NonNull URI transportURI) {
        String scheme = transportURI.getScheme();
        if ("tcp".equalsIgnoreCase(scheme)) {
            if (transportURI.getPort() <= 0) {
                throw new IllegalArgumentException("RSocket transport port can not be negative or zero");
            }
            return Optional.of(TcpClientTransport.create(transportURI.getHost(), transportURI.getPort()));
        }
        if ("ws".equalsIgnoreCase(scheme) || "wss".equalsIgnoreCase(scheme)) {
            return Optional.of(WebsocketClientTransport.create(transportURI));
        }
        return Optional.empty();
    }
}
