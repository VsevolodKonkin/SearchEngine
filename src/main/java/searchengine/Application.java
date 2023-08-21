package searchengine;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

@SpringBootApplication
@Slf4j
public class Application {
    public static void main(String[] args) {
        log.info("Starting the application...");
        SpringApplication.run(Application.class, args);
        log.info("Application started successfully.");
    }
}
