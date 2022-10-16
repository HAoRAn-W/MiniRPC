package one.whr.registry.zookeeper;

import lombok.extern.slf4j.Slf4j;
import one.whr.enums.RpcErrorEnum;
import one.whr.exception.RpcException;
import one.whr.extension.ExtensionLoader;
import one.whr.loadbalance.LoadBalance;
import one.whr.registry.ServiceDiscovery;
import one.whr.registry.zookeeper.util.CuratorUtils;
import one.whr.remote.dto.RpcRequest;
import one.whr.utils.CollectionUtil;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * 客户端服务发现实现类
 */
@Slf4j
public class ZkServiceDiscoveryImpl implements ServiceDiscovery {
    private final LoadBalance loadBalancer;

    public ZkServiceDiscoveryImpl() {
        this.loadBalancer = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension("loadBalance");
    }

    /**
     * 在客户端向服务端发送请求之前，从zk获取服务的地址，并执行负载均衡
     *
     * @param rpcRequest RPC请求
     * @return
     */
    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        String rpcServiceName = rpcRequest.getRpcServiceName(); // interface name + group + version
        CuratorFramework zkClient = CuratorUtils.getZkClient();

        // 从zk获取服务的地址
        List<String> serviceUrlList = CuratorUtils.getChildrenNodes(zkClient, rpcServiceName);
        if (CollectionUtil.isEmpty(serviceUrlList)) {
            throw new RpcException(RpcErrorEnum.SERVICE_NOT_FOUND, rpcServiceName);
        }
        String targetServiceUrl = loadBalancer.selectServiceAddress(serviceUrlList, rpcRequest);

        log.info("Target service address: [{}]", targetServiceUrl);
        String[] socketAddressArray = targetServiceUrl.split(":");
        String host = socketAddressArray[0];
        int port = Integer.parseInt(socketAddressArray[1]);
        return new InetSocketAddress(host, port);
    }
}
