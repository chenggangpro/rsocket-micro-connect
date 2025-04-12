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

import io.rsocket.RSocket;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.RSocketServer;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import reactor.core.publisher.Mono;

/**
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@TestInstance(Lifecycle.PER_CLASS)
public abstract class AbstractRSocketMicroConnectTests {

    int serverPort = 7878;
    String data = "hello world";
    String metadata = "metadata";
    CloseableChannel closeableChannel;
    RSocket clientRSocket;

    abstract void configureServer(RSocketServer rSocketServer);

    abstract void configureClient(RSocketConnector rSocketConnector);

    @BeforeAll
    void beforeAll() {
        RSocketServer rSocketServer = RSocketServer.create((setup, rsocket) -> Mono.just(new TestRSocket(data, metadata)))
                .payloadDecoder(PayloadDecoder.ZERO_COPY);
        this.configureServer(rSocketServer);
        closeableChannel = rSocketServer
                .bind(TcpServerTransport.create(serverPort))
                .block();
        RSocketConnector rSocketConnector = RSocketConnector.create()
                .payloadDecoder(PayloadDecoder.ZERO_COPY);
        this.configureClient(rSocketConnector);
        clientRSocket = rSocketConnector
                .connect(TcpClientTransport.create(serverPort))
                .block();
    }

    @AfterAll
    void afterAll() {
        closeableChannel.dispose();
    }

}
