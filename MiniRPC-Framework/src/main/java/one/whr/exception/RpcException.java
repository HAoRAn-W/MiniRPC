package one.whr.exception;

import one.whr.enums.RpcErrorEnum;

public class RpcException extends RuntimeException{
    public RpcException(RpcErrorEnum rpcErrorMessageEnum, String detail) {
        super(rpcErrorMessageEnum.getMessage() + ":" + detail);
    }

    public RpcException(String message, Throwable cause) {
        super(message, cause);
    }

    public RpcException(RpcErrorEnum rpcErrorEnum) {
        super(rpcErrorEnum.getMessage());
    }
}
