package one.whr.loadbalance;

import one.whr.remote.dto.RpcRequest;

import java.util.List;
import java.util.Random;

public class RandomLoadBalancer extends AbstractLoadBalancer{
    @Override
    protected String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        Random random = new Random();
        return serviceAddresses.get(random.nextInt(serviceAddresses.size()));
    }
}
