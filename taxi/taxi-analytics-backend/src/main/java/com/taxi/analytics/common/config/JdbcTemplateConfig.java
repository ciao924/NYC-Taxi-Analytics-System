package com.taxi.analytics.common.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

@Configuration
public class JdbcTemplateConfig {

    private static final Logger log = LoggerFactory.getLogger(JdbcTemplateConfig.class);

    @Autowired
    private DataSource dataSource;

    @Bean
    public JdbcTemplate jdbcTemplate() {
        log.info("初始化 JdbcTemplate");
        return new JdbcTemplate(dataSource);
    }
}