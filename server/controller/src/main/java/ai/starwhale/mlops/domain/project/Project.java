/*
 * Copyright 2022.1-2022
 * StarWhale.ai All right reserved. This software is the confidential and proprietary information of
 * StarWhale.ai ("Confidential Information"). You shall not disclose such Confidential Information and shall use it only
 * in accordance with the terms of the license agreement you entered into with StarWhale.ai.
 */

package ai.starwhale.mlops.domain.project;

import ai.starwhale.mlops.common.IDConvertor;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Project {

    private String id;

    private String name;

    private String ownerId;

    private boolean isDefault;

    public Project fromEntity(ProjectEntity entity) {
        return fromEntity(entity, null);
    }

    public Project fromEntity(ProjectEntity entity, IDConvertor idConvertor) {
        if(entity == null) {
            return this;
        }
        if (idConvertor != null) {
            setId(idConvertor.convert(entity.getId()));
            setOwnerId(idConvertor.convert(entity.getOwner().getId()));
        }
        setName(entity.getProjectName());
        return this;
    }

}
