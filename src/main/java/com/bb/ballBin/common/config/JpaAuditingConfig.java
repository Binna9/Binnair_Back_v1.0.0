package com.bb.ballBin.common.config;

import com.bb.ballBin.common.util.AuditorAwareImpl;
import com.bb.ballBin.user.entity.User;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.AuditorAware;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@Configuration
@EnableJpaAuditing(auditorAwareRef = "auditorAwareImpl")
public class JpaAuditingConfig {

    @Bean
    public AuditorAware<User> auditorAware() {
        return new AuditorAwareImpl();
    }
}
