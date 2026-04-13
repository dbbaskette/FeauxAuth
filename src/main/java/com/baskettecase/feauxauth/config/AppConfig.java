package com.baskettecase.feauxauth.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "feauxauth")
@Getter @Setter
public class AppConfig {

    private String issuer = "http://localhost:8080";

    private Admin admin = new Admin();

    @Getter @Setter
    public static class Admin {
        private String username = "admin";
        private String password = "feauxauth";
    }
}
