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

package ai.starwhale.mlops.domain.job;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@NoArgsConstructor
@AllArgsConstructor
@Data
public class StatusRequirement<T> {

    Set<T> requiredStatuses;

    RequireType requireType;

    public enum RequireType {
        /**
         * if any of the tasks fit the  taskStatus, the requirement is meet
         */
        ANY,
        /**
         * if not all the tasks fit the taskStatus, the requirement is not meet
         */
        ALL,
        /**
         * if any of the tasks fit the  taskStatus (status in taskStatuses), the requirement is not meet
         */
        HAVE_NO,
        /**
         * if no task fit the  taskStatus (status in taskStatuses), the requirement is not meet
         */
        MUST
    }

    public boolean fit(Collection<T> statuses) {
        switch (requireType) {
            case ANY:
            case MUST:
                return statuses.stream().anyMatch(ts -> getRequiredStatuses().contains(ts));
            case ALL:
                return statuses.stream().allMatch(ts -> getRequiredStatuses().contains(ts));
            case HAVE_NO:
                return statuses.stream().allMatch(ts -> !getRequiredStatuses().contains(ts));
            default:
                return false;

        }
    }

    public static <T> boolean match(Collection<T> tasks, Set<StatusRequirement<T>> requirements) {
        Map<Boolean, List<StatusRequirement<T>>> requireTypeListMap = requirements.stream()
                .collect(Collectors.groupingBy(tr -> tr.getRequireType() == RequireType.ANY));

        List<StatusRequirement<T>> anyR = requireTypeListMap.get(true);
        if (null != anyR) {
            for (StatusRequirement tr : anyR) {
                if (tr.fit(tasks)) {
                    return true;
                }
            }
            return false;
        }

        List<StatusRequirement<T>> negativeR = requireTypeListMap.get(false);
        if (null != negativeR) {
            for (StatusRequirement tr : negativeR) {
                if (!tr.fit(tasks)) {
                    return false;
                }
            }
        }

        return true;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        StatusRequirement that = (StatusRequirement) o;
        return requiredStatuses == that.requiredStatuses
                && requireType == that.requireType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(requiredStatuses, requireType);
    }
}
