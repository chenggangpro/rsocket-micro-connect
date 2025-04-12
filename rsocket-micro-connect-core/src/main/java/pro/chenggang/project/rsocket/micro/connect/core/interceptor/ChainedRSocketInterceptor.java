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
