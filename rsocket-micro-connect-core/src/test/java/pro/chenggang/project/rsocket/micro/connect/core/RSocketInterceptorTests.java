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
package pro.chenggang.project.rsocket.micro.connect.core;

import io.rsocket.Payload;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.RSocketServer;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.util.DefaultPayload;
import org.junit.jupiter.api.Test;
import pro.chenggang.project.rsocket.micro.connect.core.client.ContextClientSideInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.client.SimpleClientSideInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.interceptor.ChainedRSocketInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.interceptor.SetupSocketAcceptor;
import pro.chenggang.project.rsocket.micro.connect.core.server.ContextServerSideInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.server.SimpleServerSideInterceptor;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import reactor.util.context.Context;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public class RSocketInterceptorTests extends AbstractRSocketMicroConnectTests {

    @Override
    void configureServer(RSocketServer rSocketServer) {
        rSocketServer.interceptors(registry -> {
            registry.forSocketAcceptor(socketAcceptor -> {
                return new SetupSocketAcceptor(WellKnownMimeType.TEXT_PLAIN,
                        WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA,
                        socketAcceptor,
                        List.of(new SimpleServerSideInterceptor(), new ContextServerSideInterceptor()),
                        List.of(new SimpleServerSideInterceptor(), new ContextServerSideInterceptor()),
                        null
                );
            });
        });
    }

    @Override
    void configureClient(RSocketConnector rSocketConnector) {
        rSocketConnector.dataMimeType(WellKnownMimeType.TEXT_PLAIN.toString())
                .metadataMimeType(WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA.toString())
                .interceptors(registry -> {
                    registry.forRequester(new ChainedRSocketInterceptor(WellKnownMimeType.TEXT_PLAIN,
                            WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA,
                            List.of(new SimpleClientSideInterceptor(), new ContextClientSideInterceptor()),
                            List.of(new SimpleClientSideInterceptor(), new ContextClientSideInterceptor()),
                            null
                    ));
                });
    }

    @Test
    void testFireAndForget() {
        clientRSocket.fireAndForget(DefaultPayload.create("fire-and-forget", metadata))
                .contextWrite(Context.of("context-key", "context-value"))
                .as(StepVerifier::create)
                .verifyComplete();
    }

    @Test
    void testRequestAndResponse() {
        clientRSocket.requestResponse(DefaultPayload.create("request-and-response", metadata))
                .contextWrite(Context.of("context-key", "context-value"))
                .as(StepVerifier::create)
                .consumeNextWith(payload -> {
                    String dataUtf8 = payload.getDataUtf8();
                    assertEquals(data, dataUtf8);
                })
                .verifyComplete();
    }

    @Test
    void testRequestStream() {
        clientRSocket.requestStream(DefaultPayload.create("request-stream", metadata))
                .contextWrite(Context.of("context-key", "context-value"))
                .collectList()
                .as(StepVerifier::create)
                .consumeNextWith(payloadList -> {
                    payloadList.stream()
                            .map(Payload::getDataUtf8)
                            .forEach(dataUtf8 -> {
                                assertEquals(data, dataUtf8);
                            });
                })
                .verifyComplete();
    }

    @Test
    void testRequestChannel() {
        clientRSocket.requestChannel(Mono.just(DefaultPayload.create("request-channel", metadata)))
                .contextWrite(Context.of("context-key", "context-value"))
                .collectList()
                .as(StepVerifier::create)
                .consumeNextWith(payloadList -> {
                    payloadList.stream()
                            .map(Payload::getDataUtf8)
                            .forEach(dataUtf8 -> {
                                assertEquals("request-channel", dataUtf8);
                            });
                })
                .verifyComplete();
    }

    @Test
    void testMetadataPush() {
        clientRSocket.metadataPush(DefaultPayload.create("request-channel", metadata))
                .contextWrite(Context.of("context-key", "context-value"))
                .as(StepVerifier::create)
                .verifyComplete();
    }

}
