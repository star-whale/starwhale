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

package ai.starwhale.mlops.agent.task;

public interface Action<Old, New> {

    default boolean valid(Old old, Context context) {
        return true;
    }

    default void orElse(Old old, Context context) {
    }

    default void pre(Old old, Context context) {

    }

    default New processing(Old old, Context context) throws Exception {
        return null;
    }

    default void post(Old old, New n, Context context) {
    }

    default void success(Old old, New n, Context context) throws Exception {
    }

    /**
     * when occur some exception
     *
     * @param old old param
     */
    default void fail(Old old, Context context, Exception e) {
    }


    default void apply(Old old, Context context) {
        if (valid(old, context)) {
            pre(old, context);
            try {
                New o = processing(old, context);
                success(old, o, context);
                post(old, o, context);
            } catch (Exception e) {
                //log.error(e.getMessage(), e);
                fail(old, context, e);
            }
        } else {
            orElse(old, context);
        }
    }
}
