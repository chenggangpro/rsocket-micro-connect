package pro.chenggang.project.rsocket.micro.connect.spring.proxy;

import lombok.RequiredArgsConstructor;
import pro.chenggang.project.rsocket.micro.connect.spring.client.RSocketRequesterRegistry;

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
    private final Map<Class<?>, RSocketMicroConnectorProxyFactory<?>> connectorProxyFactoryCache = new ConcurrentHashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public <T> T getRSocketConnectorInstance(Class<T> type) {
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Type " + type + " should be an interface");
        }
        RSocketMicroConnectorProxyFactory<?> rSocketMicroConnectorProxyFactory = connectorProxyFactoryCache.computeIfAbsent(type,
                RSocketMicroConnectorProxyFactory::new
        );
        return (T) rSocketMicroConnectorProxyFactory.newInstance(rSocketRequesterRegistry);
    }
}
