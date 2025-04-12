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
package pro.chenggang.project.rsocket.micro.connect.core.defaults;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.RequiredArgsConstructor;

import java.util.Objects;

/**
 * The Remote rsocket info.
 *
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@Builder
@RequiredArgsConstructor(access = AccessLevel.PRIVATE)
public class RemoteRSocketInfo {

    public static final String ATTRIBUTE_KEY = RemoteRSocketInfo.class.getName() + ".attribute-key";

    private final String host;
    private final int port;

    public String getInfo() {
        return this.host + ":" + this.port;
    }

    public String getInfo(String path) {
        if (Objects.isNull(path) || path.isEmpty()) {
            return this.host + ":" + this.port;
        }
        return this.host + ":" + this.port + (path.startsWith("/") ? path : "/" + path);
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + "[host=" + this.host + ", port=" + this.port + "]";
    }
}
