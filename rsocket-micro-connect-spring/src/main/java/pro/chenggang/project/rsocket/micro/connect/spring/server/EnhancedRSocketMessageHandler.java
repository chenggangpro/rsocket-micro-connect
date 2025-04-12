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
package pro.chenggang.project.rsocket.micro.connect.spring.server;

import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.messaging.handler.CompositeMessageCondition;
import org.springframework.messaging.handler.DestinationPatternsMessageCondition;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.annotation.ConnectMapping;
import org.springframework.messaging.rsocket.annotation.support.RSocketFrameTypeMessageCondition;
import org.springframework.messaging.rsocket.annotation.support.RSocketMessageHandler;
import org.springframework.messaging.rsocket.service.RSocketExchange;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMapping;

import java.lang.reflect.AnnotatedElement;
import java.util.Optional;

/**
 * Enhanced RSocket Message Handler
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public class EnhancedRSocketMessageHandler extends RSocketMessageHandler {

    @Override
    protected CompositeMessageCondition getCondition(AnnotatedElement element) {
        MessageMapping ann1 = AnnotatedElementUtils.findMergedAnnotation(element, MessageMapping.class);
        if (ann1 != null && ann1.value().length > 0) {
            return new CompositeMessageCondition(
                    RSocketFrameTypeMessageCondition.EMPTY_CONDITION,
                    new DestinationPatternsMessageCondition(processDestinations(ann1.value()), obtainRouteMatcher())
            );
        } else if (ann1 != null) {
            Optional<CompositeMessageCondition> optionalCompositeMessageCondition = enhanceMessageMapping(element);
            if (optionalCompositeMessageCondition.isPresent()) {
                return optionalCompositeMessageCondition.get();
            }
        }
        ConnectMapping ann2 = AnnotatedElementUtils.findMergedAnnotation(element, ConnectMapping.class);
        if (ann2 != null) {
            String[] patterns = processDestinations(ann2.value());
            return new CompositeMessageCondition(
                    RSocketFrameTypeMessageCondition.CONNECT_CONDITION,
                    new DestinationPatternsMessageCondition(patterns, obtainRouteMatcher())
            );
        }
        RSocketExchange ann3 = AnnotatedElementUtils.findMergedAnnotation(element, RSocketExchange.class);
        if (ann3 != null && StringUtils.hasText(ann3.value())) {
            String[] destinations = new String[]{ann3.value()};
            return new CompositeMessageCondition(
                    RSocketFrameTypeMessageCondition.EMPTY_CONDITION,
                    new DestinationPatternsMessageCondition(processDestinations(destinations),
                            obtainRouteMatcher()
                    )
            );
        }
        return null;
    }

    private Optional<CompositeMessageCondition> enhanceMessageMapping(AnnotatedElement annotatedElement) {
        RequestMapping requestMappingAnn = AnnotatedElementUtils.findMergedAnnotation(annotatedElement, RequestMapping.class);
        if (requestMappingAnn == null) {
            return Optional.empty();
        }
        CompositeMessageCondition compositeMessageCondition = new CompositeMessageCondition(
                RSocketFrameTypeMessageCondition.EMPTY_CONDITION,
                new DestinationPatternsMessageCondition(processDestinations(requestMappingAnn.value()), obtainRouteMatcher())
        );
        return Optional.of(compositeMessageCondition);
    }
}
