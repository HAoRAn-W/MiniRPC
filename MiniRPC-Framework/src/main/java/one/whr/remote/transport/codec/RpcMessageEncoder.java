package one.whr.remote.transport.codec;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;
import one.whr.compress.Compress;
import one.whr.extension.ExtensionLoader;
import one.whr.remote.dto.RpcMessage;
import one.whr.enums.CompressTypeEnum;
import one.whr.enums.SerializationEnum;
import one.whr.serialization.Serializer;
import one.whr.utils.RpcConstants;

import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class RpcMessageEncoder extends MessageToByteEncoder<RpcMessage> {

    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(0);

    /**
     *   0     1     2     3     4        5     6     7     8         9          10      11     12  13  14   15 16
     *   +-----+-----+-----+-----+--------+----+----+----+------+-----------+-------+----- --+-----+-----+-------+
     *   |   magic   code        |version | full length         | messageType| codec|compress|    RequestId       |
     *   +-----------------------+--------+---------------------+-----------+-----------+-----------+------------+
     *   |                                                                                                       |
     *   |                                         body                                                          |
     *   |                                                                                                       |
     *   |                                        ... ...                                                        |
     *   +-------------------------------------------------------------------------------------------------------+
     * 4B  magic code（魔法数）   1B version（版本）   4B full length（消息长度）    1B messageType（消息类型）
     * 1B compress（压缩类型） 1B codec（序列化类型）    4B  requestId（请求的Id）
     * body（object类型数据）
     */
    @Override
    protected void encode(ChannelHandlerContext ctx, RpcMessage rpcMessage, ByteBuf byteBuf) {
        try {
            byteBuf.writeBytes(RpcConstants.MAGIC_NUMBER);
            byteBuf.writeByte(RpcConstants.VERSION);
            int fullLengthFieldIndex = byteBuf.writerIndex();
            byteBuf.writerIndex(byteBuf.writerIndex() + 4); // leave 4 bytes for value of full length

            byte messageType = rpcMessage.getMessageType();
            byteBuf.writeByte(messageType);
            byteBuf.writeByte(rpcMessage.getCodec());
            byteBuf.writeByte(CompressTypeEnum.GZIP.getCode());
            byteBuf.writeInt(ATOMIC_INTEGER.getAndIncrement());

            // write body
            byte[] body = null;
            int fullLength = RpcConstants.HEAD_LENGTH; // initially full length is body length

            if(messageType != RpcConstants.HEARTBEAT_PING_TYPE && messageType != RpcConstants.HEARTBEAT_PONG_TYPE) {
                String codecName = SerializationEnum.getName(rpcMessage.getCodec());
                log.info("codec name: [{}]", codecName);

                Serializer serializer = ExtensionLoader.getExtensionLoader(Serializer.class).getExtension(codecName);
                body = serializer.serialize(rpcMessage.getData());

                String compressName = CompressTypeEnum.getName(rpcMessage.getCompress());
                Compress compressor = ExtensionLoader.getExtensionLoader(Compress.class).getExtension(compressName);
                body = compressor.compress(body);
                fullLength += body.length;
            }
            if(body != null) {
                byteBuf.writeBytes(body);
            }
            int writeIndex = byteBuf.writerIndex();
            byteBuf.writerIndex(fullLengthFieldIndex);
            byteBuf.writeInt(fullLength);
            byteBuf.writerIndex(writeIndex);
        } catch (Exception e) {
            log.error("RpcRequest encode error, ", e);
        }
    }
}
