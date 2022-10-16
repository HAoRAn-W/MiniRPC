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

/**
 * RPC 服务端
 */
@Slf4j
@Component
public class RpcServer {
    public static final int PORT = 9998; // Listening port

    private final ServiceProvider serviceProvider = SingletonFactory.getInstance(ZkServiceProviderImpl.class);

    /**
     * 创建EventLoop，配置监听端口，配置pipeline，启动服务端
     *
     * @throws UnknownHostException 异常
     */
    public void start() throws UnknownHostException {
        CustomShutdownHook.getCustomeShutdownHook().clearAll(); // clear all before start
        String host = InetAddress.getLocalHost().getHostAddress();

        // 2 NioEventLoop group
        EventLoopGroup bossGroup = new NioEventLoopGroup(1); // 1 thread， MultithreadEventLoopGroup implementations which is used for NIO Selector based Channels.
        EventLoopGroup workerGroup = new NioEventLoopGroup();  // 默认的线程数是cpu核数的两倍
        DefaultEventExecutorGroup serviceHandlerGroup = new DefaultEventExecutorGroup(
                Runtime.getRuntime().availableProcessors() * 2,
                // custom service handlers' thread-pool config
                ThreadPoolFactoryUtil.createThreadFactory("service-handler-group", false)
        );

        try {
            ServerBootstrap bootstrap = new ServerBootstrap(); // bootstrap is used to config EventLoop and start it
            bootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class) // we need ServerSocketChannel on server side
                    // backlog length
                    // Socket参数，服务端接受连接的队列长度，如果队列已满，客户端连接将被拒绝。默认值，Windows为200，其他为128。
                    .option(ChannelOption.SO_BACKLOG, 128)  // bossGroup option
                    .handler(new LoggingHandler(LogLevel.INFO))
                    // disable Nagle algorithm and send package immediately
                    // Nagle's algorithm works by combining a number of small outgoing messages and sending them all at once
                    // workerGroup option
                    .childOption(ChannelOption.TCP_NODELAY, true)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)  // 启用该功能时，TCP会主动探测空闲连接的有效性
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) {
                            // ChannelPipeline是Netty处理请求的责任链，ChannelHandler则是具体处理请求的处理
                            // 实际上每一个channel都有一个处理器的流水线。
                            ChannelPipeline p = ch.pipeline(); // pipeline is the logic chain for packages
                            p.addLast(new IdleStateHandler(30, 0, 0, TimeUnit.SECONDS));  // in n out
                            p.addLast(new RpcMessageEncoder());  // outbound
                            p.addLast(new RpcMessageDecoder());  // inbound
                            p.addLast(serviceHandlerGroup, new RpcServerHandler());  // inbound
                        }
                    });

            // bind port and synchronize wait for binding success
            ChannelFuture future = bootstrap.bind(host, PORT).sync();
            // synchronize wait listening port close
            future.channel().closeFuture().sync();
        } catch (InterruptedException e) {
            log.error("Fail to start RPC server: ", e);
        } finally {
            // 关闭线程池
            log.warn("Shutting down boss group and worker group");
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
            serviceHandlerGroup.shutdownGracefully();
            log.warn("Shutdown server successfully");
        }
    }

    public void registerService(RpcServiceConfig rpcServiceConfig) {
        serviceProvider.publishService(rpcServiceConfig);
    }

}
