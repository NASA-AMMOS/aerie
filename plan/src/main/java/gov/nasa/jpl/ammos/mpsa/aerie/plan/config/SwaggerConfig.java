package gov.nasa.jpl.ammos.mpsa.aerie.plan.config;

import gov.nasa.jpl.ammos.mpsa.aerie.plan.PlanValidator;
import gov.nasa.jpl.ammos.mpsa.aerie.plan.PlanValidatorInterface;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.Contact;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.util.Collections;

@Configuration
@EnableSwagger2
public class SwaggerConfig {
    @Value("${adaptation-url}")
    private String adaptationUri;

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.basePackage("gov.nasa.jpl.ammos.mpsa.aerie.plan.controller"))
                .paths(PathSelectors.any())
                .build()
                .apiInfo(apiInfo());
    }

    @Bean
    public PlanValidatorInterface planValidator() {
        return new PlanValidator(adaptationUri);
    }

    private ApiInfo apiInfo() {
        return new ApiInfo(
                "Aerie Plans API",
                "",
                "0.0.1",
                "Terms of service",
                new Contact("MPSA", "", "seq.support@jpl.nasa.gov"),
                "License of API", "API license URL", Collections.emptyList());
    }
}
