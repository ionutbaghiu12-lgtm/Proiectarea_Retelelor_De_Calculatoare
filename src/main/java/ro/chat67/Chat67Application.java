package ro.chat67;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class Chat67Application {
    public static void main(String[] args) {
        SpringApplication.run(Chat67Application.class, args);
    }
}
