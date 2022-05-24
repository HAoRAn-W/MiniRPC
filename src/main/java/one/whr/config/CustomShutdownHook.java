package one.whr.config;

import lombok.extern.slf4j.Slf4j;
import one.whr.registry.zookeeper.util.CuratorUtils;
import one.whr.remote.transport.server.RpcServer;
import one.whr.utils.concurrent.threadpool.ThreadPoolFactoryUtil;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

@Slf4j
public class CustomShutdownHook {
    private static final CustomShutdownHook CUSTOME_SHUTDOWN_HOOK = new CustomShutdownHook();

    public static CustomShutdownHook getCustomeShutdownHook() {
        return CUSTOME_SHUTDOWN_HOOK;
    }

    public void clearAll() {
        log.info("add shutdown hook for clear all");
        // get current runtime and add shutdown hook
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                InetSocketAddress inetSocketAddress = new InetSocketAddress(InetAddress.getLocalHost().getHostAddress(), RpcServer.PORT);
                CuratorUtils.clearRegistry(CuratorUtils.getZkClient(), inetSocketAddress);
            } catch (UnknownHostException e) {
                log.error("Unable to find localhost address");
            }
            ThreadPoolFactoryUtil.shutdownAllThreadPool();
        }));
    }

}
