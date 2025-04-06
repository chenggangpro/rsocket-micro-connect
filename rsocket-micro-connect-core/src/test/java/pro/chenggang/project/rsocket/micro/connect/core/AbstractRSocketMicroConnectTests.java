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
