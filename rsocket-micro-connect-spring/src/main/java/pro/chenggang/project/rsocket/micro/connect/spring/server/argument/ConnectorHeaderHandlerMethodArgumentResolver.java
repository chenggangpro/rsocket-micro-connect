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
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.handler.annotation.reactive.AbstractNamedValueMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.MissingRequestValueException;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.CONNECTOR_HEADER_METADATA_KEY;

/**
 * Connector header handler single data method argument resolver
 * Resolve for {@link RequestHeader @RequestHeader} method parameters of HttpHeaders type.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public class ConnectorHeaderHandlerMethodArgumentResolver extends AbstractNamedValueMethodArgumentResolver {

    public ConnectorHeaderHandlerMethodArgumentResolver(ConversionService conversionService) {
        super(conversionService, null);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(RequestHeader.class) && !Map.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
        RequestHeader ann = parameter.getParameterAnnotation(RequestHeader.class);
        Assert.state(ann != null, "No RequestHeader annotation");
        return new RequestHeaderNamedValueInfo(ann);
    }

    @Override
    protected Object resolveArgumentInternal(MethodParameter parameter, Message<?> message, String name) {
        HttpHeaders httpHeaders = (HttpHeaders) message.getHeaders().get(CONNECTOR_HEADER_METADATA_KEY);
        if (Objects.isNull(httpHeaders)) {
            return null;
        }
        List<String> values = httpHeaders.get(name);
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
        throw new MissingRequestValueException(name, parameter.getNestedParameterType(), "header", parameter);
    }

    private static final class RequestHeaderNamedValueInfo extends NamedValueInfo {

        private RequestHeaderNamedValueInfo(RequestHeader annotation) {
            super(annotation.name(), annotation.required(), annotation.defaultValue());
        }
    }
}
