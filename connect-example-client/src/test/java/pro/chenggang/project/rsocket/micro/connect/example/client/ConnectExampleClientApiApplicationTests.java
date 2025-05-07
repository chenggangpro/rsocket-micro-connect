package pro.chenggang.project.rsocket.micro.connect.example.client;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.web.filter.reactive.ServerWebExchangeContextFilter;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.DEFINED_PORT)
@Import(ServerWebExchangeContextFilter.class)
public class ConnectExampleClientApiApplicationTests {

}
