package com.gjjfintech.aiprompts;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan // picks up @ConfigurationProperties classes in this package
public class CodegenServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(CodegenServiceApplication.class, args);
    }
}
