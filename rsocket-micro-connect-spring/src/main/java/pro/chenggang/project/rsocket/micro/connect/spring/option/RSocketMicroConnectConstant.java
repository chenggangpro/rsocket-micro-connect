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
package pro.chenggang.project.rsocket.micro.connect.spring.option;

import org.springframework.http.MediaType;

/**
 * The rSocket micro connect constant.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
public abstract class RSocketMicroConnectConstant {

    public static final MediaType CONNECTOR_HEADER_MEDIA_TYPE = new MediaType("application", "connector-header");

    public static final MediaType CONNECTOR_QUERY_MEDIA_TYPE = new MediaType("application", "connector-query");

    public static final String CONNECTOR_HEADER_METADATA_KEY = "connector-header";

    public static final String CONNECTOR_QUERY_METADATA_KEY = "connector-query";

    public static final String DISCOVER_ENABLE_RSOCKET_METADATA_KEY = "rsocket-micro-connect.enable";

    public static final String DISCOVER_RSOCKET_PORT_METADATA_KEY = "rsocket-micro-connect.port";
}
