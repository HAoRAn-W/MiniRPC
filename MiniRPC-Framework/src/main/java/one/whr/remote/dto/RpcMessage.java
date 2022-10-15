package one.whr.remote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/**
 * RpcMessage是对request和response的封装，不会进行序列化在网络上传输，所以不需要实现Serializable
 */
@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcMessage {
    private byte messageType;  // request 或 response

    private byte codec;  // 编解码方式

    private byte compress;  // 压缩方式

    private int requestId;  // 请求ID

    private Object data;  // 消息体
}
