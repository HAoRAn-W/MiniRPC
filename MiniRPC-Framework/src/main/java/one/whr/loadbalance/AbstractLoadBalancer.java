package one.whr.loadbalance;

import one.whr.remote.dto.RpcRequest;
import one.whr.utils.CollectionUtil;

import java.util.List;

public abstract class AbstractLoadBalancer implements LoadBalance {
    /**
     * 根据请求选择服务地址，如果地址列表为空，返回null
     * 如果地址列表中只有一个，不必进行下一步选择直接返回
     * 如果有多个可选地址，doSelect方法进行负载均衡
     *
     * @param serviceUrlList 服务地址列表
     * @param rpcRequest     RPC请求
     * @return 服务地址
     */
    @Override
    public String selectServiceAddress(List<String> serviceUrlList, RpcRequest rpcRequest) {
        if (CollectionUtil.isEmpty(serviceUrlList)) {
            return null;
        }
        if (serviceUrlList.size() == 1) {
            return serviceUrlList.get(0);
        }
        return doSelect(serviceUrlList, rpcRequest);
    }

    protected abstract String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest);
}
