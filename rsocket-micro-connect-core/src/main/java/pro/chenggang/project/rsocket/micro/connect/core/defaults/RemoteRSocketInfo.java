package pro.chenggang.project.rsocket.micro.connect.core.defaults;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

/**
 * The Remote rsocket info.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RemoteRSocketInfo {

    public static final String ATTRIBUTE_KEY = RemoteRSocketInfo.class.getName()+".attribute-key";

    private final String host;
    private final int port;

    public String getInfo() {
        return this.host + ":" + this.port;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[host=" + this.host + ", port=" + this.port + "]";
    }
}
