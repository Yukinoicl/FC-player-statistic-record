package com.fcdata.fcdataserver.config;

import java.io.IOException;
import java.nio.file.Files;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SqliteConfig {

    @Bean
    public static BeanFactoryPostProcessor sqliteDirectoryCreator() {
        return beanFactory -> {
            try {
                Files.createDirectories(LocalAppSupport.dataDir());
            } catch (IOException e) {
                throw new IllegalStateException("无法创建 SQLite 数据目录", e);
            }
        };
    }
}
