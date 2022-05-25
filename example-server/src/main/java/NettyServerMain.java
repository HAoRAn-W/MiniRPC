import one.whr.annotation.RpcScan;
import one.whr.config.RpcServiceConfig;
import one.whr.remote.transport.server.RpcServer;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import service.HelloService;
import service.impl.HelloServiceImpl;

import java.net.UnknownHostException;

@RpcScan(basePackage = {"one.whr"})
public class NettyServerMain {
    public static void main(String[] args) throws UnknownHostException {
        // register service via annotation
        AnnotationConfigApplicationContext applicationContext = new AnnotationConfigApplicationContext(NettyServerMain.class);
        RpcServer rpcServer = (RpcServer) applicationContext.getBean("rpcServer");
        HelloService helloService = new HelloServiceImpl();
        RpcServiceConfig rpcServiceConfig = RpcServiceConfig.builder()
                .group("hello-test-1")
                .version("version1.0")
                .service(helloService)
                .build();
        rpcServer.registerService(rpcServiceConfig);
        rpcServer.start();
    }
}
