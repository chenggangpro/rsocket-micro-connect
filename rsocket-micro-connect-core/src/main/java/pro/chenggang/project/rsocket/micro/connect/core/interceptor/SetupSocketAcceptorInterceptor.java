package pro.chenggang.project.rsocket.micro.connect.core.interceptor;

import io.rsocket.SocketAcceptor;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.plugins.SocketAcceptorInterceptor;
import lombok.extern.slf4j.Slf4j;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionAfterInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionBeforeInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionUnexpectedInterceptor;

import java.util.List;

import static io.rsocket.metadata.WellKnownMimeType.APPLICATION_CBOR;
import static io.rsocket.metadata.WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA;

/**
 * The Setup socket acceptor interceptor.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Slf4j
public class SetupSocketAcceptorInterceptor implements SocketAcceptorInterceptor {

    private final WellKnownMimeType defaultDataMimeType;
    private final WellKnownMimeType defaultMetadataMimeType;
    private final List<RSocketExecutionBeforeInterceptor> beforeInterceptors;
    private final List<RSocketExecutionAfterInterceptor> afterInterceptors;
    private final List<RSocketExecutionUnexpectedInterceptor> unexpectedInterceptors;

    public SetupSocketAcceptorInterceptor(List<RSocketExecutionBeforeInterceptor> beforeInterceptors,
                                          List<RSocketExecutionAfterInterceptor> afterInterceptors,
                                          List<RSocketExecutionUnexpectedInterceptor> unexpectedInterceptors) {
        this(APPLICATION_CBOR, MESSAGE_RSOCKET_COMPOSITE_METADATA, beforeInterceptors, afterInterceptors, unexpectedInterceptors);
    }

    public SetupSocketAcceptorInterceptor(WellKnownMimeType defaultDataMimeType,
                                          WellKnownMimeType defaultMetadataMimeType,
                                          List<RSocketExecutionBeforeInterceptor> beforeInterceptors,
                                          List<RSocketExecutionAfterInterceptor> afterInterceptors,
                                          List<RSocketExecutionUnexpectedInterceptor> unexpectedInterceptors) {
        this.defaultDataMimeType = defaultDataMimeType;
        this.defaultMetadataMimeType = defaultMetadataMimeType;
        this.beforeInterceptors = beforeInterceptors;
        this.afterInterceptors = afterInterceptors;
        this.unexpectedInterceptors = unexpectedInterceptors;
    }

    @Override
    public SocketAcceptor apply(SocketAcceptor socketAcceptor) {
        return new SetupSocketAcceptor(defaultDataMimeType,
                defaultMetadataMimeType,
                socketAcceptor,
                beforeInterceptors,
                afterInterceptors,
                unexpectedInterceptors
        );
    }

}
