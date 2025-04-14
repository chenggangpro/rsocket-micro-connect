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
