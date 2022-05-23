package one.whr.factory;

import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SingletonFactory {
    private static final Map<String, Object> objectMap = new ConcurrentHashMap<>();

    private SingletonFactory() {
    }

    public static <T> T getInstance(Class<T> clazz) {
        if (clazz == null) {
            throw new IllegalArgumentException();
        }
        String key = clazz.toString();
        if (objectMap.containsKey(key)) {
            return clazz.cast(objectMap.get(key));
        } else {
            return clazz.cast(objectMap.computeIfAbsent(key, c -> {
                try {
                    return clazz.getDeclaredConstructor().newInstance();

                } catch (InstantiationException | IllegalAccessException | InvocationTargetException |
                         NoSuchMethodException e) {
                    throw new RuntimeException(e.getMessage(), e);
                }
            }));
        }
    }
}
