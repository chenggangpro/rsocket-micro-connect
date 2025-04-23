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
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.handler.invocation.reactive.HandlerMethodArgumentResolver;
import org.springframework.util.Assert;
import pro.chenggang.project.rsocket.micro.connect.spring.annotation.RequestPartName;
import reactor.core.publisher.Mono;

import java.util.Objects;

import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.CONNECTOR_FILE_PART_NAME_METADATA_KEY;

/**
 * Resolve for {@link RequestPartName @RequestPartName} method parameters of String type.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public class RequestPartNameMethodArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestPartName.class) && String.class.equals(parameter.getParameterType());
    }

    @Override
    public Mono<Object> resolveArgument(MethodParameter parameter, Message<?> message) {
        Object fileStreamName = message.getHeaders().get(CONNECTOR_FILE_PART_NAME_METADATA_KEY);
        RequestPartName requestPartName = parameter.getParameterAnnotation(RequestPartName.class);
        Assert.state(requestPartName != null, "No RequestPartName annotation");
        if (requestPartName.required() && Objects.isNull(fileStreamName)) {
            return Mono.error(new MessageHandlingException(message,
                    "Missing request part name for method parameter [" + parameter.getParameterName() + "]"
            ));
        }
        return Mono.justOrEmpty(fileStreamName);
    }
}
