package com.tanvir.core.config;

import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.TimeUnit;

@Configuration
public class NettyConfig {

    @Bean
    public NettyServerCustomizer nettyServerCustomizer() {
        return httpServer -> httpServer.tcpConfiguration(tcpServer ->
                tcpServer.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 900000)  // Connection timeout: 15 minutes
                        .option(ChannelOption.SO_RCVBUF, 100 * 1024 * 1024)       // Receive buffer: 100 MB
                        .option(ChannelOption.SO_SNDBUF, 100 * 1024 * 1024)       // Send buffer: 100 MB
                        .doOnConnection(connection -> {
                            // Add handlers to the pipeline
                            ChannelPipeline pipeline = connection.channel().pipeline();
                            pipeline.addLast(new ReadTimeoutHandler(900, TimeUnit.SECONDS));  // Read timeout: 15 minutes
                            pipeline.addLast(new WriteTimeoutHandler(900, TimeUnit.SECONDS)); // Write timeout: 15 minutes
                        })
        );
    }
}
