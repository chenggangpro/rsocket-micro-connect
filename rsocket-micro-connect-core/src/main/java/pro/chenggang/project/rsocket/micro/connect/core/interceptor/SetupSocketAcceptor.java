/*
 *    Copyright 2025 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
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
                                afterInterceptors
                        ))
                        .defaultIfEmpty(new ChainedInterceptedRSocket(rSocket,
                                dataMimeType,
                                metadataMimeType,
                                beforeInterceptors,
                                afterInterceptors
                        )));
    }

    protected WellKnownMimeType parseMimeType(String str, WellKnownMimeType defaultMimeType) {
        return (Objects.nonNull(str) && !str.isBlank()) ? WellKnownMimeType.fromString(str) : defaultMimeType;
    }

}
