package one.whr.loadbalance.loadbalancer;

import lombok.extern.slf4j.Slf4j;
import one.whr.loadbalance.AbstractLoadBalancer;
import one.whr.remote.dto.RpcRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
public class ConsistentHashLoadBalancer extends AbstractLoadBalancer {
    // store selectors for different services
    private final ConcurrentHashMap<String, ConsistentHashSelector> selectors = new ConcurrentHashMap<>();

    /**
     * 通过RPC请求中的service名称选择selector进行地址选择
     *
     * @param serviceAddresses 服务地址列表
     * @param rpcRequest       RPC 请求
     * @return 服务地址
     */
    @Override
    protected String doSelect(List<String> serviceAddresses, RpcRequest rpcRequest) {
        int identityHashCode = System.identityHashCode(serviceAddresses);
        String rpcServiceName = rpcRequest.getRpcServiceName();
        ConsistentHashSelector selector = selectors.get(rpcServiceName);

        if (selector == null || selector.identityHashCode != identityHashCode) {
            selectors.put(rpcServiceName, new ConsistentHashSelector(serviceAddresses, 160, identityHashCode));
            selector = selectors.get(rpcServiceName);
        }
        return selector.select(rpcServiceName + Arrays.stream(rpcRequest.getParameters()));

    }

    /*
     * 静态内部类
     * 1. 在外部创建静态内部类实例不需要创建外部类的实例
     * 2. 静态内部类中可以定义静态成员和实例成员：
     *     外部类以外的其他类需要通过完整的类名访问静态内部类中的静态成员
     *     访问静态内部类中的实例成员，需要通过静态内部类的实例
     * 3. 静态内部类可以直接访问外部类的静态成员，如果要访问外部类的实例成员，则需要通过外部类的实例去访问
     */

    /**
     * 一致性哈希选择器
     */
    static class ConsistentHashSelector {
        // store workers for the service
        private final TreeMap<Long, String> virtualInvokers;
        private final int identityHashCode; // 服务对应的identityHashCode

        /**
         * @param serviceAddresses 服务地址列表
         * @param replicaNumber    replica数量
         * @param identityHashCode identity哈希
         */
        ConsistentHashSelector(List<String> serviceAddresses, int replicaNumber, int identityHashCode) {
            this.virtualInvokers = new TreeMap<>();
            this.identityHashCode = identityHashCode;

            for (String invoker : serviceAddresses) {
                for (int i = 0; i < replicaNumber / 4; i++) {
                    byte[] digest = md5(invoker + i);
                    for (int j = 0; j < 4; j++) {
                        long m = hash(digest, j);
                        virtualInvokers.put(m, invoker);
                    }
                }
            }
        }

        static byte[] md5(String key) {
            MessageDigest md;
            try {
                md = MessageDigest.getInstance("MD5");
                byte[] bytes = key.getBytes(StandardCharsets.UTF_8);
                md.update(bytes);
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException(e.getMessage(), e);
            }

            return md.digest();
        }

        static long hash(byte[] digest, int idx) {
            return ((long) (digest[3 + idx * 4] & 255) << 24 |
                    (long) (digest[2 + idx * 4] & 255) << 16 |
                    (long) (digest[1 + idx * 4] & 255) << 8 |
                    (long) (digest[idx * 4] & 255)) &
                    4294967295L;
        }

        public String select(String rpcServiceKey) {
            byte[] digest = md5(rpcServiceKey);
            return selectByKey(hash(digest, 0));
        }

        public String selectByKey(long hashCode) {
            Map.Entry<Long, String> entry = virtualInvokers.tailMap(hashCode, true).firstEntry();
            if (entry == null) {
                entry = virtualInvokers.firstEntry();
            }
            return entry.getValue();
        }
    }


}
