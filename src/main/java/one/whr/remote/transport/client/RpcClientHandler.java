package one.whr.remote.transport.client;

import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import one.whr.factory.SingletonFactory;
import one.whr.remote.dto.RpcMessage;
import one.whr.remote.dto.RpcResponse;
import one.whr.enums.CompressTypeEnum;
import one.whr.enums.SerializationEnum;
import one.whr.utils.RpcConstants;

import java.net.InetSocketAddress;

@Slf4j
public class RpcClientHandler extends ChannelInboundHandlerAdapter {

    private final UnprocessedRequestMap unprocessedRequests;
    private final RpcClient rpcClient;

    public RpcClientHandler() {
        this.unprocessedRequests = SingletonFactory.getInstance(UnprocessedRequestMap.class);
        this.rpcClient = SingletonFactory.getInstance(RpcClient.class);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            log.info("client receive message: [{}]", msg);
            if(msg instanceof RpcMessage) {
                RpcMessage tmp = (RpcMessage) msg;
                byte messageType = tmp.getMessageType();
                if(messageType == RpcConstants.HEARTBEAT_PONG_TYPE) {
                    log.info("heart [{}]", tmp.getData());
                }
                else if (messageType == RpcConstants.RESPONSE_TYPE) {
                    RpcResponse<Object> rpcResponse = (RpcResponse<Object>) tmp.getData();
                    unprocessedRequests.complete(rpcResponse); // todo
                }
            }
        } finally {
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object event) throws Exception {
        if(event instanceof IdleStateEvent) {
            // when idle, send ping
            IdleState state = ((IdleStateEvent) event).state();
            if(state == IdleState.WRITER_IDLE) {
                log.info("write idle happens: [{}]", ctx.channel().remoteAddress());
                Channel channel = rpcClient.getChannel((InetSocketAddress) ctx.channel().remoteAddress());
                RpcMessage rpcMessage = new RpcMessage();
                rpcMessage.setCodec(SerializationEnum.KYRO.getCode());
                rpcMessage.setCompress(CompressTypeEnum.GZIP.getCode());
                rpcMessage.setMessageType(RpcConstants.HEARTBEAT_PING_TYPE);
                rpcMessage.setData(RpcConstants.PING);
                channel.writeAndFlush(rpcMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        }
        else {
            super.userEventTriggered(ctx, event);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("client catch exception：", cause);
        cause.printStackTrace();
        ctx.close();
    }

}
