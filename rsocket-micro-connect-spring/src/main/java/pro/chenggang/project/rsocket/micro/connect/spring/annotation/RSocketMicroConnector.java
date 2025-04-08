package pro.chenggang.project.rsocket.micro.connect.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The RSocket Micro Connect.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RSocketMicroConnector {

    /**
     * RSocket binding address.
     * <li>ws://127.0.0.1:8080 for websocket transport</li>
     * <li>wws://127.0.0.1:8080 for websocket transport</li>
     * <li>tcp://127.0.0.1:8080 for tcp transport</li>
     *
     * @return the address must be a String value which can be parsed to a URI instance
     */
    String value();

}
