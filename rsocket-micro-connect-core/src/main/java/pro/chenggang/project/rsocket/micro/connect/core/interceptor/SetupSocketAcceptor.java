package pro.chenggang.project.rsocket.micro.connect.core.interceptor;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.metadata.WellKnownMimeType;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionAfterInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionBeforeInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionUnexpectedInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.util.RSocketMicroConnectUtil;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Objects;

/**
 * The Setup socket acceptor.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Slf4j
@RequiredArgsConstructor
public class SetupSocketAcceptor implements SocketAcceptor {

    @NonNull
    private final WellKnownMimeType defaultDataMimeType;
    @NonNull
    private final WellKnownMimeType defaultMetadataMimeType;
    private final SocketAcceptor delegate;
    private final List<RSocketExecutionBeforeInterceptor> beforeInterceptors;
    private final List<RSocketExecutionAfterInterceptor> afterInterceptors;
    private final List<RSocketExecutionUnexpectedInterceptor> unexpectedInterceptors;

    @Override
    public Mono<RSocket> accept(ConnectionSetupPayload setup, RSocket sendingSocket) {
        WellKnownMimeType dataMimeType = parseMimeType(setup.dataMimeType(), this.defaultDataMimeType);
        WellKnownMimeType metadataMimeType = parseMimeType(setup.metadataMimeType(), this.defaultMetadataMimeType);
        return this.delegate.accept(setup, sendingSocket)
                .flatMap(rSocket -> Mono.fromCallable(() -> RSocketMicroConnectUtil.getRemoteRSocketInfo(sendingSocket))
                        .flatMap(Mono::justOrEmpty)
                        .doOnNext(remoteRSocketInfo -> {
                            log.info("RSocketClient[{}] connected successfully", remoteRSocketInfo.getInfo());
                        })
                        .map(remoteRSocketInfo -> new ChainedInterceptedRSocket(rSocket,
                                dataMimeType,
                                metadataMimeType,
                                remoteRSocketInfo,
                                beforeInterceptors,
                                afterInterceptors,
                                unexpectedInterceptors
                        ))
                        .defaultIfEmpty(new ChainedInterceptedRSocket(rSocket,
                                dataMimeType,
                                metadataMimeType,
                                beforeInterceptors,
                                afterInterceptors,
                                unexpectedInterceptors
                        )));
    }

    protected WellKnownMimeType parseMimeType(String str, WellKnownMimeType defaultMimeType) {
        return (Objects.nonNull(str) && !str.isBlank()) ? WellKnownMimeType.fromString(str) : defaultMimeType;
    }

}
