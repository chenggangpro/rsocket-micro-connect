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

import io.rsocket.SocketAcceptor;
import io.rsocket.metadata.WellKnownMimeType;
import io.rsocket.plugins.SocketAcceptorInterceptor;
import lombok.extern.slf4j.Slf4j;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionAfterInterceptor;
import pro.chenggang.project.rsocket.micro.connect.core.api.RSocketExecutionBeforeInterceptor;

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

    public SetupSocketAcceptorInterceptor(List<RSocketExecutionBeforeInterceptor> beforeInterceptors,
                                          List<RSocketExecutionAfterInterceptor> afterInterceptors) {
        this(APPLICATION_CBOR, MESSAGE_RSOCKET_COMPOSITE_METADATA, beforeInterceptors, afterInterceptors);
    }

    public SetupSocketAcceptorInterceptor(WellKnownMimeType defaultDataMimeType,
                                          WellKnownMimeType defaultMetadataMimeType,
                                          List<RSocketExecutionBeforeInterceptor> beforeInterceptors,
                                          List<RSocketExecutionAfterInterceptor> afterInterceptors) {
        this.defaultDataMimeType = defaultDataMimeType;
        this.defaultMetadataMimeType = defaultMetadataMimeType;
        this.beforeInterceptors = beforeInterceptors;
        this.afterInterceptors = afterInterceptors;
    }

    @Override
    public SocketAcceptor apply(SocketAcceptor socketAcceptor) {
        return new SetupSocketAcceptor(defaultDataMimeType,
                defaultMetadataMimeType,
                socketAcceptor,
                beforeInterceptors,
                afterInterceptors
        );
    }

}
