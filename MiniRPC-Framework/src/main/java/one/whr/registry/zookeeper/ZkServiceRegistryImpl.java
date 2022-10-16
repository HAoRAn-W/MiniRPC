package one.whr.registry.zookeeper;

import one.whr.registry.ServiceRegistry;
import one.whr.registry.zookeeper.util.CuratorUtils;
import org.apache.curator.framework.CuratorFramework;

import java.net.InetSocketAddress;

public class ZkServiceRegistryImpl implements ServiceRegistry {
    /**
     * 将服务注册到zk
     *
     * @param rpcServiceName    RPC服务名称
     * @param inetSocketAddress 服务所在服务器的地址和端口
     */
    @Override
    public void registerService(String rpcServiceName, InetSocketAddress inetSocketAddress) {
        String servicePath = CuratorUtils.ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName + inetSocketAddress.toString();
        CuratorFramework zkClient = CuratorUtils.getZkClient();
        CuratorUtils.createPersistentNode(zkClient, servicePath);
    }
}
