package one.whr.loadbalance;

import one.whr.annotation.SPI;
import one.whr.remote.dto.RpcRequest;

import java.util.List;

@SPI
public interface LoadBalance {
    // Choose one worker from a list of existing service addresses
    String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest);
}
