package one.whr.remote.transport.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import lombok.extern.slf4j.Slf4j;
import one.whr.extension.ExtensionLoader;
import one.whr.factory.SingletonFactory;
import one.whr.registry.ServiceDiscovery;
import one.whr.remote.dto.RpcMessage;
import one.whr.remote.dto.RpcRequest;
import one.whr.remote.dto.RpcResponse;
import one.whr.enums.CompressTypeEnum;
import one.whr.enums.SerializationEnum;
import one.whr.remote.transport.codec.RpcMessageDecoder;
import one.whr.remote.transport.codec.RpcMessageEncoder;
import one.whr.serialization.KryoSerializer;
import one.whr.utils.RpcConstants;

import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

@Slf4j
public class RpcClient implements RpcRequestTransport {
    private final Bootstrap bootstrap;
    private final EventLoopGroup eventLoopGroup;
    private final UnprocessedRequestMap unprocessedRequests;
    private final ChannelProvider channelProvider;
    private final ServiceDiscovery serviceDiscovery;

    public RpcClient() {
        eventLoopGroup = new NioEventLoopGroup();
        bootstrap = new Bootstrap();
        KryoSerializer kryoSerializer = new KryoSerializer();
        bootstrap.group(eventLoopGroup)
                .channel(NioSocketChannel.class)
                .handler(new LoggingHandler(LogLevel.INFO))
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
                .handler(new ChannelInitializer<SocketChannel>() {
                    @Override
                    public void initChannel(SocketChannel ch) {
                        ch.pipeline()
                                .addLast(new IdleStateHandler(0, 0, 0, TimeUnit.SECONDS))
                                .addLast(new RpcMessageEncoder())  // outbound
                                .addLast(new RpcMessageDecoder())  // inbound
                                .addLast(new RpcClientHandler());  // inbound
                    }
                });
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequestMap.class);
        this.channelProvider = SingletonFactory.getInstance(ChannelProvider.class);
        this.serviceDiscovery = ExtensionLoader.getExtensionLoader(ServiceDiscovery.class).getExtension("zk");
    }

    @Override
    public Object sendRpcRequest(RpcRequest rpcRequest) throws ExecutionException, InterruptedException {
        CompletableFuture<RpcResponse<Object>> resultFuture = new CompletableFuture<>();

        // 获取服务地址
        InetSocketAddress inetSocketAddress = serviceDiscovery.lookupService(rpcRequest);

        // 根据服务地址创建channel
        Channel channel = getChannel(inetSocketAddress);
        if (channel.isActive()) {
            // 将请求存入map
            unprocessedRequests.put(rpcRequest.getRequestId(), resultFuture);
            RpcMessage rpcMessage = RpcMessage.builder()
                    .data(rpcRequest)
                    .codec(SerializationEnum.KRYO.getCode())
                    .compress(CompressTypeEnum.GZIP.getCode())
                    .messageType(RpcConstants.REQUEST_TYPE)
                    .build();
            channel.writeAndFlush(rpcMessage)
                    .addListener((ChannelFutureListener) future -> {
                        if (future.isSuccess()) {
                            log.info("message sent: [{}]", rpcMessage);
                        } else {
                            future.channel().close();
                            resultFuture.completeExceptionally(future.cause());
                            log.error("Send message failed", future.cause());
                        }
                    });
        } else {
            throw new IllegalStateException();
        }
        return resultFuture;
    }

    public Channel getChannel(InetSocketAddress inetSocketAddress) throws ExecutionException, InterruptedException {
        // 如果连接已经创建，直接拿来用
        Channel channel = channelProvider.get(inetSocketAddress);

        // 连接没有创建，先建立连接
        if (channel == null) {
            channel = doConnect(inetSocketAddress);
            channelProvider.set(inetSocketAddress, channel);
        }
        return channel;
    }

    public Channel doConnect(InetSocketAddress inetSocketAddress) throws ExecutionException, InterruptedException {
        CompletableFuture<Channel> completableFuture = new CompletableFuture<>();
        bootstrap.connect(inetSocketAddress).addListener((ChannelFutureListener) future -> {
            if (future.isSuccess()) {
                log.info("Client has connected to [{}] successfully", inetSocketAddress.toString());
                completableFuture.complete(future.channel());
            } else {
                throw new IllegalStateException();
            }
        });
        return completableFuture.get();
    }



    public void close() {
        eventLoopGroup.shutdownGracefully();
    }
}
