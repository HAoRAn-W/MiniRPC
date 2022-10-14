package one.whr.remote.transport.client;

import one.whr.annotation.SPI;
import one.whr.remote.dto.RpcRequest;

import java.util.concurrent.ExecutionException;

@SPI
public interface RpcRequestTransport {
    Object sendRpcRequest(RpcRequest rpcRequest) throws ExecutionException, InterruptedException;
}
