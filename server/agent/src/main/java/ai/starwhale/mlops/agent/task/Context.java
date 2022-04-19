/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Builder
@AllArgsConstructor
@Data
public class Context {

    private final Map<String, Object> values = new HashMap<>();

    public void set(String key, Object obj) {
        values.put(key, obj);
    }

    public <T> T get(String key, Class<T> clazz) {
        return clazz.cast(values.get(key));
    }

    public static Context instance() {
        return new Context();
    }
}
