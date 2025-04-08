package pro.chenggang.project.rsocket.micro.connect.spring.proxy;

/**
 * The RSocket micro connector registry.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public interface RSocketMicroConnectorRegistry {

    /**
     * Gets rsocket connector instance.
     *
     * @param <T>  the connector interface
     * @param type the connector interface's class
     * @return the rsocket connector instance
     */
    <T> T getRSocketConnectorInstance(Class<T> type);
}