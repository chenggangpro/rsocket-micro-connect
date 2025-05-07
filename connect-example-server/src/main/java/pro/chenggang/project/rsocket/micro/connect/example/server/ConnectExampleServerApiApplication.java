package pro.chenggang.project.rsocket.micro.connect.example.server;

import lombok.Generated;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import reactor.core.publisher.Hooks;

@Generated
@Slf4j
@SpringBootApplication
public class ConnectExampleServerApiApplication {

    public static void main(String[] args) {
        if (log.isDebugEnabled()) {
            Hooks.onOperatorDebug();
        }
        Hooks.enableAutomaticContextPropagation();
        SpringApplication.run(ConnectExampleServerApiApplication.class, args);
    }

}
