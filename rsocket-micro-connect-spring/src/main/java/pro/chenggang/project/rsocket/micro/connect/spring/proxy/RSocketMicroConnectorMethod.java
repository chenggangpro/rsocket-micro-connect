/*
 *    Copyright 2009-2024 the original author or authors.
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
package pro.chenggang.project.rsocket.micro.connect.spring.proxy;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.rsocket.RSocketRequester;
import org.springframework.messaging.rsocket.RSocketRequester.RequestSpec;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MimeTypeUtils;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pro.chenggang.project.rsocket.micro.connect.core.util.RSocketMicroConnectUtil;
import pro.chenggang.project.rsocket.micro.connect.spring.annotation.RSocketMicroConnector;
import pro.chenggang.project.rsocket.micro.connect.spring.client.RSocketRequesterRegistry;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static pro.chenggang.project.rsocket.micro.connect.core.util.RSocketMicroConnectUtil.resolveReturnType;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.HTTP_HEADER_MEDIA_TYPE;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.HTTP_QUERY_MEDIA_TYPE;

/**
 * The RSocket Micro Connect Method.
 *
 * @author Gang Cheng
 */
@Slf4j
public class RSocketMicroConnectorMethod {

    private final MethodSignature methodSignature;
    private final ConnectorMethodData connectorMethodData;

    public RSocketMicroConnectorMethod(Class<?> connectorInterface, Method method) {
        this.methodSignature = new MethodSignature(connectorInterface, method);
        this.connectorMethodData = new ConnectorMethodData(connectorInterface, method);
    }

    /**
     * Execute proxy method.
     *
     * @param rSocketRequesterRegistry the rsocket requester registry
     * @param args                     the args
     * @return the result value
     */
    public Object execute(RSocketRequesterRegistry rSocketRequesterRegistry, Object[] args) {
        RSocketRequester rSocketRequester = rSocketRequesterRegistry.getRSocketRequester(connectorMethodData.getTransportURI());
        if (this.methodSignature.returnsVoid) {
            if (this.methodSignature.returnsMany) {
                return Flux.from(this.executeFireAndForget(rSocketRequester, args));
            }
            return Mono.from(this.executeFireAndForget(rSocketRequester, args));
        }
        if (this.methodSignature.returnsMany) {
            return this.executeRequestStream(rSocketRequester, args);
        }
        return this.executeRequestResponse(rSocketRequester, args);
    }

    /**
     * Execute fire-and-forget by rsocket
     *
     * @param rSocketRequester the rsocket requester
     * @param args             the ars
     * @return {@code Publisher<Void>}
     */
    private Publisher<Void> executeFireAndForget(RSocketRequester rSocketRequester, Object[] args) {
        RequestSpec requestSpec = this.newRequestSpec(rSocketRequester, args);
        this.resolveMetadataIfPresent(requestSpec, args);
        this.resolveBodyIfPresent(requestSpec, args);
        return requestSpec.send();
    }

    /**
     * Execute request-stream by rsocket
     *
     * @param rSocketRequester the rsocket requester
     * @param args             the args
     * @return {@code Flux<R>}
     */
    private <R> Flux<R> executeRequestStream(RSocketRequester rSocketRequester, Object[] args) {
        RequestSpec requestSpec = this.newRequestSpec(rSocketRequester, args);
        this.resolveMetadataIfPresent(requestSpec, args);
        this.resolveBodyIfPresent(requestSpec, args);
        return requestSpec.retrieveFlux(new ParameterizedTypeReference<>() {
            @Override
            public Type getType() {
                return methodSignature.getReturnType();
            }
        });
    }

    /**
     * Execute request-response by rsocket
     *
     * @param rSocketRequester the rsocket requester
     * @param args             the args
     * @return {@code Mono<R>}
     */
    private <R> Mono<R> executeRequestResponse(RSocketRequester rSocketRequester, Object[] args) {
        RequestSpec requestSpec = this.newRequestSpec(rSocketRequester, args);
        this.resolveMetadataIfPresent(requestSpec, args);
        this.resolveBodyIfPresent(requestSpec, args);
        return requestSpec.retrieveMono(new ParameterizedTypeReference<>() {
            @Override
            public Type getType() {
                return methodSignature.getReturnType();
            }
        });
    }

    private RSocketRequester.RequestSpec newRequestSpec(RSocketRequester rSocketRequester, Object[] args) {
        String route = this.connectorMethodData.getRoute();
        if (Objects.isNull(args) || args.length == 0) {
            return rSocketRequester.route(route);
        }
        String[] pathVariableNames = RSocketMicroConnectUtil.substringsBetween(route, "{", "}");
        if (Objects.isNull(pathVariableNames) || pathVariableNames.length == 0) {
            return rSocketRequester.route(route);
        }
        Annotation[][] parameterAnnotations = connectorMethodData.getParameterAnnotations();
        Map<String, Integer> pathVariables = new HashMap<>();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            Annotation[] annotations = parameterAnnotations[i];
            for (Annotation annotation : annotations) {
                if (DestinationVariable.class.equals(annotation.annotationType())
                        || PathVariable.class.equals(annotation.annotationType())) {
                    String pathValiableName = (String) AnnotationUtils.getValue(annotation);
                    if (!StringUtils.hasText(pathValiableName)) {
                        Parameter parameter = connectorMethodData.getParameters()[i];
                        pathValiableName = parameter.getName();
                    }
                    pathVariables.put(pathValiableName, i);
                    break;
                }
            }
        }
        Object[] pathVariableValues = new Object[pathVariableNames.length];
        for (int i = 0; i < pathVariableNames.length; i++) {
            String pathVariableName = pathVariableNames[i];
            Integer index = pathVariables.get(pathVariableName);
            Object variableValue = args[index];
            pathVariableValues[i] = variableValue;
        }
        return rSocketRequester.route(route, pathVariableValues);
    }

    private void resolveBodyIfPresent(RSocketRequester.RequestSpec requestSpec, Object[] args) {
        if (Objects.isNull(args) || args.length == 0) {
            return;
        }
        Annotation[][] parameterAnnotations = connectorMethodData.getParameterAnnotations();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            Annotation[] annotations = parameterAnnotations[i];
            boolean isRequestBodyAnnotationMatched = false;
            for (Annotation annotation : annotations) {
                if (PathVariable.class.equals(annotation.annotationType())
                        || DestinationVariable.class.equals(annotation.annotationType())
                        || RequestParam.class.equals(annotation.annotationType())
                        || RequestHeader.class.equals(annotation.annotationType())) {
                    break;
                }
                if (RequestBody.class.equals(annotation.annotationType())) {
                    isRequestBodyAnnotationMatched = true;
                    break;
                }
            }
            if (isRequestBodyAnnotationMatched) {
                // The first @RequestBoyd arg
                Object argValue = args[i];
                if (Objects.nonNull(argValue)) {
                    requestSpec.data(argValue);
                    return;
                }
            }
        }
        for (int i = 0; i < parameterAnnotations.length; i++) {
            Annotation[] annotations = parameterAnnotations[i];
            boolean isAnyResolvedAnnotationMatched = false;
            for (Annotation annotation : annotations) {
                if (PathVariable.class.equals(annotation.annotationType())
                        || DestinationVariable.class.equals(annotation.annotationType())
                        || RequestParam.class.equals(annotation.annotationType())
                        || RequestHeader.class.equals(annotation.annotationType())) {
                    isAnyResolvedAnnotationMatched = true;
                    break;
                }
            }
            if (!isAnyResolvedAnnotationMatched) {
                // The first no resolved annotation arg
                Object argValue = args[i];
                if (Objects.nonNull(argValue)) {
                    requestSpec.data(argValue);
                    return;
                }
            }
        }
    }

    private void resolveMetadataIfPresent(RSocketRequester.RequestSpec requestSpec, Object[] args) {
        if (Objects.isNull(args) || args.length == 0) {
            return;
        }
        this.resolveHttpHeaderValue(args)
                .ifPresent(httpHeaders -> {
                    requestSpec.metadata(metadataSpec -> {
                        metadataSpec.metadata(httpHeaders, MimeTypeUtils.parseMimeType(HTTP_HEADER_MEDIA_TYPE.toString()));
                    });
                });
        this.resolveQueryParamsValue(args)
                .ifPresent(queryParams -> {
                    requestSpec.metadata(metadataSpec -> {
                        metadataSpec.metadata(queryParams, MimeTypeUtils.parseMimeType(HTTP_QUERY_MEDIA_TYPE.toString()));
                    });
                });
    }

    private Optional<HttpHeaders> resolveHttpHeaderValue(Object[] args) {
        if (Objects.isNull(args) || args.length == 0) {
            return Optional.empty();
        }
        Annotation[][] parameterAnnotations = connectorMethodData.getParameterAnnotations();
        Parameter[] parameters = connectorMethodData.getParameters();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            Annotation[] annotations = parameterAnnotations[i];
            for (Annotation annotation : annotations) {
                if (RequestHeader.class.equals(annotation.annotationType())) {
                    Object argValue = args[i];
                    if (Objects.nonNull(argValue)) {
                        Class<?> parameterType = parameters[i].getType();
                        if (HttpHeaders.class.equals(parameterType)) {
                            return Optional.of((HttpHeaders) argValue);
                        }
                    }
                }
            }
        }
        return Optional.empty();
    }

    private Optional<MultiValueMap<String, String>> resolveQueryParamsValue(Object[] args) {
        if (Objects.isNull(args) || args.length == 0) {
            return Optional.empty();
        }
        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
        Annotation[][] parameterAnnotations = connectorMethodData.getParameterAnnotations();
        Parameter[] parameters = connectorMethodData.getParameters();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            Annotation[] annotations = parameterAnnotations[i];
            for (Annotation annotation : annotations) {
                if (RequestParam.class.equals(annotation.annotationType())) {
                    String requestParamName = (String) AnnotationUtils.getValue(annotation);
                    if (!StringUtils.hasText(requestParamName)) {
                        Parameter parameter = parameters[i];
                        requestParamName = parameter.getName();
                    }
                    Object argValue = args[i];
                    if (Objects.nonNull(argValue)) {
                        multiValueMap.add(requestParamName, String.valueOf(argValue));
                    }
                }
            }
        }
        if (multiValueMap.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(multiValueMap);
    }

    /**
     * The Method signature.
     */
    @Getter
    public static class MethodSignature {

        private final boolean returnsMany;
        private final boolean returnsVoid;
        private final Type returnType;

        /**
         * Instantiates a new Method signature.
         *
         * @param serviceInterface the mapper interface
         * @param method           the method
         */
        public MethodSignature(Class<?> serviceInterface, Method method) {
            Class<?> methodReturnType = method.getReturnType();
            if (void.class.equals(methodReturnType)) {
                throw new UnsupportedOperationException(
                        "Return type is void should be changed to Mono<Void> or Flux<Void>");
            }
            if (!Mono.class.equals(methodReturnType) && !Flux.class.equals(methodReturnType)) {
                throw new UnsupportedOperationException("Return type should by either Mono or Flux");
            }
            Type resolvedReturnType = resolveReturnType(method, serviceInterface);
            if (resolvedReturnType instanceof ParameterizedType parameterizedType) {
                this.returnType = parameterizedType.getActualTypeArguments()[0];
            } else {
                this.returnType = resolvedReturnType;
            }
            this.returnsVoid = Void.class.equals(this.returnType);
            this.returnsMany = Flux.class.equals(methodReturnType);
        }
    }

    @Getter
    public static class ConnectorMethodData {

        private final URI transportURI;
        private final String name;
        private final String route;
        private final Annotation[][] parameterAnnotations;
        private final Parameter[] parameters;

        public ConnectorMethodData(Class<?> connectorInterface, Method method) {
            RSocketMicroConnector rSocketMicroConnector = connectorInterface.getAnnotation(RSocketMicroConnector.class);
            if (Objects.isNull(rSocketMicroConnector)) {
                throw new IllegalArgumentException(
                        "RSocket micro connector interface should be annotated with a @RSocketMicroConnector annotation");
            }
            String value = (String) AnnotationUtils.getValue(rSocketMicroConnector);
            if (!StringUtils.hasText(value)) {
                throw new IllegalArgumentException("RSocket transport uri must not be blank in @RSocketMicroConnector");
            }
            URI transportURI = URI.create(value);
            if (!StringUtils.hasText(transportURI.getScheme())) {
                throw new IllegalArgumentException("RSocket transport scheme can not be blank");
            }
            if (!StringUtils.hasText(transportURI.getHost())) {
                throw new IllegalArgumentException("RSocket transport host can not be blank");
            }
            if (transportURI.getPort() <= 0) {
                throw new IllegalArgumentException("RSocket transport port can not be negative or zero");
            }
            this.transportURI = transportURI;
            this.name = method.getName();
            this.route = extractRoute(method);
            this.parameterAnnotations = method.getParameterAnnotations();
            this.parameters = method.getParameters();
        }

        /**
         * extract route
         *
         * @param method the service method
         * @return the route value
         */
        private String extractRoute(Method method) {
            Annotation[] annotations = method.getAnnotations();
            String route = null;
            for (Annotation annotation : annotations) {
                if (MessageMapping.class.isAssignableFrom(annotation.annotationType())) {
                    MessageMapping messageMapping = (MessageMapping) annotation;
                    if (messageMapping.value().length > 0) {
                        route = messageMapping.value()[0];
                        break;
                    }
                }
                if (RequestMapping.class.isAssignableFrom(annotation.annotationType())) {
                    RequestMapping requestMapping = (RequestMapping) annotation;
                    if (requestMapping.value().length > 0) {
                        route = requestMapping.value()[0];
                        break;
                    }
                }
            }
            if (!StringUtils.hasText(route)) {
                throw new IllegalArgumentException("Can not extract rsocket route from method : " + method);
            }
            return route;
        }
    }

}
