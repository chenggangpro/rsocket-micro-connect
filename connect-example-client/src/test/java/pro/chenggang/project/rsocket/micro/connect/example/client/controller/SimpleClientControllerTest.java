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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.util.UriBuilder;
import pro.chenggang.project.rsocket.micro.connect.example.client.ConnectExampleClientApiApplicationTests;
import pro.chenggang.project.rsocket.micro.connect.example.client.dto.BodyValue;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Gang Cheng
 * @version 0.1.0
 * @since 0.1.0
 */
class SimpleClientControllerTest extends ConnectExampleClientApiApplicationTests {

    @Autowired
    WebTestClient webTestClient;

    @Autowired
    ObjectMapper objectMapper;

    UriBuilder getDefaultUri(UriBuilder uriBuilder) {
        return uriBuilder
                .scheme("http")
                .host("127.0.0.1")
                .port(9092);
    }

    @Test
    void getDataWithPathVariable() {
        String pathVariable = "this-is-path-variable-data";
        this.webTestClient.get()
                .uri(uriBuilder -> getDefaultUri(uriBuilder)
                        .path("/client/data/path-variable/" + pathVariable)
                        .build()
                )
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.TEXT_PLAIN)
                .exchange()
                .expectBody(String.class)
                .value(result -> assertEquals(pathVariable, result));
    }

    @Test
    void getDataWithQueryParam() {
        this.webTestClient.get()
                .uri(uriBuilder -> getDefaultUri(uriBuilder)
                        .path("/client/data/query-variable")
                        .queryParam("var1", "123")
                        .queryParam("var2", "456")
                        .build()
                )
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.TEXT_PLAIN)
                .exchange()
                .expectBody(String.class)
                .value(result -> assertEquals("123,456", result));
    }

    @Test
    void postDataWithBody() {
        BodyValue bodyValue = BodyValue.builder()
                .name("name")
                .value("value")
                .build();
        this.webTestClient.post()
                .uri(uriBuilder -> getDefaultUri(uriBuilder)
                        .path("/client/data/body")
                        .build()
                )
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_PLAIN)
                .bodyValue(bodyValue)
                .exchange()
                .expectBody(String.class)
                .value(result -> {
                    try {
                        assertEquals(objectMapper.writeValueAsString(bodyValue), result);
                    } catch (JsonProcessingException e) {
                        throw new RuntimeException(e);
                    }
                });
    }

    @Test
    void getDataWithHeader() {
        Set<String> testHeaderKeys = new LinkedHashSet<>();
        testHeaderKeys.add("accept-encoding");
        testHeaderKeys.add("user-agent");
        testHeaderKeys.add("host");
        testHeaderKeys.add("WebTestClient-Request-Id");
        testHeaderKeys.add("Content-Type");
        testHeaderKeys.add("Accept");
        this.webTestClient.get()
                .uri(uriBuilder -> getDefaultUri(uriBuilder)
                        .path("/client/data/header")
                        .build()
                )
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.TEXT_PLAIN)
                .exchange()
                .expectBody(String.class)
                .value(result -> assertEquals(String.join(",", testHeaderKeys), result));
    }

    @Test
    void getDataWithAutoHeader() {
        Set<String> testHeaderKeys = new LinkedHashSet<>();
        testHeaderKeys.add("accept-encoding");
        testHeaderKeys.add("user-agent");
        testHeaderKeys.add("host");
        testHeaderKeys.add("WebTestClient-Request-Id");
        testHeaderKeys.add("Content-Type");
        testHeaderKeys.add("Accept");
        this.webTestClient.get()
                .uri(uriBuilder -> getDefaultUri(uriBuilder)
                        .path("/client/data/auto-header")
                        .build()
                )
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.TEXT_PLAIN)
                .exchange()
                .expectBody(String.class)
                .value(result -> assertEquals(String.join(",", testHeaderKeys), result));
    }

    @Test
    void getDataWithExtraHeader() {
        this.webTestClient.get()
                .uri(uriBuilder -> getDefaultUri(uriBuilder)
                        .path("/client/data/extra-header")
                        .build()
                )
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .accept(MediaType.TEXT_PLAIN)
                .exchange()
                .expectBody(String.class)
                .value(result -> assertEquals("x-extra-header-value", result));
    }

    @Test
    void postDataWithUploadFile() throws IOException {
        MultipartBodyBuilder builder = new MultipartBodyBuilder();
        ClassPathResource classPathResource = new ClassPathResource("application.yml");
        long length = classPathResource.getFile().length();
        builder.part("file", classPathResource);
        this.webTestClient.post()
                .uri(uriBuilder -> getDefaultUri(uriBuilder)
                        .path("/client/data/file-upload")
                        .build()
                )
                .contentType(MediaType.MULTIPART_FORM_DATA)
                .accept(MediaType.TEXT_PLAIN)
                .body(BodyInserters.fromMultipartData(builder.build()))
                .exchange()
                .expectBody(String.class)
                .value(result -> assertEquals("application.yml:" + length, result));
    }
}