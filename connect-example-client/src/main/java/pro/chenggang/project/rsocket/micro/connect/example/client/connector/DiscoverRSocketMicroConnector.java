package pro.chenggang.project.rsocket.micro.connect.example.client.connector;

import org.springframework.context.annotation.Profile;
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
@Profile("discover")
@RSocketMicroConnector("tcp://rsocket-server")
public interface DiscoverRSocketMicroConnector {

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

    @RequestMapping("/server/data/file-upload")
    Mono<String> uploadFile(@RequestPartName String name, Flux<DataBuffer> dataBufferFlux);
}
