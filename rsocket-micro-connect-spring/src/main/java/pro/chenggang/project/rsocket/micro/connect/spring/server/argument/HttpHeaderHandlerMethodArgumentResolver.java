package pro.chenggang.project.rsocket.micro.connect.spring.server.argument;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodArgumentResolver;
import org.springframework.web.bind.annotation.RequestHeader;
import reactor.core.publisher.Mono;

import java.util.Objects;

/**
 * Http header handler method argument resolver
 * Resolve for {@link RequestHeader @RequestHeader} method parameters of HttpHeaders type.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public class HttpHeaderHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestHeader.class) && HttpHeaders.class.equals(parameter.getParameterType());
    }

    @Override
    public Mono<Object> resolveArgument(MethodParameter parameter, Message<?> message) {
        Object httpHeaders = message.getHeaders().get(HttpHeaders.class.getName());
        if (parameter.hasParameterAnnotation(Nullable.class) && Objects.isNull(httpHeaders)) {
            return Mono.empty();
        }
        if (Objects.isNull(httpHeaders)) {
            return Mono.error(new IllegalArgumentException("Missing HttpHeaders in Message of : "
                    + parameter
                    + ". You can annotate @Nullable(org.springframework.lang.Nullable) on parameter if you allow null value"));
        }
        return Mono.just(httpHeaders);
    }
}
