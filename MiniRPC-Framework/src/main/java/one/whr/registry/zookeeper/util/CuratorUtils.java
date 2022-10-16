package one.whr.registry.zookeeper.util;

import lombok.extern.slf4j.Slf4j;
import one.whr.enums.RpcConfigEnum;
import one.whr.utils.PropertiesUtils;
import org.apache.curator.RetryPolicy;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.imps.CuratorFrameworkState;
import org.apache.curator.framework.recipes.cache.PathChildrenCache;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.apache.zookeeper.CreateMode;

import java.net.InetSocketAddress;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Slf4j
public class CuratorUtils {
    private static final int BASE_SLEEP_TIME = 1000;

    private static final int MAX_RETRIES = 3;

    public static final String ZK_REGISTER_ROOT_PATH = "/my-rpc";

    private static final Map<String, List<String>> SERVICE_ADDRESS_MAP = new ConcurrentHashMap<>();

    private static final Set<String> REGISTERED_PATH_SET = ConcurrentHashMap.newKeySet();

    private static CuratorFramework zkClient;

    private static final String DEFAULT_ZOOKEEPER_ADDRESS = "127.0.0.1:2181";

    private CuratorUtils() {
    }

    /**
     * 将服务注册到zk
     *
     * @param zkClient zk
     * @param path     服务的路径
     */
    public static void createPersistentNode(CuratorFramework zkClient, String path) {
        try {
            if (REGISTERED_PATH_SET.contains(path) || zkClient.checkExists().forPath(path) != null) {
                log.info("The node already exists. The node is:[{}]", path);
            } else {
                zkClient.create().creatingParentsIfNeeded().withMode(CreateMode.PERSISTENT).forPath(path);
                log.info("The node was created successfully. The node is:[{}]", path);
            }
        } catch (Exception e) {
            log.error("create persistent node for path [{}] fail", path);
        }
    }

    /**
     * 获取节点下具体的地址列表
     *
     * @param zkClient       zk
     * @param rpcServiceName 服务名
     * @return url列表
     */
    public static List<String> getChildrenNodes(CuratorFramework zkClient, String rpcServiceName) {
        if (SERVICE_ADDRESS_MAP.containsKey(rpcServiceName)) {
            return SERVICE_ADDRESS_MAP.get(rpcServiceName);
        }

        List<String> result = null;
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        try {
            result = zkClient.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, result);
            registerWatcher(rpcServiceName, zkClient);
        } catch (Exception e) {
            log.error("get children nodes for path [{}] fail", servicePath);
        }
        return result;
    }

    /**
     * 添加一个watcher，在子节点发生改变时更新服务地址保存进SERVICE_ADDRESS_MAP
     *
     * @param rpcServiceName 服务名
     * @param zkClient       zk
     * @throws Exception 异常
     */
    private static void registerWatcher(String rpcServiceName, CuratorFramework zkClient) throws Exception {
        String servicePath = ZK_REGISTER_ROOT_PATH + "/" + rpcServiceName;
        PathChildrenCache pathChildrenCache = new PathChildrenCache(zkClient, servicePath, true);
        PathChildrenCacheListener pathChildrenCacheListener = ((curatorFramework, pathChildrenCacheEvent) -> {
            List<String> serviceAddresses = curatorFramework.getChildren().forPath(servicePath);
            SERVICE_ADDRESS_MAP.put(rpcServiceName, serviceAddresses);
        });

        pathChildrenCache.getListenable().addListener(pathChildrenCacheListener);
        pathChildrenCache.start();
    }

    /**
     * clear all registered services
     *
     * @param zkClient          zk
     * @param inetSocketAddress service address
     */
    public static void clearRegistry(CuratorFramework zkClient, InetSocketAddress inetSocketAddress) {
        REGISTERED_PATH_SET.stream().parallel().forEach(p -> {
            try {
                if (p.endsWith(inetSocketAddress.toString())) {
                    zkClient.delete().forPath(p);
                }
            } catch (Exception e) {
                log.error("clear registry for path [{}] fail", p);
            }
        });

        log.info("All registered services on the server are cleared:[{}]", REGISTERED_PATH_SET);
    }

    /**
     * configure and start zk if not started yet
     *
     * @return zk
     */
    public static CuratorFramework getZkClient() {

        if (zkClient != null && zkClient.getState() == CuratorFrameworkState.STARTED) {
            return zkClient;
        }

        Properties properties = PropertiesUtils.readPropertiesFile(RpcConfigEnum.RPC_CONFIG_PATH.getPropertyValue());
        String zkAddress = properties != null && properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) != null ?
                properties.getProperty(RpcConfigEnum.ZK_ADDRESS.getPropertyValue()) : DEFAULT_ZOOKEEPER_ADDRESS;

        RetryPolicy retryPolicy = new ExponentialBackoffRetry(BASE_SLEEP_TIME, MAX_RETRIES);
        zkClient = CuratorFrameworkFactory.builder().connectString(zkAddress).retryPolicy(retryPolicy).build();

        zkClient.start();
        try {
            if (!zkClient.blockUntilConnected(30, TimeUnit.SECONDS)) {
                throw new RuntimeException("Timeout waiting for connecting to Zookeeper!");
            }
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return zkClient;

    }

}
