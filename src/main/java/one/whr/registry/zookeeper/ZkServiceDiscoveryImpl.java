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

@Slf4j
public class ZkServiceDiscoveryImpl implements ServiceDiscovery {
    private final LoadBalance loadBalancer;

    public ZkServiceDiscoveryImpl() {
        this.loadBalancer = ExtensionLoader.getExtensionLoader(LoadBalance.class).getExtension("loadBalance");
    }

    @Override
    public InetSocketAddress lookupService(RpcRequest rpcRequest) {
        String rpcServiceName = rpcRequest.getRpcServiceName();
        CuratorFramework zkClient = CuratorUtils.getZkClient();

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
