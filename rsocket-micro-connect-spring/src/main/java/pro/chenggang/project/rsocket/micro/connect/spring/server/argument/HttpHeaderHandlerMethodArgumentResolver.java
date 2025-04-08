package pro.chenggang.project.rsocket.micro.connect.spring.server.argument;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodArgumentResolver;
import org.springframework.web.bind.annotation.RequestHeader;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.HTTP_HEADER_METADATA_KEY;

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
        Object httpHeaders = message.getHeaders().get(HTTP_HEADER_METADATA_KEY);
        RequestHeader requestHeader = parameter.getParameterAnnotation(RequestHeader.class);
        if (Objects.isNull(httpHeaders)) {
            return requestHeader.required() ? Mono.error(new IllegalArgumentException("Missing HttpHeaders in method parameter: " + parameter)) : Mono.empty();
        }
        return Mono.just(httpHeaders);
    }
}
