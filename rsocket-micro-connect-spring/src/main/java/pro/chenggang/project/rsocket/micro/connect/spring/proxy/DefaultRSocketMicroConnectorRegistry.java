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
package pro.chenggang.project.rsocket.micro.connect.spring.proxy;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.core.convert.ConversionService;
import pro.chenggang.project.rsocket.micro.connect.spring.client.RSocketRequesterRegistry;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The default rsocket micro connector registry.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@RequiredArgsConstructor
public class DefaultRSocketMicroConnectorRegistry implements RSocketMicroConnectorRegistry {

    private final RSocketRequesterRegistry rSocketRequesterRegistry;
    private final ConversionService conversionService;
    private final Map<Class<?>, RSocketMicroConnectorProxyFactory<?>> connectorProxyFactoryCache = new ConcurrentHashMap<>();
    private final List<RSocketMicroConnectorExecutionCustomizer> rSocketMicroConnectorExecutionCustomizers;

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getRSocketConnectorInstance(@NonNull Class<T> type) {
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Type " + type + " should be an interface");
        }
        RSocketMicroConnectorProxyFactory<?> rSocketMicroConnectorProxyFactory = connectorProxyFactoryCache.computeIfAbsent(type,
                connectorInterface -> new RSocketMicroConnectorProxyFactory<>(connectorInterface,
                        rSocketMicroConnectorExecutionCustomizers
                )
        );
        return (T) rSocketMicroConnectorProxyFactory.newInstance(rSocketRequesterRegistry, conversionService);
    }

}
