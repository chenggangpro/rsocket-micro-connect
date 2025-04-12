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
            return Optional.of(TcpClientTransport.create(transportURI.getHost(), transportURI.getPort()));
        }
        if ("ws".equalsIgnoreCase(scheme) || "wss".equalsIgnoreCase(scheme)) {
            return Optional.of(WebsocketClientTransport.create(transportURI));
        }
        return Optional.empty();
    }
}
