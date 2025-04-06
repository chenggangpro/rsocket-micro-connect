package pro.chenggang.project.rsocket.micro.connect.core;

import io.rsocket.RSocket;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.plugins.RSocketInterceptor;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionAfterInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionBeforeInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionUnexpectedInterceptor;

import java.util.List;

/**
 * The Chained rsocket interceptor.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@RequiredArgsConstructor
public class ChainedRSocketInterceptor implements RSocketInterceptor {

    @NonNull
    private final WellKnownMimeType defaultDataMimeType;
    @NonNull
    private final WellKnownMimeType defaultMetadataMimeType;
    private final List<RSocketExecutionBeforeInterceptor> beforeInterceptors;
    private final List<RSocketExecutionAfterInterceptor> afterInterceptors;
    private final List<RSocketExecutionUnexpectedInterceptor> unexpectedInterceptors;

    @Override
    public RSocket apply(RSocket rSocket) {
        return new ChainedInterceptedRSocket(rSocket,
                defaultDataMimeType,
                defaultMetadataMimeType,
                beforeInterceptors,
                afterInterceptors,
                unexpectedInterceptors
        );
    }
}
