package gov.nasa.jpl.adaptation;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


@SpringBootApplication
public class AdaptationApplication {

    private static final Logger logger = LoggerFactory.getLogger(AdaptationApplication.class);

    public static void main(String[] args) {
        SpringApplication.run(AdaptationApplication.class, args);
        logger.info("Adapttion Service started");
    }
}
