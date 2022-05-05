package ai.starwhale.mlops.common;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class InvokerManager<K, T> {

    private Map<K, Invoker<T>> map;

    public static <K, T> InvokerManager<K, T> create() {
        return new InvokerManager<>();
    }

    private InvokerManager() {
        map = new HashMap<>();
    }

    public InvokerManager<K, T> addInvoker(K key, Invoker<T> invoker) {
        map.put(key, invoker);
        return this;
    }

    public InvokerManager<K, T> unmodifiable() {
        this.map = Collections.unmodifiableMap(map);
        return this;
    }

    public void invoke(K key, T param) throws UnsupportedOperationException {
        if(!map.containsKey(key)) {
            throw new UnsupportedOperationException(String.format("Unknown invoker key: %s", key));
        }
        map.get(key).invoke(param);
    }

}
