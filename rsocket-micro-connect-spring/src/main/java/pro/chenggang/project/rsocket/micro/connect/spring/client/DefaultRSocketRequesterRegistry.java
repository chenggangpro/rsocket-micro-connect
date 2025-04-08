package pro.chenggang.project.rsocket.micro.connect.spring.client;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketStrategies;

import java.net.URI;

/**
 * The default rsocket requester registry.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@RequiredArgsConstructor
public class DefaultRSocketRequesterRegistry implements RSocketRequesterRegistry {

    private final RSocketRequester.Builder builder;
    private final RSocketStrategies rSocketStrategies;

    @Override
    public RSocketRequester getRSocketRequester(@NonNull URI transportURI) {
        String scheme = transportURI.getScheme();
        if ("tcp".equalsIgnoreCase(scheme)) {
            return builder.rsocketStrategies(rSocketStrategies)
                    .tcp(transportURI.getHost(), transportURI.getPort());
        }
        if ("ws".equalsIgnoreCase(scheme) || "wss".equalsIgnoreCase(scheme)) {
            return builder.rsocketStrategies(rSocketStrategies)
                    .websocket(transportURI);
        }
        throw new IllegalArgumentException("Un supported rsocket transport uri: " + transportURI);
    }
}
