package pro.chenggang.project.rsocket.micro.connect.spring.option;

import org.springframework.http.MediaType;

/**
 * The rSocket micro connect constant.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public abstract class RSocketMicroConnectConstant {

    public static final MediaType HTTP_HEADER_MEDIA_TYPE = new MediaType("application", "http-header");

    public static final MediaType HTTP_QUERY_MEDIA_TYPE = new MediaType("application", "http-query");

    public static final String HTTP_HEADER_METADATA_KEY = "http-header";

    public static final String HTTP_QUERY_METADATA_KEY = "http-query";

    public static final String DISCOVER_ENABLE_RSOCKET_METADATA_KEY = "rsocket-micro-connect.enable";

    public static final String DISCOVER_RSOCKET_PORT_METADATA_KEY = "rsocket-micro-connect.port";
}
