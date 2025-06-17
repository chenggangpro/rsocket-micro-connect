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
package pro.chenggang.project.rsocket.micro.connect.spring.common;

import lombok.Getter;
import org.springframework.util.AntPathMatcher;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/**
 * The logging properties.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Getter
public class LoggingProperties {

    /**
     * The constant LOGGING_ATTR_KEY for saving logging flag in the RSocket exchange attributes.
     */
    public static final String LOGGING_ATTR_KEY = LoggingProperties.class.getName() + ".logging";

    /**
     * The AntPathMatcher instance.
     */
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();

    /**
     * The exclude routes.
     * This is used to match the routes with AntPathMatcher.
     */
    public Set<String> excludeRoutes = new HashSet<>();

    /**
     * The exclude headers.
     */
    public Set<String> excludeHeaders;

    /**
     * Sets exclude routes.
     *
     * @param excludeRoutes the exclude routes
     */
    public void setExcludeRoutes(Set<String> excludeRoutes) {
        if (excludeRoutes == null || excludeRoutes.isEmpty()) {
            this.excludeRoutes = Set.of();
            return;
        }
        this.excludeRoutes = new HashSet<>(excludeRoutes);
    }

    /**
     * Sets exclude headers.
     *
     * @param excludeHeaders the exclude headers
     */
    public void setExcludeHeaders(Set<String> excludeHeaders) {
        if (excludeHeaders == null || excludeHeaders.isEmpty()) {
            this.excludeHeaders = Set.of();
            return;
        }
        this.excludeHeaders = new HashSet<>(excludeHeaders.size());
        for (String excludeHeader : excludeHeaders) {
            this.excludeHeaders.add(excludeHeader.toLowerCase(Locale.getDefault()));
        }
    }

    /**
     * Determine the given route whether is a logging route.
     *
     * @param route the given route
     * @return the ture if the route is a logging route, otherwise false
     */
    public boolean isLoggingRoute(String route) {
        if (route == null || route.isEmpty()) {
            return true;
        }
        if (excludeRoutes == null || excludeRoutes.isEmpty()) {
            return true;
        }
        for (String excludeRoute : excludeRoutes) {
            if (antPathMatcher.match(excludeRoute, route)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Determine the given header whether is a logging header.
     *
     * @param header the given header
     * @return the true if the header is a logging header, otherwise false
     */
    public boolean isLoggingHeader(String header) {
        if (header == null || header.isEmpty()) {
            return true;
        }
        if (excludeHeaders == null || excludeHeaders.isEmpty()) {
            return true;
        }
        return !excludeHeaders.contains(header.toLowerCase(Locale.getDefault()));
    }
}
