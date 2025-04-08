package pro.chenggang.project.rsocket.micro.connect.core.interceptor;

import io.rsocket.ConnectionSetupPayload;
import io.rsocket.DuplexConnection;
import io.rsocket.RSocket;
import io.rsocket.SocketAcceptor;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.util.RSocketProxy;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionAfterInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionBeforeInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionUnexpectedInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.defaults.RemoteRSocketInfo;
import reactor.core.publisher.Mono;

import java.lang.reflect.Field;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

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
                .flatMap(rSocket -> Mono.fromCallable(() -> this.getRemoteRSocketInfo(sendingSocket))
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

    protected Optional<RemoteRSocketInfo> getRemoteRSocketInfo(RSocket rSocket) {
        try {
            RSocket unwrapedRsocket = this.unwrapRSocketProxy(rSocket);
            Field connectionField = findField(unwrapedRsocket.getClass(), "connection", DuplexConnection.class);
            if (Objects.isNull(connectionField)) {
                log.warn("Unable to get DuplexConnection field from RSocket: {}", unwrapedRsocket);
                return Optional.empty();
            }
            connectionField.trySetAccessible();
            DuplexConnection duplexConnection = (DuplexConnection) connectionField.get(unwrapedRsocket);
            SocketAddress socketAddress = duplexConnection.remoteAddress();
            if (socketAddress instanceof InetSocketAddress inetSocketAddress) {
                RemoteRSocketInfo remoteRSocketInfo = RemoteRSocketInfo.builder()
                        .host(inetSocketAddress.getHostString())
                        .port(inetSocketAddress.getPort())
                        .build();
                return Optional.of(remoteRSocketInfo);
            }
            log.warn("Unable to get remote rsocket info from SocketAddress: {} of RSocket: {}", socketAddress, unwrapedRsocket);
        } catch (IllegalAccessException e) {
            log.warn("Unable to get remote rsocket info from RSocket: {}", rSocket, e);
        }
        return Optional.empty();
    }

    protected RSocket unwrapRSocketProxy(RSocket rSocket) throws IllegalAccessException {
        if (!(rSocket instanceof RSocketProxy)) {
            return rSocket;
        }
        Field field = findField(RSocketProxy.class, "source", RSocket.class);
        if (Objects.isNull(field)) {
            return rSocket;
        }
        field.trySetAccessible();
        RSocket source = (RSocket) field.get(rSocket);
        return unwrapRSocketProxy(source);
    }

    protected Field findField(Class<?> clazz, String name, Class<?> type) {
        Class<?> searchType = clazz;
        while (Object.class != searchType && searchType != null) {
            Field[] fields = searchType.getDeclaredFields();
            for (Field field : fields) {
                if ((name == null || name.equals(field.getName())) && (type == null || type.equals(field.getType()))) {
                    return field;
                }
            }
            searchType = searchType.getSuperclass();
        }
        return null;
    }

}
