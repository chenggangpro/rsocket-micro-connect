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
package pro.chenggang.project.rsocket.micro.connect.example.server.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import pro.chenggang.project.rsocket.micro.connect.example.server.dto.BodyValue;
import pro.chenggang.project.rsocket.micro.connect.spring.annotation.RSocketMicroEndpoint;
import pro.chenggang.project.rsocket.micro.connect.spring.annotation.RequestPartName;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.File;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Map.Entry;

/**
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
@RestController
@RSocketMicroEndpoint
@RequiredArgsConstructor
public class ServerController {

    private final ObjectMapper objectMapper;

    @MessageMapping
    @GetMapping("/server/data/path-variable/{variable}")
    public Mono<String> getData(@PathVariable("variable") String variable) {
        return Mono.just(variable);
    }

    @MessageMapping
    @GetMapping("/server/data/query-variable")
    public Mono<String> getData(@RequestParam("var1") String variable1, @RequestParam("var2") String variable2) {
        return Mono.just(variable1 + "," + variable2);
    }

    @MessageMapping
    @PostMapping("/server/data/body")
    public Mono<String> getData(@RequestBody BodyValue bodyValue) {
        return Mono.fromCallable(() -> objectMapper.writeValueAsString(bodyValue));
    }

    @MessageMapping
    @GetMapping("/server/data/stream")
    public Flux<String> getStreamData(@RequestHeader HttpHeaders headers) {
        return Flux.fromIterable(headers.entrySet())
                .map(Entry::getKey);
    }

    @MessageMapping("/server/data/stream/auto-header")
    public Flux<String> getStreamDataAutoHeader(@RequestHeader HttpHeaders headers) {
        return Flux.fromIterable(headers.entrySet())
                .map(Entry::getKey);
    }

    @MessageMapping("/server/data/extra-header")
    public Mono<String> getExtraHeader(@RequestHeader(name = "x-extra-header") String extraHeader) {
        return Mono.just(extraHeader);
    }

    @MessageMapping
    @PostMapping("/server/data/file-upload")
    public Flux<String> getData(@RequestPartName String partName, Flux<DataBuffer> dataBufferFlux) {
        String userDir = System.getProperty("user.dir");
        String savedFilePath = userDir + File.separator + partName;
        return DataBufferUtils.write(dataBufferFlux, Paths.get(savedFilePath), StandardOpenOption.CREATE_NEW)
                .thenMany(Mono.defer(() -> {
                    return Mono.fromCallable(() -> {
                                File savedFile = new File(savedFilePath);
                                long length = savedFile.length();
                                savedFile.deleteOnExit();
                                return length;
                            })
                            .map(length -> partName + ":" + length);
                }));
    }

}
