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

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * The Connector execution.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public final class ConnectorExecution {

    @Getter
    private final ConnectorExecutionMetadata connectorExecutionMetadata;
    @Getter
    private String route;
    @Getter
    private Object bodyData;
    @Getter
    private String requestPartName;
    private final Map<String, String> pathVariables = new HashMap<>();
    private final MultiValueMap<String, String> headers = new LinkedMultiValueMap<>();
    private final MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();

    @Builder(access = AccessLevel.PACKAGE)
    private ConnectorExecution(@NonNull ConnectorExecutionMetadata connectorExecutionMetadata,
                               @NonNull String route,
                               Object bodyData,
                               String requestPartName,
                               Map<String, String> pathVariables,
                               MultiValueMap<String, String> headers,
                               MultiValueMap<String, String> queryParams) {
        this.connectorExecutionMetadata = connectorExecutionMetadata;
        this.route = route;
        this.bodyData = bodyData;
        this.requestPartName = requestPartName;
        if (Objects.nonNull(pathVariables) && !pathVariables.isEmpty()) {
            this.pathVariables.putAll(pathVariables);
        }
        if (Objects.nonNull(headers) && !headers.isEmpty()) {
            this.headers.putAll(headers);
        }
        if (Objects.nonNull(queryParams) && !queryParams.isEmpty()) {
            this.queryParams.putAll(queryParams);
        }
    }

    /**
     * Create a new ConnectionExecution from connector execution.
     *
     * @param connectorExecution the connector execution
     * @return the new connector execution
     */
    public static ConnectorExecution from(@NonNull ConnectorExecution connectorExecution) {
        return new ConnectorExecution(connectorExecution.getConnectorExecutionMetadata(),
                connectorExecution.getRoute(),
                connectorExecution.getBodyData(),
                connectorExecution.getRequestPartName(),
                connectorExecution.getPathVariables(),
                connectorExecution.getHeaders(),
                connectorExecution.getQueryParams()
        );
    }

    /**
     * Gets path variables.
     *
     * @return the unmodifiable path variables
     */
    public Map<String, String> getPathVariables() {
        return Collections.unmodifiableMap(this.pathVariables);
    }

    /**
     * Gets headers.
     *
     * @return the unmodifiable headers
     */
    public MultiValueMap<String, String> getHeaders() {
        return CollectionUtils.unmodifiableMultiValueMap(headers);
    }

    /**
     * Gets query params.
     *
     * @return the unmodifiable query params
     */
    public MultiValueMap<String, String> getQueryParams() {
        return CollectionUtils.unmodifiableMultiValueMap(queryParams);
    }

    /**
     * Configure connector route.
     *
     * @param route the route can not be null or empty
     * @return the connector execution
     */
    public ConnectorExecution route(@NonNull String route) {
        Assert.hasText(route, () -> "RSocket connector route can not be ");
        this.route = route;
        return this;
    }

    /**
     * Configure body data.
     *
     * @param body the body can be null
     * @return the connector execution
     */
    public ConnectorExecution bodyData(Object body) {
        this.bodyData = body;
        return this;
    }

    /**
     * Configure request part name.
     *
     * @param requestPartName the request part name
     * @return the connector execution
     */
    public ConnectorExecution requestPartName(@NonNull String requestPartName) {
        this.requestPartName = requestPartName;
        return this;
    }

    /**
     * Add path variable.
     *
     * @param name  the path variable name can not be null
     * @param value the path variable value can not be null
     * @return the connector execution
     */
    public ConnectorExecution addPathVariable(@NonNull String name, @NonNull String value) {
        this.pathVariables.put(name, value);
        return this;
    }

    /**
     * Add path variables.
     *
     * @param pathVariables the path variables can not be null
     * @return the connector execution
     */
    public ConnectorExecution addPathVariables(@NonNull Map<String, String> pathVariables) {
        if (!pathVariables.isEmpty()) {
            this.pathVariables.putAll(pathVariables);
        }
        return this;
    }

    /**
     * Configure to new path variables. This will clear old par variables.
     *
     * @param pathVariables the new path variables can not be null
     * @return the connector execution
     */
    public ConnectorExecution newPathVariables(@NonNull Map<String, String> pathVariables) {
        Assert.notEmpty(pathVariables, () -> "New path variables can not be empty");
        this.pathVariables.clear();
        this.pathVariables.putAll(pathVariables);
        return this;
    }

    /**
     * Remove path variable.
     *
     * @param pathVariableName the path variable name can not be null
     * @return the connector execution
     */
    public ConnectorExecution removePathVariable(@NonNull String pathVariableName) {
        this.pathVariables.remove(pathVariableName);
        return this;
    }

    /**
     * Add header.
     *
     * @param key   the header key can not be null
     * @param value the header value can not be null
     * @return the connector execution
     */
    public ConnectorExecution addHeader(@NonNull String key, @NonNull String value) {
        this.headers.add(key, value);
        return this;
    }

    /**
     * Add headers.
     *
     * @param key    the header key can not be null
     * @param values the header values can not be null
     * @return the connector execution
     */
    public ConnectorExecution addHeaders(@NonNull String key, @NonNull List<String> values) {
        if (!values.isEmpty()) {
            this.headers.put(key, values);
        }
        return this;
    }

    /**
     * Add headers.
     *
     * @param headers the headers can not be null
     * @return the connector execution
     */
    public ConnectorExecution addHeaders(@NonNull MultiValueMap<String, String> headers) {
        if (!headers.isEmpty()) {
            this.headers.putAll(headers);
        }
        return this;
    }

    /**
     * Configure new headers.
     *
     * @param headers the headers can not be null
     * @return the connector execution
     */
    public ConnectorExecution newHeaders(@NonNull MultiValueMap<String, String> headers) {
        Assert.notEmpty(headers, () -> "New headers can not be empty");
        this.headers.clear();
        this.headers.putAll(headers);
        return this;
    }

    /**
     * Remove header.
     *
     * @param headerName the header name can not be null
     * @return the connector execution
     */
    public ConnectorExecution removeHeader(@NonNull String headerName) {
        this.headers.remove(headerName);
        return this;
    }

    /**
     * Configure query param.
     *
     * @param key   the key can not be null
     * @param value the value can not be null
     * @return the connector execution
     */
    public ConnectorExecution addQueryParam(@NonNull String key, @NonNull String value) {
        this.queryParams.add(key, value);
        return this;
    }

    /**
     * Add query params.
     *
     * @param key    the key can not be null
     * @param values the values can not be null
     * @return the connector execution
     */
    public ConnectorExecution addQueryParams(@NonNull String key, @NonNull List<String> values) {
        if (!values.isEmpty()) {
            this.queryParams.put(key, values);
        }
        return this;
    }

    /**
     * Add query params.
     *
     * @param queryParams the query params can not be null
     * @return the connector execution
     */
    public ConnectorExecution addQueryParams(@NonNull MultiValueMap<String, String> queryParams) {
        if (!headers.isEmpty()) {
            this.queryParams.putAll(headers);
        }
        return this;
    }

    /**
     * Configure new query params. This will clear old query params.
     *
     * @param queryParams the query params can not be null
     * @return the connector execution
     */
    public ConnectorExecution newQueryParams(@NonNull MultiValueMap<String, String> queryParams) {
        Assert.notEmpty(queryParams, () -> "New headers can not be empty");
        this.queryParams.clear();
        this.queryParams.putAll(queryParams);
        return this;
    }

    /**
     * Remove query params.
     *
     * @param queryName the query name can not be null
     * @return the connector execution
     */
    public ConnectorExecution removeQueryParams(@NonNull String queryName) {
        this.queryParams.remove(queryName);
        return this;
    }

}
