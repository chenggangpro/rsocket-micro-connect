package pro.chenggang.project.rsocket.micro.connect.core.exception;

import io.rsocket.RSocketErrorException;
import io.rsocket.frame.ErrorFrameCodec;
import lombok.NonNull;

import java.io.Serial;
import java.net.URI;

/**
 * The rsocket instance not found exception.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public class RSocketInstanceNotFoundException extends RSocketErrorException {

    @Serial
    private static final long serialVersionUID = -1151855281779918872L;

    public RSocketInstanceNotFoundException(@NonNull URI uri) {
        super(ErrorFrameCodec.CONNECTION_ERROR, "No load-balanced rsocket instance found for transport " + uri);
    }

}
