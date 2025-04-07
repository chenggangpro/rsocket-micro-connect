package pro.chenggang.project.rsocket.micro.connect.spring.server.argument;

import org.springframework.core.MethodParameter;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodArgumentResolver;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;
import reactor.core.publisher.Mono;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.HTTP_QUERY_METADATA_KEY;

/**
 * Http query to Map handler method argument resolver
 * Resolve for {@link RequestParam @RequestParam} method parameters of MultiValueMap type.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public class HttpQueryMapHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        return (requestParam != null && Map.class.isAssignableFrom(parameter.getParameterType())
                && !StringUtils.hasText(requestParam.name()));
    }

    @Override
    public Mono<Object> resolveArgument(MethodParameter parameter, Message<?> message) {
        Object httpQueries = message.getHeaders().get(HTTP_QUERY_METADATA_KEY);
        if (parameter.hasParameterAnnotation(Nullable.class) && Objects.isNull(httpQueries)) {
            return Mono.empty();
        }
        if (MultiValueMap.class.isAssignableFrom(parameter.getParameterType())) {
            return Mono.justOrEmpty(httpQueries)
                    .defaultIfEmpty(new LinkedMultiValueMap<>(0));
        }
        return Mono.justOrEmpty(httpQueries)
                .map(queries -> {
                    MultiValueMap<String, String> parameterMap = (MultiValueMap<String, String>) queries;
                    Map<String, String> result = CollectionUtils.newLinkedHashMap(parameterMap.size());
                    parameterMap.forEach((key, values) -> {
                        if (values.size() > 0) {
                            result.put(key, values.get(0));
                        }
                    });
                    return result;
                })
                .defaultIfEmpty(new LinkedHashMap<>(0))
                .cast(Object.class);
    }
}
