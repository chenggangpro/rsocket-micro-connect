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
import org.springframework.util.Assert;
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
import pro.chenggang.project.rsocket.micro.connect.spring.proxy.ConnectorExecution.ConnectorExecutionBuilder;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static pro.chenggang.project.rsocket.micro.connect.core.util.RSocketMicroConnectUtil.resolveReturnType;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.CONNECTOR_HEADER_MEDIA_TYPE;
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.CONNECTOR_QUERY_MEDIA_TYPE;

/**
 * The RSocket Micro Connect Method.
 *
 * @author Gang Cheng
 */
@Slf4j
public class RSocketMicroConnectorMethod {

    private final MethodSignature methodSignature;
    private final ConnectorData connectorData;
    private final List<RSocketMicroConnectorExecutionCustomizer> executionCustomizers;

    public RSocketMicroConnectorMethod(Class<?> connectorInterface,
                                       Method method,
                                       List<RSocketMicroConnectorExecutionCustomizer> executionCustomizers) {
        this.methodSignature = new MethodSignature(connectorInterface, method);
        this.connectorData = new ConnectorData(connectorInterface, method);
        if (Objects.isNull(executionCustomizers) || executionCustomizers.isEmpty()) {
            this.executionCustomizers = Collections.emptyList();
        } else {
            this.executionCustomizers = executionCustomizers;
        }
    }

    /**
     * Execute proxy method.
     *
     * @param rSocketRequesterRegistry the rsocket requester registry
     * @param args                     the args
     * @return the result value
     */
    public Object execute(RSocketRequesterRegistry rSocketRequesterRegistry, Object[] args) {
        RSocketRequester rSocketRequester = rSocketRequesterRegistry.getRSocketRequester(connectorData.getTransportURI());
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
        ConnectorExecution connectorExecution = this.initConnectionExecution(args);
        RequestSpec requestSpec = this.resolveRequestSpec(rSocketRequester, connectorExecution);
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
        ConnectorExecution connectorExecution = this.initConnectionExecution(args);
        RequestSpec requestSpec = this.resolveRequestSpec(rSocketRequester, connectorExecution);
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
        ConnectorExecution connectorExecution = this.initConnectionExecution(args);
        RequestSpec requestSpec = this.resolveRequestSpec(rSocketRequester, connectorExecution);
        return requestSpec.retrieveMono(new ParameterizedTypeReference<>() {
            @Override
            public Type getType() {
                return methodSignature.getReturnType();
            }
        });
    }

    /**
     * Resolve connector execution to RSocketRequester.RequestSpec
     *
     * @param rSocketRequester   the rsocket requester
     * @param connectorExecution the connector execution
     * @return the RSocketRequester.RequestSpec
     */
    private RSocketRequester.RequestSpec resolveRequestSpec(RSocketRequester rSocketRequester,
                                                            ConnectorExecution connectorExecution) {
        for (RSocketMicroConnectorExecutionCustomizer executionCustomizer : executionCustomizers) {
            executionCustomizer.customize(connectorExecution);
        }
        RequestSpec requestSpec = getPathVariables(connectorExecution)
                .map(pathVariables -> rSocketRequester.route(connectorExecution.getRoute(), pathVariables))
                .orElseGet(() -> rSocketRequester.route(connectorExecution.getRoute()));
        MultiValueMap<String, String> headers = connectorExecution.getHeaders();
        if (!headers.isEmpty()) {
            requestSpec.metadata(metadataSpec -> {
                metadataSpec.metadata(headers, MimeTypeUtils.parseMimeType(CONNECTOR_HEADER_MEDIA_TYPE.toString()));
            });
        }
        MultiValueMap<String, String> queryParams = connectorExecution.getQueryParams();
        if (!queryParams.isEmpty()) {
            requestSpec.metadata(metadataSpec -> {
                metadataSpec.metadata(queryParams, MimeTypeUtils.parseMimeType(CONNECTOR_QUERY_MEDIA_TYPE.toString()));
            });
        }
        Object bodyData = connectorExecution.getBodyData();
        if (Objects.nonNull(bodyData)) {
            requestSpec.data(bodyData);
        }
        return requestSpec;
    }

    /**
     * Gets path variables from connector execution
     *
     * @param connectorExecution the connector execution
     * @return the optional path variable
     */
    private Optional<Object[]> getPathVariables(ConnectorExecution connectorExecution) {
        String route = connectorExecution.getRoute();
        String[] pathVariableNames = RSocketMicroConnectUtil.substringsBetween(route, "{", "}");
        if (Objects.isNull(pathVariableNames) || pathVariableNames.length == 0) {
            log.debug("Prepare path variables, no path variable placeholder like {...} in route {}", route);
            return Optional.empty();
        }
        Map<String, String> pathVariables = connectorExecution.getPathVariables();
        if (pathVariables.isEmpty()) {
            log.debug("Prepare path variables, no path variables found in connect execution");
            return Optional.empty();
        }
        Object[] pathVariableValues = new Object[pathVariableNames.length];
        for (int i = 0; i < pathVariableNames.length; i++) {
            String pathVariableName = pathVariableNames[i];
            String pathVariableValue = pathVariables.get(pathVariableName);
            Assert.notNull(pathVariableValue, () -> "Path variable " + pathVariableName + " can not be null");
            pathVariableValues[i] = pathVariableValue;
        }
        return Optional.of(pathVariableValues);
    }

    /**
     * Init connection execution
     *
     * @param args the method execution args
     * @return the connector execution
     */
    private ConnectorExecution initConnectionExecution(Object[] args) {
        ConnectorExecutionMetadata connectorExecutionMetadata = ConnectorExecutionMetadata.builder()
                .originalRoute(this.connectorData.getOriginalRoute())
                .parameters(this.methodSignature.getParameters())
                .parameterAnnotations(this.methodSignature.getParameterAnnotations())
                .connectorInterface(this.methodSignature.getConnectorInterface())
                .connectorMethod(this.methodSignature.connectorMethod)
                .args(args)
                .build();
        ConnectorExecutionBuilder connectorExecutionBuilder = ConnectorExecution.builder()
                .connectorExecutionMetadata(connectorExecutionMetadata)
                .route(this.connectorData.getOriginalRoute());
        this.resolvePathVariables(args).ifPresent(connectorExecutionBuilder::pathVariables);
        this.resolveBody(args).ifPresent(connectorExecutionBuilder::bodyData);
        this.resolveHeaderValues(args).ifPresent(connectorExecutionBuilder::headers);
        this.resolveQueryParams(args).ifPresent(connectorExecutionBuilder::queryParams);
        return connectorExecutionBuilder.build();
    }

    private Optional<Map<String, String>> resolvePathVariables(Object[] args) {
        String originalRoute = this.connectorData.getOriginalRoute();
        if (Objects.isNull(args) || args.length == 0) {
            return Optional.empty();
        }
        String[] pathVariableNames = RSocketMicroConnectUtil.substringsBetween(originalRoute, "{", "}");
        if (Objects.isNull(pathVariableNames) || pathVariableNames.length == 0) {
            return Optional.empty();
        }
        Annotation[][] parameterAnnotations = methodSignature.getParameterAnnotations();
        Map<String, String> pathVariables = new HashMap<>();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            Annotation[] annotations = parameterAnnotations[i];
            for (Annotation annotation : annotations) {
                if (DestinationVariable.class.equals(annotation.annotationType())
                        || PathVariable.class.equals(annotation.annotationType())) {
                    String pathValiableName = (String) AnnotationUtils.getValue(annotation);
                    if (!StringUtils.hasText(pathValiableName)) {
                        Parameter parameter = methodSignature.getParameters()[i];
                        pathValiableName = parameter.getName();
                    }
                    Object arg = args[i];
                    if (Objects.nonNull(arg)) {
                        pathVariables.put(pathValiableName, arg.toString());
                    }
                    break;
                }
            }
        }
        return Optional.of(pathVariables);
    }

    private Optional<Object> resolveBody(Object[] args) {
        if (Objects.isNull(args) || args.length == 0) {
            return Optional.empty();
        }
        Annotation[][] parameterAnnotations = methodSignature.getParameterAnnotations();
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
                    return Optional.of(argValue);
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
                    return Optional.of(argValue);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<MultiValueMap<String, String>> resolveHeaderValues(Object[] args) {
        if (Objects.isNull(args) || args.length == 0) {
            return Optional.empty();
        }
        Annotation[][] parameterAnnotations = methodSignature.getParameterAnnotations();
        Parameter[] parameters = methodSignature.getParameters();
        final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        for (int i = 0; i < parameterAnnotations.length; i++) {
            Annotation[] annotations = parameterAnnotations[i];
            for (Annotation annotation : annotations) {
                if (RequestHeader.class.equals(annotation.annotationType())) {
                    Object argValue = args[i];
                    if (Objects.nonNull(argValue)) {
                        Class<?> parameterType = parameters[i].getType();
                        if (HttpHeaders.class.equals(parameterType)) {
                            headers.addAll((HttpHeaders) argValue);
                            continue;
                        }
                        if (MultiValueMap.class.isAssignableFrom(parameterType)) {
                            MultiValueMap<?, ?> multiValueMap = (MultiValueMap<?, ?>) argValue;
                            multiValueMap.forEach((k, v) -> {
                                if (!(k instanceof String)) {
                                    return;
                                }
                                for (Object item : v) {
                                    if (!(item instanceof String)) {
                                        headers.add((String) k, String.valueOf(item));
                                    } else {
                                        headers.add((String) k, (String) item);
                                    }
                                }
                            });
                        }
                    }
                }
            }
        }
        if (headers.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(headers);
    }

    private Optional<MultiValueMap<String, String>> resolveQueryParams(Object[] args) {
        if (Objects.isNull(args) || args.length == 0) {
            return Optional.empty();
        }
        final MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
        Annotation[][] parameterAnnotations = methodSignature.getParameterAnnotations();
        Parameter[] parameters = methodSignature.getParameters();
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
    static class MethodSignature {

        private final Class<?> connectorInterface;
        private final Method connectorMethod;
        private final boolean returnsMany;
        private final boolean returnsVoid;
        private final Type returnType;

        /**
         * Instantiates a new Method signature.
         *
         * @param connectorInterface the connector interface
         * @param connectorMethod    the connector method
         */
        MethodSignature(Class<?> connectorInterface, Method connectorMethod) {
            RSocketMicroConnector rSocketMicroConnector = connectorInterface.getAnnotation(RSocketMicroConnector.class);
            if (Objects.isNull(rSocketMicroConnector)) {
                throw new IllegalArgumentException(
                        "RSocket micro connector interface should be annotated with a @RSocketMicroConnector annotation");
            }
            this.connectorInterface = connectorInterface;
            this.connectorMethod = connectorMethod;
            Class<?> methodReturnType = connectorMethod.getReturnType();
            if (void.class.equals(methodReturnType)) {
                throw new UnsupportedOperationException(
                        "Return type is void should be changed to Mono<Void> or Flux<Void>");
            }
            if (!Mono.class.equals(methodReturnType) && !Flux.class.equals(methodReturnType)) {
                throw new UnsupportedOperationException("Return type should by either Mono or Flux");
            }
            Type resolvedReturnType = resolveReturnType(connectorMethod, connectorInterface);
            if (resolvedReturnType instanceof ParameterizedType parameterizedType) {
                this.returnType = parameterizedType.getActualTypeArguments()[0];
            } else {
                this.returnType = resolvedReturnType;
            }
            this.returnsVoid = Void.class.equals(this.returnType);
            this.returnsMany = Flux.class.equals(methodReturnType);
        }

        /**
         * Get parameter annotations from connector method.
         *
         * @return the annotation[][]
         */
        Annotation[][] getParameterAnnotations() {
            return connectorMethod.getParameterAnnotations();
        }

        /**
         * Get parameters from connector method.
         *
         * @return the parameter[]
         */
        Parameter[] getParameters() {
            return connectorMethod.getParameters();
        }
    }

    @Getter
    static class ConnectorData {

        private final URI transportURI;
        private final String originalRoute;

        ConnectorData(Class<?> connectorInterface, Method connectorMethod) {
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
            this.originalRoute = extractRoute(connectorMethod);
        }

        /**
         * To extract route
         *
         * @param method the connector method
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
