package pro.chenggang.project.rsocket.micro.connect.spring.client;

import lombok.NonNull;
import org.springframework.messaging.rsocket.RSocketRequester;

import java.net.URI;

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
}
