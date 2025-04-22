package pro.chenggang.project.rsocket.micro.connect.spring.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * The request part name.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequestPartName {

    /**
     * Whether the part name is required.
     * <p>Defaults to {@code true}, leading to an exception being thrown
     * if the part name is missing in the request. Switch this to
     * {@code false} if you prefer a {@code null} value if the part name is not present.
     */
    boolean required() default true;

}