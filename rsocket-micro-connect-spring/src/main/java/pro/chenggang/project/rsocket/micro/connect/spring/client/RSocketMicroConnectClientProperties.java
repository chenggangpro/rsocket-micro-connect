package pro.chenggang.project.rsocket.micro.connect.spring.client;

import io.rsocket.metadata.WellKnownMimeType;
import lombok.Getter;
import lombok.Setter;

/**
 * The RSocket micro connect server properties.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Getter
@Setter
public class RSocketMicroConnectClientProperties {

    /**
     * The properties PREFIX.
     */
    public static final String PREFIX = "rsocket-micro-connect.client";

    /**
     * The Default data mime type.
     */
    public WellKnownMimeType defaultDataMimeType = WellKnownMimeType.APPLICATION_CBOR;
    /**
     * The Default metadata mime type.
     */
    public WellKnownMimeType defaultMetadataMimeType = WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA;
}
