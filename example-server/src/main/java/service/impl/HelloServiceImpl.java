package service.impl;

import lombok.extern.slf4j.Slf4j;
import service.Hello;
import service.HelloService;

@Slf4j
public class HelloServiceImpl implements HelloService {
    static {
        System.out.println("HelloServiceImpl created");
    }
    @Override
    public String hello(Hello hello) {
        log.info("HelloServiceImpl received: {}.", hello.getMessage());
        String result = "Hello description is " + hello.getDescription();
        log.info("HelloServiceImpl return result: {}.", result);
        return result;
    }
}
