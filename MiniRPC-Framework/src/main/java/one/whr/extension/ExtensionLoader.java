package one.whr.extension;


import lombok.extern.slf4j.Slf4j;
import one.whr.annotation.SPI;
import one.whr.utils.StringUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
public class ExtensionLoader<T> {

    private static final String SERVICE_DIR = "META-INF/extensions/";

    // store ExtensionLoaders for different types
    private static final Map<Class<?>, ExtensionLoader<?>> EXTENSION_LOADERS = new ConcurrentHashMap<>();

    // cache instances that have been instanced
    private final Map<String, Holder<Object>> INSTANCE_CACHE = new ConcurrentHashMap<>();

    // cache classes
    private final Holder<Map<String, Class<?>>> CLASSES_HOLDER = new Holder<>();

    // store instances
    private static final Map<Class<?>, Object> EXTENSION_INSTANCES = new ConcurrentHashMap<>();

    // store the type this ExtensionLoader is used for
    private final Class<?> type;

    private ExtensionLoader(Class<?> type) {
        this.type = type;
    }


    /**
     * 这是一个静态方法，
     * 根据传入的类型返回对应的ExtensionLoader加载器
     * type必须是接口类型，不可为null且必须被@SPI注解
     * 如果这个接口类型是第一次被加载，先创建一个ExtensionLoader加载器，保存在EXTENSION_LOADERS中
     * @param type 接口类型
     * @param <S> 要加载的类型
     * @return 该接口对应的扩展加载器
     */
    public static <S> ExtensionLoader<S> getExtensionLoader(Class<S> type) {
        if (type == null) {
            throw new IllegalArgumentException("Extension type is null");
        }
        if (!type.isInterface()) {
            throw new IllegalArgumentException("Extension type must be an interface");
        }
        if (!type.isAnnotationPresent(SPI.class)) {
            throw new IllegalArgumentException("Extension type not annotated by @SPI");
        }

        ExtensionLoader<S> extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        if (extensionLoader == null) {
            EXTENSION_LOADERS.putIfAbsent(type, new ExtensionLoader<S>(type));
            extensionLoader = (ExtensionLoader<S>) EXTENSION_LOADERS.get(type);
        }
        return extensionLoader;
    }

    /**
     * 根据指定的配置项名称加载实例
     * 传入的名称不可为空
     * 首先查看INSTANCE_CACHE中该名称的实例是否已经被加载过了，如果已经存在，直接拿来用
     * 如果不存在，在INSTANCE_CACHE中先加入一个Holder，
     * 通过双重检查锁，使用createExtension(name)创建实例并返回。
     * @param name 扩展的名称，与配置文件中等号左边的相同
     * @return 指定的实例
     */
    public T getExtension(String name) {
        if (StringUtil.isBlank(name)) {
            throw new IllegalArgumentException("Extension name should not be null or empty");
        }
        Holder<Object> holder = INSTANCE_CACHE.get(name); // try to get cached instance first
        if (holder == null) {
            INSTANCE_CACHE.putIfAbsent(name, new Holder<>());
            holder = INSTANCE_CACHE.get(name);
        }

        Object instance = holder.get(); // get instance from holder

        // double check lock
        if (instance == null) {
            synchronized (holder) {
                // though we have synchronization on local variable, but only the reference is local
                // in Java memory model, the object lives on heap, which may be access by multiple threads
                instance = holder.get();
                if (instance == null) {
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

    /**
     * 通过getExtensionClasses()加载所有的配置的类，查看配置文件中是否包含该name的配置项，如果不存在则抛出异常
     * 在EXTENSION_INSTANCES查看是否包含该类的实例，如果有就直接用
     * 如果没有，就用反射的方法创建一个实例， 保存在EXTENSION_INSTANCES中，并返回
     * @param name 扩展的名称，与配置文件中等号左边的相同
     * @return 加载的实例
     */
    private T createExtension(String name) {
        Class<?> clazz = getExtensionClasses().get(name);
        if(clazz == null) {
            throw new RuntimeException("No extension class named " + name);
        }
        T instance = (T) EXTENSION_INSTANCES.get(clazz);
        if(instance == null) {
            try {
                EXTENSION_INSTANCES.putIfAbsent(clazz, clazz.newInstance());
                instance = (T) EXTENSION_INSTANCES.get(clazz);

            } catch (InstantiationException | IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }
        return instance;
    }

    /**
     * 在CLASSES_HOLDER尝试获得一个map，保存类name和对应的类
     * 如果不存在，就用双重检查锁创建一个，并调用loadDir()把所有的配置文件路径下的接口配置文件中的类全部保存到map中
     * @return name和其对应的类的map
     */
    // get loaded classes
    private Map<String, Class<?>> getExtensionClasses() {
        Map<String, Class<?>> classes = CLASSES_HOLDER.get();
        if (classes == null) {
            synchronized (CLASSES_HOLDER) {
                classes = CLASSES_HOLDER.get();
                if (classes == null) {
                    classes = new HashMap<>();
                    loadDir(classes);
                    CLASSES_HOLDER.set(classes);
                }
            }
        }
        return classes;
    }


    /**
     * 从指定的路径下读取配置文件，通过负责的类加载器加载类，获得name和类的映射，并保存在map中返回
     * @param extensionClasses name和其对应的类的map
     */
    private void loadDir(Map<String, Class<?>> extensionClasses) {
        String fileName = ExtensionLoader.SERVICE_DIR + type.getName();
        try {
            Enumeration<URL> urls;
            ClassLoader classLoader = ExtensionLoader.class.getClassLoader();
            urls = classLoader.getResources(fileName);
            if (urls != null) {
                while (urls.hasMoreElements()) {
                    URL resourceUrl = urls.nextElement();
                    loadClasses(extensionClasses, classLoader, resourceUrl);
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }


    /**
     * 从文件中加载所有的类，保存在map中返回
     * @param extensionClasses name和其对应的类的map
     * @param classLoader 指定的类加载器
     * @param url 配置文件路径
     */
    private void loadClasses(Map<String, Class<?>> extensionClasses, ClassLoader classLoader, URL url) {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(url.openStream(), UTF_8))) {
            String line;
            while((line = reader.readLine()) != null) {
                final int ci = line.indexOf('#'); // index of comment symbol '#'
                if(ci >= 0) {
                    line = line.substring(0, ci); // discard comment
                }
                line = line.trim();
                if(line.length() > 0) {
                    try {
                        final int ei = line.indexOf('=');
                        String name = line.substring(0, ei).trim();
                        String clazzName = line.substring(ei + 1).trim();

                        if(name.length() > 0 & clazzName.length() > 0) {
                            Class<?> clazz = classLoader.loadClass(clazzName);
                            extensionClasses.put(name, clazz);
                        }
                    } catch (ClassNotFoundException e) {
                        log.error(e.getMessage());
                    }
                }
            }
        } catch (IOException e) {
            log.error(e.getMessage());
        }
    }
}
