package one.whr.remote.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;
import one.whr.enums.RpcResponseCodeEnum;

import java.io.Serializable;

@AllArgsConstructor
@NoArgsConstructor
@Getter
@Setter
@Builder
@ToString
public class RpcResponse<T> implements Serializable {
    private static final long serialVersionUID = 9074908372316261836L;
    private String requestId;

    private Integer code;

    private String message;

    private T data;

    /**
     * 生成success的RPC响应
     * @param data 服务执行结果
     * @param requestId 请求ID
     * @param <T> 类型
     * @return RPC响应
     */
    public static <T> RpcResponse<T> generateSuccessResponse(T data, String requestId) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setCode(RpcResponseCodeEnum.SUCCESS.getCode());
        response.setMessage(RpcResponseCodeEnum.SUCCESS.getMessage());
        response.setRequestId(requestId);
        if (null != data) {
            response.setData(data);
        }
        return response;
    }

    /**
     * 生成RPC失败响应
     * @param rpcResponseCodeEnum 错误代码
     * @param <T> 类型
     * @return RPC失败响应
     */
    public static <T> RpcResponse<T> generateFailResponse(RpcResponseCodeEnum rpcResponseCodeEnum) {
        RpcResponse<T> response = new RpcResponse<>();
        response.setCode(rpcResponseCodeEnum.getCode());
        response.setMessage(rpcResponseCodeEnum.getMessage());
        return response;
    }
}
