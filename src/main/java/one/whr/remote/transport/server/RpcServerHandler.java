package one.whr.remote.transport.server;

import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.timeout.IdleState;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.util.ReferenceCountUtil;
import lombok.extern.slf4j.Slf4j;
import one.whr.compress.Compress;
import one.whr.enums.CompressTypeEnum;
import one.whr.enums.RpcConfigEnum;
import one.whr.enums.RpcResponseCodeEnum;
import one.whr.enums.SerializationEnum;
import one.whr.factory.SingletonFactory;
import one.whr.remote.dto.RpcMessage;
import one.whr.remote.dto.RpcRequest;
import one.whr.remote.dto.RpcResponse;
import one.whr.utils.RpcConstants;

/**
 * Customize channel handler to process messages sent by clients
 */
@Slf4j
public class RpcServerHandler extends ChannelInboundHandlerAdapter {
    private final RpcRequestHandler rpcRequestHandler;

    public RpcServerHandler() {
        this.rpcRequestHandler = SingletonFactory.getInstance(RpcRequestHandler.class);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) {
        try {
            if (msg instanceof RpcMessage) {
                log.info("Server received message: [{}]", msg);
                RpcMessage responseMessage = new RpcMessage();
                responseMessage.setCodec(SerializationEnum.KYRO.getCode());
                responseMessage.setCompress(CompressTypeEnum.GZIP.getCode());

                byte messageType = ((RpcMessage) msg).getMessageType();
                if (messageType == RpcConstants.HEARTBEAT_PING_TYPE) {
                    responseMessage.setMessageType(RpcConstants.HEARTBEAT_PONG_TYPE);
                    responseMessage.setData(RpcConstants.PONG);
                } else {
                    RpcRequest rpcRequest = (RpcRequest) ((RpcMessage) msg).getData();
                    Object result = rpcRequestHandler.handle(rpcRequest);
                    log.info(String.format("Server RPC get result: %s", result.toString()));
                    responseMessage.setMessageType(RpcConstants.RESPONSE_TYPE);
                    if (ctx.channel().isActive() && ctx.channel().isWritable()) {
                        RpcResponse<Object> rpcResponse = RpcResponse.success(result, rpcRequest.getRequestId());
                        responseMessage.setData(rpcResponse);
                    } else {
                        RpcResponse<Object> rpcResponse = RpcResponse.fail(RpcResponseCodeEnum.FAIL);
                        responseMessage.setData(rpcResponse);
                        log.error("context not writable now, message dropped");
                    }
                }
                ctx.writeAndFlush(responseMessage).addListener(ChannelFutureListener.CLOSE_ON_FAILURE);
            }
        } finally {
            //Ensure that ByteBuf is released, otherwise there may be memory leaks
            ReferenceCountUtil.release(msg);
        }
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleState state = ((IdleStateEvent) evt).state();
            if (state == IdleState.READER_IDLE) {
                log.info("idle check happen, so close the connection");
                ctx.close();
            }
        } else {
            super.userEventTriggered(ctx, evt);
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        log.error("server exception occur!");
        cause.printStackTrace();
        ctx.close();
    }
}
