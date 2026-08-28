package com.fcdata.fcdataserver;

import com.fcdata.fcdataserver.config.LocalAppSupport;
import java.io.IOException;
import java.nio.file.Files;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationEnvironmentPreparedEvent;
import org.springframework.context.ApplicationListener;
import org.springframework.core.env.MapPropertySource;

@SpringBootApplication
public class FcdataserverApplication {

    public static void main(String[] args) throws IOException {
        Integer running = LocalAppSupport.findRunningInstance(args);
        if (running != null) {
            LocalAppSupport.openBrowser(running);
            return;
        }

        int port = LocalAppSupport.resolveListenPort(args);
        if (LocalAppSupport.portInUse(port)) {
            LocalAppSupport.openBrowser(port);
            return;
        }

        Files.createDirectories(LocalAppSupport.dataDir());
        if (LocalAppSupport.packaged()) {
            System.setProperty("java.awt.headless", "false");
        }
        SpringApplication app = new SpringApplication(FcdataserverApplication.class);
        if (LocalAppSupport.packaged()) {
            app.setHeadless(false);
        }
        app.addListeners((ApplicationListener<ApplicationEnvironmentPreparedEvent>) event -> {
            Map<String, Object> props = new LinkedHashMap<>();
            props.put("spring.datasource.url", LocalAppSupport.jdbcUrl());
            props.put("server.port", String.valueOf(port));
            if (LocalAppSupport.packaged()) {
                props.put("server.address", "127.0.0.1");
            }
            event.getEnvironment().getPropertySources().addFirst(new MapPropertySource("fcdataPaths", props));
        });
        app.run(args);
    }
}
