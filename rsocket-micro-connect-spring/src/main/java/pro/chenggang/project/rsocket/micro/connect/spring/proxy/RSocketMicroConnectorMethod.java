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
import org.springframework.core.io.buffer.DataBuffer;
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
import org.springframework.web.bind.annotation.RequestPart;
import pro.chenggang.project.rsocket.micro.connect.core.util.RSocketMicroConnectUtil;
import pro.chenggang.project.rsocket.micro.connect.spring.annotation.RSocketMicroConnector;
import pro.chenggang.project.rsocket.micro.connect.spring.annotation.RequestPartName;
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
import static pro.chenggang.project.rsocket.micro.connect.spring.option.RSocketMicroConnectConstant.CONNECTOR_FILE_PART_NAME_MEDIA_TYPE;
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
    private final ResolvedParameterIndexInfo resolvedParameterIndexInfo;
    private final List<RSocketMicroConnectorExecutionCustomizer> executionCustomizers;

    public RSocketMicroConnectorMethod(Class<?> connectorInterface,
                                       Method method,
                                       List<RSocketMicroConnectorExecutionCustomizer> executionCustomizers) {
        this.methodSignature = new MethodSignature(connectorInterface, method);
        this.connectorData = new ConnectorData(connectorInterface, method);
        this.resolvedParameterIndexInfo = new ResolvedParameterIndexInfo(connectorInterface,
                method,
                connectorData.getOriginalRoute()
        );
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
    public Publisher<?> execute(RSocketRequesterRegistry rSocketRequesterRegistry, Object[] args) {
        final ConnectorExecution connectorExecution = this.initConnectionExecution(args);
        if (this.methodSignature.returnsVoid) {
            return this.executeFireAndForget(rSocketRequesterRegistry, connectorExecution);
        }
        if (this.methodSignature.returnsMany) {
            return this.executeRequestStreamOrChannel(rSocketRequesterRegistry, connectorExecution);
        }
        if (Objects.nonNull(connectorExecution.getBodyData()) && connectorExecution.getBodyData() instanceof Flux) {
            return this.executeRequestStreamOrChannel(rSocketRequesterRegistry, connectorExecution)
                    .singleOrEmpty()
                    .onErrorMap(IndexOutOfBoundsException.class,
                            ex -> new IllegalStateException("The response data count of RSocket is more than one, " +
                                    "but execution method returns Mono<...>. Execution method: " + this.methodSignature.getConnectorMethod())
                    );
        }
        return this.executeRequestResponse(rSocketRequesterRegistry, connectorExecution);
    }

    /**
     * Execute fire-and-forget by rsocket
     *
     * @param rSocketRequesterRegistry the rsocket requester registry
     * @param connectorExecution       the connector execution
     * @return {@code Publisher<Void>}
     */
    private Publisher<Void> executeFireAndForget(RSocketRequesterRegistry rSocketRequesterRegistry,
                                                 ConnectorExecution connectorExecution) {
        Mono<Void> fireAndForgetMono = this.resolveRequestSpec(rSocketRequesterRegistry, connectorExecution)
                .flatMap(RequestSpec::send);
        if (this.methodSignature.returnsMany) {
            return Flux.from(fireAndForgetMono);
        }
        return fireAndForgetMono;
    }

    /**
     * Execute request-stream by rsocket
     *
     * @param rSocketRequesterRegistry the rsocket requester registry
     * @param connectorExecution       the connector execution
     * @return {@code Flux<R>}
     */
    private <R> Flux<R> executeRequestStreamOrChannel(RSocketRequesterRegistry rSocketRequesterRegistry,
                                                      ConnectorExecution connectorExecution) {
        return this.resolveRequestSpec(rSocketRequesterRegistry, connectorExecution)
                .flatMapMany(requestSpec -> {
                    return requestSpec.retrieveFlux(new ParameterizedTypeReference<>() {
                        @Override
                        public Type getType() {
                            return methodSignature.getReturnType();
                        }
                    });
                });
    }

    /**
     * Execute request-response by rsocket
     *
     * @param rSocketRequesterRegistry the rsocket requester registry
     * @param connectorExecution       the connector execution
     * @return {@code Mono<R>}
     */
    private <R> Mono<R> executeRequestResponse(RSocketRequesterRegistry rSocketRequesterRegistry,
                                               ConnectorExecution connectorExecution) {
        return this.resolveRequestSpec(rSocketRequesterRegistry, connectorExecution)
                .flatMap(requestSpec -> {
                    return requestSpec.retrieveMono(new ParameterizedTypeReference<>() {
                        @Override
                        public Type getType() {
                            return methodSignature.getReturnType();
                        }
                    });
                });
    }

    /**
     * Resolve request spec
     *
     * @param rSocketRequesterRegistry the rsocket requester registry
     * @param connectorExecution       the connector execution
     * @return the prepared request spec
     */
    private Mono<RequestSpec> resolveRequestSpec(RSocketRequesterRegistry rSocketRequesterRegistry,
                                                 ConnectorExecution connectorExecution) {
        return Mono.just(connectorExecution)
                .flatMap(execution -> Flux.fromIterable(executionCustomizers)
                        .concatMap(executionCustomizer -> executionCustomizer.customize(execution))
                        .then(Mono.defer(() -> Mono.just(execution)))
                )
                .flatMap(execution -> Mono.fromCallable(() -> {
                    RSocketRequester rSocketRequester = rSocketRequesterRegistry.getRSocketRequester(connectorData.getTransportURI());
                    return this.resolveRequestSpec(rSocketRequester, execution);
                }));
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
        String requestPartName = connectorExecution.getRequestPartName();
        if (Objects.nonNull(requestPartName)) {
            requestSpec.metadata(metadataSpec -> {
                metadataSpec.metadata(requestPartName,
                        MimeTypeUtils.parseMimeType(CONNECTOR_FILE_PART_NAME_MEDIA_TYPE.toString())
                );
            });
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
            return Optional.empty();
        }
        Map<String, String> pathVariables = connectorExecution.getPathVariables();
        if (pathVariables.isEmpty()) {
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
        this.resolveRequestPartName(args).ifPresent(connectorExecutionBuilder::requestPartName);
        this.resolveHeaderValues(args).ifPresent(connectorExecutionBuilder::headers);
        this.resolveQueryParams(args).ifPresent(connectorExecutionBuilder::queryParams);
        return connectorExecutionBuilder.build();
    }

    private Optional<Map<String, String>> resolvePathVariables(Object[] args) {
        if (Objects.isNull(args) || args.length == 0) {
            return Optional.empty();
        }
        Map<String, String> pathVariables = new HashMap<>();
        this.resolvedParameterIndexInfo.getPathVariableIndex()
                .forEach((pathVariableName, valueIndex) -> {
                    Object arg = args[valueIndex];
                    if (Objects.nonNull(arg)) {
                        pathVariables.put(pathVariableName, arg.toString());
                    }
                });
        if (pathVariables.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(pathVariables);
    }

    private Optional<Object> resolveBody(Object[] args) {
        if (Objects.isNull(args) || args.length == 0) {
            return Optional.empty();
        }
        Integer bodyIndex = this.resolvedParameterIndexInfo.getBodyIndex();
        if (Objects.isNull(bodyIndex)) {
            return Optional.empty();
        }
        Object argValue = args[bodyIndex];
        return Optional.ofNullable(argValue);
    }

    private Optional<MultiValueMap<String, String>> resolveHeaderValues(Object[] args) {
        if (Objects.isNull(args) || args.length == 0) {
            return Optional.empty();
        }
        ResolvedHeaderIndex headerIndex = this.resolvedParameterIndexInfo.getHeaderIndex();
        Integer singleHeaderIndex = headerIndex.singleHeaders();
        MultiValueMap<String, Integer> namedHeaderIndex = headerIndex.namedHeader();
        MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
        if (Objects.nonNull(singleHeaderIndex)) {
            Object arg = args[singleHeaderIndex];
            if (Objects.nonNull(arg)) {
                if (arg instanceof HttpHeaders argValue) {
                    headers.addAll(argValue);
                } else if (arg instanceof MultiValueMap<?, ?> argValue) {
                    argValue.forEach((k, v) -> {
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
        namedHeaderIndex.forEach((name, indexList) -> {
            indexList.forEach(index -> {
                Object argValue = args[index];
                if (Objects.nonNull(argValue)) {
                    headers.add(name, argValue.toString());
                }
            });
        });
        if (headers.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(headers);
    }

    private Optional<MultiValueMap<String, String>> resolveQueryParams(Object[] args) {
        if (Objects.isNull(args) || args.length == 0) {
            return Optional.empty();
        }
        MultiValueMap<String, String> multiValueMap = new LinkedMultiValueMap<>();
        this.resolvedParameterIndexInfo.getQueryParamIndex()
                .forEach((name, indexList) -> {
                    indexList.forEach(index -> {
                        Object argValue = args[index];
                        if (Objects.nonNull(argValue)) {
                            multiValueMap.add(name, argValue.toString());
                        }
                    });
                });
        if (multiValueMap.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(multiValueMap);
    }

    private Optional<String> resolveRequestPartName(Object[] args) {
        if (Objects.isNull(args) || args.length == 0) {
            return Optional.empty();
        }
        Integer partNameIndex = this.resolvedParameterIndexInfo.getPartNameIndex();
        if (Objects.nonNull(partNameIndex)) {
            Object argValue = args[partNameIndex];
            if (Objects.nonNull(argValue)) {
                return Optional.of(argValue.toString());
            }
        }
        return Optional.empty();
    }

    @Getter
    static class ResolvedParameterIndexInfo {

        private final Map<String, Integer> pathVariableIndex;
        private final Integer bodyIndex;
        private final ResolvedHeaderIndex headerIndex;
        private final MultiValueMap<String, Integer> queryParamIndex;
        private final Integer partNameIndex;

        ResolvedParameterIndexInfo(Class<?> connectorInterface, Method connectorMethod, String originalRoute) {
            this.pathVariableIndex = this.resolvePathVariableIndex(connectorMethod, originalRoute);
            this.bodyIndex = this.resolveBodyIndex(connectorInterface, connectorMethod);
            this.headerIndex = this.resolveHeaderIndex(connectorMethod);
            this.queryParamIndex = this.resolveQueryParamIndex(connectorMethod);
            this.partNameIndex = this.resolveRequestPartNameIndex(connectorMethod);
        }

        private Map<String, Integer> resolvePathVariableIndex(Method connectorMethod, String originalRoute) {
            String[] pathVariableNames = RSocketMicroConnectUtil.substringsBetween(originalRoute, "{", "}");
            if (Objects.isNull(pathVariableNames) || pathVariableNames.length == 0) {
                return Collections.emptyMap();
            }
            if (connectorMethod.getParameterCount() == 0) {
                return Collections.emptyMap();
            }
            Annotation[][] parameterAnnotations = connectorMethod.getParameterAnnotations();
            Map<String, Integer> pathVariables = new HashMap<>();
            for (int i = 0; i < parameterAnnotations.length; i++) {
                Annotation[] annotations = parameterAnnotations[i];
                for (Annotation annotation : annotations) {
                    if (DestinationVariable.class.equals(annotation.annotationType())
                            || PathVariable.class.equals(annotation.annotationType())) {
                        String pathValiableName = (String) AnnotationUtils.getValue(annotation);
                        if (!StringUtils.hasText(pathValiableName)) {
                            Parameter parameter = connectorMethod.getParameters()[i];
                            pathValiableName = parameter.getName();
                        }
                        pathVariables.put(pathValiableName, i);
                        break;
                    }
                }
            }
            return pathVariables;
        }

        private Integer resolveBodyIndex(Class<?> connectorInterface, Method connectorMethod) {
            if (connectorMethod.getParameterCount() == 0) {
                return null;
            }
            Parameter[] parameters = connectorMethod.getParameters();
            Annotation[][] parameterAnnotations = connectorMethod.getParameterAnnotations();
            for (int i = 0; i < parameterAnnotations.length; i++) {
                Annotation[] annotations = parameterAnnotations[i];
                boolean isRequestBodyAnnotationMatched = false;
                boolean isRequestPartAnnotationMatched = false;
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
                    if (RequestPart.class.equals(annotation.annotationType())) {
                        isRequestPartAnnotationMatched = true;
                        break;
                    }
                }
                if (isRequestBodyAnnotationMatched || isRequestPartAnnotationMatched) {
                    if (isRequestPartAnnotationMatched) {
                        Class<?> inferredType = RSocketMicroConnectUtil.parseInferredClass(parameters[i].getParameterizedType());
                        if (!DataBuffer.class.isAssignableFrom(inferredType)) {
                            throw new IllegalArgumentException(
                                    "@RequestPart only support Flux<org.springframework.core.io.buffer.DataBuffer> type parameter, " +
                                            "please check method " + connectorMethod + " of interface " + connectorInterface);
                        }
                    }
                    // The first @RequestBody or @RequestPart arg
                    return i;
                }
            }
            for (int i = 0; i < parameterAnnotations.length; i++) {
                Annotation[] annotations = parameterAnnotations[i];
                boolean isAnyResolvedAnnotationMatched = false;
                for (Annotation annotation : annotations) {
                    if (PathVariable.class.equals(annotation.annotationType())
                            || DestinationVariable.class.equals(annotation.annotationType())
                            || RequestParam.class.equals(annotation.annotationType())
                            || RequestHeader.class.equals(annotation.annotationType())
                            || RequestPartName.class.equals(annotation.annotationType())) {
                        isAnyResolvedAnnotationMatched = true;
                        break;
                    }
                }
                if (!isAnyResolvedAnnotationMatched) {
                    // The first no-resolved annotated arg
                    return i;
                }
            }
            return null;
        }


        private ResolvedHeaderIndex resolveHeaderIndex(Method connectorMethod) {
            MultiValueMap<String, Integer> namedHeaderIndex = new LinkedMultiValueMap<>();
            Integer singleHeaderIndex = null;
            if (connectorMethod.getParameterCount() == 0) {
                return new ResolvedHeaderIndex(singleHeaderIndex, namedHeaderIndex);
            }
            Annotation[][] parameterAnnotations = connectorMethod.getParameterAnnotations();
            Parameter[] parameters = connectorMethod.getParameters();
            for (int i = 0; i < parameterAnnotations.length; i++) {
                Annotation[] annotations = parameterAnnotations[i];
                for (Annotation annotation : annotations) {
                    if (RequestHeader.class.equals(annotation.annotationType())) {
                        Class<?> parameterType = parameters[i].getType();
                        if (HttpHeaders.class.equals(parameterType) && Objects.isNull(singleHeaderIndex)) {
                            singleHeaderIndex = i;
                            continue;
                        }
                        if (MultiValueMap.class.isAssignableFrom(parameterType) && Objects.isNull(singleHeaderIndex)) {
                            singleHeaderIndex = i;
                            continue;
                        }
                        RequestHeader requestHeader = parameters[i].getAnnotation(RequestHeader.class);
                        if (StringUtils.hasText(requestHeader.name())) {
                            namedHeaderIndex.add(requestHeader.name(), i);
                        }
                    }
                }
            }
            return new ResolvedHeaderIndex(singleHeaderIndex, namedHeaderIndex);
        }

        private MultiValueMap<String, Integer> resolveQueryParamIndex(Method connectorMethod) {
            MultiValueMap<String, Integer> queryParamIndex = new LinkedMultiValueMap<>();
            if (connectorMethod.getParameterCount() == 0) {
                return queryParamIndex;
            }
            Annotation[][] parameterAnnotations = connectorMethod.getParameterAnnotations();
            Parameter[] parameters = connectorMethod.getParameters();
            for (int i = 0; i < parameterAnnotations.length; i++) {
                Annotation[] annotations = parameterAnnotations[i];
                for (Annotation annotation : annotations) {
                    if (RequestParam.class.equals(annotation.annotationType())) {
                        String requestParamName = (String) AnnotationUtils.getValue(annotation);
                        if (!StringUtils.hasText(requestParamName)) {
                            Parameter parameter = parameters[i];
                            requestParamName = parameter.getName();
                        }
                        queryParamIndex.add(requestParamName, i);
                    }
                }
            }
            return queryParamIndex;
        }

        private Integer resolveRequestPartNameIndex(Method connectorMethod) {
            if (connectorMethod.getParameterCount() == 0) {
                return null;
            }
            Annotation[][] parameterAnnotations = connectorMethod.getParameterAnnotations();
            Parameter[] parameters = connectorMethod.getParameters();
            for (int i = 0; i < parameterAnnotations.length; i++) {
                Annotation[] annotations = parameterAnnotations[i];
                for (Annotation annotation : annotations) {
                    if (RequestPartName.class.equals(annotation.annotationType())) {
                        if (String.class.equals(parameters[i].getType())) {
                            return i;
                        } else {
                            throw new IllegalArgumentException("@RequestPartName should be annotated on a String type value, " +
                                    "please check parameter " + parameters[i] + " in method " + connectorMethod);
                        }
                    }
                }
            }
            return null;
        }

    }

    private record ResolvedHeaderIndex(Integer singleHeaders, MultiValueMap<String, Integer> namedHeader) {
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
                throw new UnsupportedOperationException("Return type is void should be changed to Mono<Void> or Flux<Void>");
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
