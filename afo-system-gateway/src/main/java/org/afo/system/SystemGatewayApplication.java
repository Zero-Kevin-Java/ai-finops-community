package org.afo.system;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.ComponentScan;

@SpringBootApplication
@ComponentScan(basePackages = {
    "org.afo.system",
    "org.afo.llm"
})
public class SystemGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(SystemGatewayApplication.class, args);
    }
}
