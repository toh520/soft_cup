package com.toh520.campus.config;

import org.springframework.boot.web.embedded.tomcat.TomcatServletWebServerFactory;
import org.springframework.boot.web.server.WebServerFactoryCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebServerConfig {
  /** 使用标准 NIO2 连接器，在 Windows 上通过 IOCP 避免 AF_UNIX selector 回环依赖。 */
  @Bean
  WebServerFactoryCustomizer<TomcatServletWebServerFactory> nio2Connector() {
    return factory -> factory.setProtocol("org.apache.coyote.http11.Http11Nio2Protocol");
  }
}
