package one.whr.impl;

import lombok.extern.slf4j.Slf4j;
import one.whr.annotation.RpcService;
import service.Hello;
import service.HelloService;

@Slf4j
@RpcService(group = "hello-test-1", version = "version1.0")
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
