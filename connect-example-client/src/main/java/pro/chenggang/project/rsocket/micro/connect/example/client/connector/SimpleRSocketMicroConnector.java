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
package pro.chenggang.project.rsocket.micro.connect.example.client.connector;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import pro.chenggang.project.rsocket.micro.connect.example.client.dto.BodyValue;
import pro.chenggang.project.rsocket.micro.connect.spring.annotation.RSocketMicroConnector;
import pro.chenggang.project.rsocket.micro.connect.spring.annotation.RequestPartName;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@RSocketMicroConnector("tcp://127.0.0.1:23408")
public interface SimpleRSocketMicroConnector {

    @MessageMapping("/server/data/path-variable/{variable}")
    Mono<String> getSingleData(@PathVariable("variable") String variable);

    @RequestMapping("/server/data/query-variable")
    Mono<String> getMultiData(@RequestParam("var1") String variable1, @RequestParam("var2") String variable2);

    @MessageMapping("/server/data/body")
    Mono<String> sendBodyData(@RequestBody BodyValue bodyValue);

    @MessageMapping("/server/data/stream")
    Flux<String> getStreamData(@RequestHeader HttpHeaders headers);

    @MessageMapping("/server/data/stream/auto-header")
    Flux<String> getStreamDataAutoHeader();

    @MessageMapping("/server/data/extra-header")
    Mono<String> getExtraHeader(@RequestHeader(name = "x-extra-header") String extraHeader);

    @MessageMapping("/server/data/no-logging")
    Mono<String> noLogging();

    @MessageMapping("/server/data/no-logging-header")
    Mono<String> noLoggingHeader(@RequestHeader(name = "x-extra-header") String extraHeader);

    @RequestMapping("/server/data/file-upload")
    Mono<String> uploadFile(@RequestPartName String name, Flux<DataBuffer> dataBufferFlux);

}
