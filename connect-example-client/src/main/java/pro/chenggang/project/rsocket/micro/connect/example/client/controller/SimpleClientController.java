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
package pro.chenggang.project.rsocket.micro.connect.example.client.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import pro.chenggang.project.rsocket.micro.connect.example.client.connector.SimpleRSocketMicroConnector;
import pro.chenggang.project.rsocket.micro.connect.example.client.dto.BodyValue;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.stream.Collectors;

/**
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@RestController
@RequiredArgsConstructor
public class SimpleClientController {

    private final SimpleRSocketMicroConnector simpleRSocketMicroConnector;

    @GetMapping("/client/data/path-variable/{variable}")
    public Mono<String> getDataWithPathVariable(@PathVariable("variable") String variable) {
        return simpleRSocketMicroConnector.getSingleData(variable);
    }

    @GetMapping("/client/data/query-variable")
    public Mono<String> getDataWithQueryParam(@RequestParam("var1") LocalDate variable1, @RequestParam("var2") Integer variable2) {
        return simpleRSocketMicroConnector.getMultiData(variable1, variable2);
    }

    @PostMapping("/client/data/body")
    public Mono<String> postDataWithBody(@RequestBody BodyValue bodyValue) {
        return simpleRSocketMicroConnector.sendBodyData(bodyValue);
    }

    @GetMapping("/client/data/header")
    public Mono<String> getDataWithHeader(@RequestHeader HttpHeaders headers) {
        return simpleRSocketMicroConnector.getStreamData(headers)
                .collect(Collectors.joining(","));
    }

    @GetMapping("/client/data/auto-header")
    public Mono<String> getDataWithAutoHeader() {
        return simpleRSocketMicroConnector.getStreamDataAutoHeader()
                .collect(Collectors.joining(","));
    }

    @GetMapping("/client/data/extra-header")
    public Mono<String> getDataWithExtraHeader() {
        return simpleRSocketMicroConnector.getExtraHeader("x-extra-header-value");
    }

    @GetMapping("/client/data/no-logging")
    public Mono<String> getDataWithNoLogging() {
        return simpleRSocketMicroConnector.noLogging();
    }

    @GetMapping("/client/data/no-logging-header")
    public Mono<String> getDataWithNoLoggingHeader() {
        return simpleRSocketMicroConnector.noLoggingHeader("x-extra-header-value");
    }

    @PostMapping(value = "/client/data/file-upload")
    public Mono<String> postDataWithUploadFile(@RequestPart("file") FilePart filePart) {
        return simpleRSocketMicroConnector.uploadFile(
                filePart.filename(),
                filePart.content()
        );
    }

}
