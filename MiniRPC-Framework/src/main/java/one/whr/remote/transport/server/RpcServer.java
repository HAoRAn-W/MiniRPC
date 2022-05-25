package one.whr.remote.transport.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import lombok.extern.slf4j.Slf4j;
import one.whr.config.CustomShutdownHook;
import one.whr.config.RpcServiceConfig;
import one.whr.factory.SingletonFactory;
import one.whr.registry.ServiceProvider;
import one.whr.registry.zookeeper.ZkServiceProviderImpl;
import one.whr.remote.transport.codec.RpcMessageDecoder;
import one.whr.remote.transport.codec.RpcMessageEncoder;
import one.whr.utils.concurrent.threadpool.ThreadPoolFactoryUtil;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RpcServer {
    public static final int PORT = 9998;

    private final ServiceProvider serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);

    public void registerService(RpcServiceConfig rpcServiceConfig) {
        serviceProvider.publishService(rpcServiceConfig);
    }

    public void start() throws UnknownHostException {
        CustomShutdownHook.getCustomeShutdownHook().clearAll(); // clear all before start
        String host = InetAddress.getLocalHost().getHostAddress();
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
                Runtime.getRuntime().availableProcessors() * 2,
                ThreadPoolFactoryUtil.createThreadFactory("service-handler-group", false)
        );

        try {
            ServerBootstrap bootstrap = new ServerBootstrap();
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    // disable Nagle algorithm and send package immediately
                    // Nagle's algorithm works by combining a number of small outgoing messages and sending them all at once
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    // backlog length
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .handler(new LoggingHandler(LogLevel.INFO))
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));
                            p.addLast(new RpcMessageEncoder());
                            p.addLast(new RpcMessageDecoder());
                            p.addLast(serviceHandlerGroup, new RpcServerHandler());
                        }
                    });
            // bind port and synchronize wait for binding success
            ChannelFuture future = bootstrap.bind(host, PORT).sync();
            // synchronize wait listening port close
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Fail to start RPC server: ", e);
        } finally {
            log.warn("Shutting down boss group and worker group");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            serviceHandlerGroup.shutdownGracefully();
            log.warn("Shutdown server successfully");
        }
    }

}
