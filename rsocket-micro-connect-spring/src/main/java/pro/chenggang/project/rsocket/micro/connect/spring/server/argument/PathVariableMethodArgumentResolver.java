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
import org.springframework.messaging.handler.annotation.ValueConstants;
import org.springframework.messaging.handler.annotation.reactive.DestinationVariableMethodArgumentResolver;
import org.springframework.util.Assert;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * <b>THIS CLASS IS INHERITED FROM {@link org.springframework.messaging.handler.annotation.reactive.DestinationVariableMethodArgumentResolver}</b>
 * Resolve for {@link PathVariable @PathVariable} method parameters.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public class PathVariableMethodArgumentResolver extends DestinationVariableMethodArgumentResolver {

    public PathVariableMethodArgumentResolver(ConversionService conversionService) {
        super(conversionService);
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(PathVariable.class);
    }

    @Override
    protected NamedValueInfo createNamedValueInfo(MethodParameter parameter) {
        PathVariable annot = parameter.getParameterAnnotation(PathVariable.class);
        Assert.state(annot != null, "No PathVariable annotation");
        return new PathVariableNamedValueInfo(annot);
    }

    private static final class PathVariableNamedValueInfo extends NamedValueInfo {

        private PathVariableNamedValueInfo(PathVariable annotation) {
            super(annotation.value(), annotation.required(), ValueConstants.DEFAULT_NONE);
        }
    }
}
