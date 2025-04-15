/*
 *    Copyright 2025 the original author or authors.
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *       https://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 */
package pro.chenggang.project.rsocket.micro.connect.spring.server.argument;

import org.springframework.core.MethodParameter;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.RequestHeader;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Objects;

import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.CONNECTOR_HEADER_METADATA_KEY;

/**
 * Connector header multiple data handler method argument resolver
 * Resolve for {@link RequestHeader @RequestHeader} method parameters of HttpHeaders type.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public class ConnectorHeaderMapHandlerMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestHeader.class) && Map.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Mono<Object> resolveArgument(MethodParameter parameter, Message<?> message) {
        RequestHeader requestHeader = parameter.getParameterAnnotation(RequestHeader.class);
        Assert.state(requestHeader != null, "No RequestHeader annotation");
        HttpHeaders httpHeaders = (HttpHeaders) message.getHeaders().get(CONNECTOR_HEADER_METADATA_KEY);
        if (Objects.isNull(httpHeaders)) {
            return requestHeader.required() ? Mono.error(
                    new IllegalArgumentException("Missing connector header data for method parameter: " + parameter)) : Mono.empty();
        }
        if (MultiValueMap.class.isAssignableFrom(parameter.getParameterType())) {
            return Mono.just(httpHeaders);
        }
        return Mono.just(httpHeaders.toSingleValueMap());
    }
}
