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
import org.springframework.core.ReactiveAdapterRegistry;
import org.springframework.core.codec.Decoder;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.messaging.handler.annotation.reactive.PayloadMethodArgumentResolver;
import org.springframework.validation.Validator;
import org.springframework.web.bind.annotation.RequestPart;
import pro.chenggang.project.rsocket.micro.connect.core.util.RSocketMicroConnectUtil;
import reactor.core.publisher.Flux;

import java.util.List;

/**
 * The request part payload method argument resolver.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public class RequestPartPayloadMethodArgumentResolver extends PayloadMethodArgumentResolver {

    public RequestPartPayloadMethodArgumentResolver(List<? extends Decoder<?>> decoders,
                                                    Validator validator,
                                                    ReactiveAdapterRegistry registry) {
        super(decoders, validator, registry, false);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        if (!parameter.hasParameterAnnotation(RequestPart.class)) {
            return false;
        }
        if (!Flux.class.equals(parameter.getParameterType())) {
            return false;
        }
        Class<?> inferredClass = RSocketMicroConnectUtil.parseInferredClass(parameter.getGenericParameterType());
        return DataBuffer.class.equals(inferredClass);
    }

}
