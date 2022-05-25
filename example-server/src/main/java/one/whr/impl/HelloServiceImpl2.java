package one.whr.impl;

import lombok.extern.slf4j.Slf4j;
import service.Hello;
import service.HelloService;

@Slf4j
public class HelloServiceImpl2 implements HelloService {
    static {
        System.out.println("HelloServiceImpl2 created");
    }

    @Override
    public String hello(Hello hello) {
        log.info("HelloServiceImpl2 received: {}.", hello.getMessage());
        String result = "Hello description is " + hello.getDescription();
        log.info("HelloServiceImpl2 return result: {}.", result);
        return result;
    }
}
