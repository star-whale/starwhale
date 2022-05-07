/**
 * Copyright 2022 Starwhale, Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
