package tn.esprit.arabsoftback;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class ArabSoftBackApplication {

    public static void main(String[] args) {
        SpringApplication.run(ArabSoftBackApplication.class, args);
    }

}
