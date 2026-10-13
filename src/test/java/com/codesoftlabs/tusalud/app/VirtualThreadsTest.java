package com.codesoftlabs.tusalud.app;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.Executor;
import org.apache.tomcat.util.threads.VirtualThreadExecutor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.tomcat.TomcatWebServer;
import org.springframework.boot.web.server.context.WebServerApplicationContext;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
class VirtualThreadsTest {

    @Autowired
    private WebServerApplicationContext context;

    @Test
    void tomcatAtiendePeticionesConVirtualThreads() {
        TomcatWebServer webServer = (TomcatWebServer) context.getWebServer();
        Executor executor =
                webServer.getTomcat().getConnector().getProtocolHandler().getExecutor();

        assertThat(executor).isInstanceOf(VirtualThreadExecutor.class);
    }
}
