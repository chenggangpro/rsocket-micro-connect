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
package pro.chenggang.project.rsocket.micro.connect.spring.client;

import io.rsocket.metadata.WellKnownMimeType;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.NestedConfigurationProperty;
import pro.chenggang.project.rsocket.micro.connect.spring.common.LoggingProperties;

import java.time.Duration;
import java.util.List;

/**
 * The RSocket micro connect server properties.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Getter
@Setter
public class RSocketMicroConnectClientProperties {

    /**
     * The properties PROPERTIES_PREFIX.
     */
    public static final String PROPERTIES_PREFIX = "rsocket-micro-connect.client";

    /**
     * Whether the rsocket micro connect client is enabled.
     * Default value is true.
     */
    public boolean enabled = true;

    /**
     * The Logging properties.
     */
    @NestedConfigurationProperty
    public LoggingProperties logging;

    /**
     * The Enable discover.
     */
    public boolean enableDiscover = false;

    /**
     * The Micro connector package.
     * If there are other packages need to be scanned, this should be configured
     */
    public List<String> microConnectorPackage;

    /**
     * The Refresh discover interval.
     */
    public Duration refreshDiscoverInterval = Duration.ofSeconds(10);

    /**
     * The Default data mime type.
     */
    public WellKnownMimeType defaultDataMimeType = WellKnownMimeType.APPLICATION_CBOR;
    /**
     * The Default metadata mime type.
     */
    public WellKnownMimeType defaultMetadataMimeType = WellKnownMimeType.MESSAGE_RSOCKET_COMPOSITE_METADATA;
}
