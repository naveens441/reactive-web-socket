package reactive.websocket;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.stream.Stream;

@SpringBootApplication
public class WebSocketApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebSocketApplication.class, args);
    }

}

@Log4j2
@Configuration
class GreetingWebSocketConfiguration {
    @Bean
    SimpleUrlHandlerMapping simpleUrlHandlerMapping(WebSocketHandler webSocketHandler) {
        return new SimpleUrlHandlerMapping(Map.of("/ws/greeting", webSocketHandler), 10);
    }

    @Bean
    WebSocketHandler webSocketHandler(GreetingService greetingService) {
        return new WebSocketHandler() {
            @Override
            public Mono<Void> handle(WebSocketSession session) {
                var receive = session.receive()
                .map(WebSocketMessage::getPayloadAsText)
                        .map(GreetingRequest::new)
                        .flatMap(greetingService::greetMany)
                        .map(GreetingResponse::getMessage)
                        .map(session::textMessage)
                        .doFinally(signal -> log.info("finally" + signal.toString()));
                return session.send(receive);
            }
        };
    }
}

@Service
class GreetingService {
    //Search for BlockHound
    Flux<GreetingResponse> greetMany(GreetingRequest request) {
        return Flux
                .fromStream(Stream.generate(() ->
                        greet(request.getName())))
                .delayElements(Duration.ofSeconds(1));
        //.subscribeOn(Schedulers.immediate());//explore
        //.subscribeOn(Schedulers.boundedElastic());
        //.subscribeOn(Schedulers.parallel());
    }

  private GreetingResponse greet(String name) {
        return new GreetingResponse("Hello " + name + "@" + Instant.now());
    }


}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingResponse {
    private String message;
}

@Data
@AllArgsConstructor
@NoArgsConstructor
class GreetingRequest {
    private String name;
}

