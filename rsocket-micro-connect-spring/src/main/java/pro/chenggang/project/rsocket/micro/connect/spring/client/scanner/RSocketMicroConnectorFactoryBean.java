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
package pro.chenggang.project.rsocket.micro.connect.spring.client.scanner;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.FactoryBean;
import pro.chenggang.project.rsocket.micro.connect.spring.proxy.RSocketMicroConnectorRegistry;

/**
 * The rocket micro connector factory bean.
 *
 * @param <T> the rsocket micro connector interface type
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@RequiredArgsConstructor
public class RSocketMicroConnectorFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> connectorInterface;
    private final RSocketMicroConnectorRegistry rSocketMicroConnectorRegistry;

    @Override
    public T getObject() throws Exception {
        return rSocketMicroConnectorRegistry.getRSocketConnectorInstance(connectorInterface);
    }

    @Override
    public Class<?> getObjectType() {
        return this.connectorInterface;
    }
}
