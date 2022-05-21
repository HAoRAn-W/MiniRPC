package one.whr.extension;


import lombok.extern.slf4j.Slf4j;
import one.whr.extension.annotation.SPI;
import one.whr.utils.Holder;
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

    // get ExtensionLoader for a certain type
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
                instance = holder.get();
                if (instance == null) {
                    instance = createExtension(name);
                    holder.set(instance);
                }
            }
        }
        return (T) instance;
    }

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

    // Extension configuration file name is named as type name
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

    // load class from configuration files
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
