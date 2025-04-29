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
package pro.chenggang.project.rsocket.micro.connect.spring.client.loadbalance;

import io.rsocket.loadbalance.LoadbalanceStrategy;
import io.rsocket.loadbalance.RoundRobinLoadbalanceStrategy;
import lombok.NonNull;

import java.net.URI;

/**
 * The default rsocket load-balance strategies.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public class DefaultRSocketLoadBalanceStrategies implements RSocketLoadBalanceStrategies {

    private final LoadbalanceStrategy loadbalanceStrategy = new RoundRobinLoadbalanceStrategy();

    @Override
    public LoadbalanceStrategy getLoadBalanceStrategy(@NonNull URI transportURI) {
        return this.loadbalanceStrategy;
    }
}
