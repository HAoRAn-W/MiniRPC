package one.whr.loadbalance;

import one.whr.remote.dto.RpcRequest;
import one.whr.utils.CollectionUtil;

import java.util.List;

public abstract class AbstractLoadBalancer implements LoadBalance{
    @Override
    public String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest) {
        if(CollectionUtil.isEmpty(serviceUrlList)) {
            return null;
        }
        if(serviceUrlList.size() == 1) {
            return serviceUrlList.get(0);
        }
        return doSelect(serviceUrlList, rpcRequest);
    }

    protected abstract String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest);
}
