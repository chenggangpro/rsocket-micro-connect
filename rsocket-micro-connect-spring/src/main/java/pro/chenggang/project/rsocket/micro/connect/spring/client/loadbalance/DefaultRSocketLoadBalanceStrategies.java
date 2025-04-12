package pro.chenggang.project.rsocket.micro.connect.spring.client.loadbalance;

import io.rsocket.loadbalance.LoadbalanceStrategy;
import io.rsocket.loadbalance.RoundRobinLoadbalanceStrategy;
import lombok.NonNull;

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
    public LoadbalanceStrategy getLoadBalanceStrategy(@NonNull String rsocketHost) {
        return this.loadbalanceStrategy;
    }
}
