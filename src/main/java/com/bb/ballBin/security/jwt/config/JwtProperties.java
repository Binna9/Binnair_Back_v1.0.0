package com.bb.ballBin.security.jwt.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "spring.jwt") // properties 파일 바인딩
public class JwtProperties {

    private String secret;
    private String oauth2Secret;
    private Long accessTokenExpiration;
    private Long refreshTokenExpiration;
}
