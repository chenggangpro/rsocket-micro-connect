package pro.chenggang.project.rsocket.micro.connect.spring.client.loadbalance;

import io.rsocket.loadbalance.LoadbalanceStrategy;
import lombok.NonNull;

/**
 * The rocket load-balance strategies.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public interface RSocketLoadBalanceStrategies {

    /**
     * Gets load-balance strategy.
     *
     * @param rsocketHost the rsocket host
     * @return the load-balance strategy
     */
    LoadbalanceStrategy getLoadBalanceStrategy(@NonNull String rsocketHost);
}
