/*
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

package ai.starwhale.mlops.agent.task;

import cn.hutool.json.JSONUtil;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public interface Action<Input, Output> {

    default boolean valid(Input input, Context context) {
        return true;
    }

    default void orElse(Input input, Context context) {
    }

    default void pre(Input input, Context context) {

    }

    default Output processing(Input input, Context context) throws Exception {
        return null;
    }

    default void post(Input input, Output output, Context context) {
    }

    default void success(Input input, Output output, Context context) throws Exception {
    }

    /**
     * when occur some exception
     *
     * @param input old param
     */
    default void fail(Input input, Context context, Exception e) {
    }


    default void apply(Input input, Context context) {
        if (valid(input, context)) {
            pre(input, context);
            Output output = null;
            try {
                output = processing(input, context);
                success(input, output, context);
            } catch (Exception e) {
                LogHolder.LOGGER.error("execute action:{}, input is:{} error:{}", this.getClass(), JSONUtil.toJsonStr(input), e.getMessage(), e);
                fail(input, context, e);
            }
            post(input, output, context);
        } else {
            orElse(input, context);
        }
    }
}

final class LogHolder { // not public
    static final Logger LOGGER = getLogger(Action.class);
}