package com.fcdata.fcdataserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class BrowserLauncher {

    private static final Logger log = LoggerFactory.getLogger(BrowserLauncher.class);

    @Value("${server.port:11899}")
    private int port;

    @Value("${fcdata.open-browser:false}")
    private boolean openBrowser;

    @EventListener(ApplicationReadyEvent.class)
    public void onReady() {
        log.info("FC26 career app is ready at {}", LocalAppSupport.publicUrl(port));
        if (openBrowser) {
            LocalAppSupport.openBrowser(port);
        }
    }
}
