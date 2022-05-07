/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.com.
 */

package ai.starwhale.mlops.agent.task;

public interface SelectOneToExecute<Input, Output> {

    default void apply(Input input, Context context, Condition<Input> condition,
                       Action<Input, Output> one, Action<Input, Output> another) {
        if (condition.judge(input)) {
            one.apply(input, context);
        } else {
            another.apply(input, context);
        }
    }
}
