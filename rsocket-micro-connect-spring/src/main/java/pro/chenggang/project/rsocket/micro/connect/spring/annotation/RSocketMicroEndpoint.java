package pro.chenggang.project.rsocket.micro.connect.spring.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The RSocket Micro Endpoint.
 * This should be annotated on Controller Class
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Controller
@MessageMapping
public @interface RSocketMicroEndpoint {

    @AliasFor(annotation = MessageMapping.class)
    String[] value() default {};
}