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

import lombok.Getter;
import lombok.NonNull;
import pro.chenggang.project.rsocket.micro.connect.spring.client.RSocketRequesterRegistry;

import java.lang.reflect.Proxy;
import java.util.List;

/**
 * The rsocket micro connector proxy factory.
 *
 * @param <T> the type parameter
 * @author Gang Cheng
 */
@Getter
public class RSocketMicroConnectorProxyFactory<T> {

    /**
     * The connector interface
     */
    private final Class<T> connectorInterface;

    /**
     * Connector execution customizer list
     */
    private final List<RSocketMicroConnectorExecutionCustomizer> connectorExecutionCustomizers;

    /**
     * Instantiates a new rsocket micro connector proxy factory.
     *
     * @param connectorInterface            the rsocket micro connector interface
     * @param connectorExecutionCustomizers the connector execution customizer list
     */
    public RSocketMicroConnectorProxyFactory(Class<T> connectorInterface,
                                             List<RSocketMicroConnectorExecutionCustomizer> connectorExecutionCustomizers) {
        this.connectorInterface = connectorInterface;
        this.connectorExecutionCustomizers = connectorExecutionCustomizers;
    }

    /**
     * New rsocket micro connector proxy instance.
     *
     * @param rSocketRequesterRegistry the rsocket requester registry
     * @return the rsocket micro connector proxy instance
     */
    public T newInstance(@NonNull RSocketRequesterRegistry rSocketRequesterRegistry) {
        final RSocketMicroConnectorProxy<T> serviceProxy = new RSocketMicroConnectorProxy<>(connectorInterface,
                rSocketRequesterRegistry,
                connectorExecutionCustomizers
        );
        return newInstance(serviceProxy);
    }

    protected T newInstance(RSocketMicroConnectorProxy<T> rSocketMicroConnectorProxy) {
        return (T) Proxy.newProxyInstance(connectorInterface.getClassLoader(), new Class[]{connectorInterface},
                rSocketMicroConnectorProxy
        );
    }

}
