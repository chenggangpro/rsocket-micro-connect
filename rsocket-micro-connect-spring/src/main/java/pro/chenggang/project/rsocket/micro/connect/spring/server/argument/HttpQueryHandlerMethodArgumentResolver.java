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
import org.springframework.core.convert.ConversionService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandlingException;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.handler.annotation.ValueConstants;
import org.springframework.messaging.handler.annotation.reactive.AbstractNamedValueMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;
import java.util.Objects;

import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.HTTP_QUERY_METADATA_KEY;

/**
 * Http query handler method argument resolver
 * Resolve for {@link RequestParam @RequestParam} method parameters of MultiValueMap type.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public class HttpQueryHandlerMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

    public HttpQueryHandlerMethodArgumentResolver(ConversionService conversionService) {
        super(conversionService, null);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        RequestParam requestParam = parameter.getParameterAnnotation(RequestParam.class);
        return (requestParam != null && StringUtils.hasText(requestParam.name()));
    }

    @Override
    protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
        RequestParam annot = parameter.getParameterAnnotation(RequestParam.class);
        Assert.state(annot != null, "No RequestParam annotation");
        return new QueryVariableNamedValueInfo(annot);
    }

    @Override
    protected Object resolveArgumentInternal(MethodParameter parameter, Message<?> message, String name) {
        MessageHeaders headers = message.getHeaders();
        MultiValueMap<String, String> vars = (MultiValueMap<String, String>) headers.get(HTTP_QUERY_METADATA_KEY);
        if (Objects.isNull(vars)) {
            return null;
        }
        List<String> values = vars.get(name);
        if (Objects.isNull(values)) {
            return null;
        }
        if (values.size() == 1) {
            return values.get(0);
        }
        return values;
    }

    @Override
    protected void handleMissingValue(String name, MethodParameter parameter, Message<?> message) {
        throw new MessageHandlingException(message, "Missing query variable '" + name + "' " +
                "for method parameter type [" + parameter.getParameterType() + "]");
    }

    private static final class QueryVariableNamedValueInfo extends NamedValueInfo {

        private QueryVariableNamedValueInfo(RequestParam annotation) {
            super(annotation.value(), annotation.required(), ValueConstants.DEFAULT_NONE);
        }
    }

}
