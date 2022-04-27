/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
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
