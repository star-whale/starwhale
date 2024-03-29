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

package ai.starwhale.mlops.domain.panel;

import ai.starwhale.mlops.api.protocol.panel.PanelPluginVo;
import ai.starwhale.mlops.common.Converter;
import ai.starwhale.mlops.common.IdConverter;
import ai.starwhale.mlops.domain.panel.po.PanelPluginEntity;
import ai.starwhale.mlops.exception.ConvertException;
import org.springframework.stereotype.Component;

@Component
public class PanelPluginConverter implements Converter<PanelPluginEntity, PanelPluginVo> {

    private final IdConverter idConvertor;

    PanelPluginConverter(IdConverter idConvertor) {
        this.idConvertor = idConvertor;
    }

    @Override
    public PanelPluginVo convert(PanelPluginEntity plugin) throws ConvertException {
        return PanelPluginVo.builder()
                .id(idConvertor.convert(plugin.getId()))
                .name(plugin.getName())
                .version(plugin.getVersion())
                .build();
    }

    @Override
    public PanelPluginEntity revert(PanelPluginVo panelPluginVo) throws ConvertException {
        return PanelPluginEntity.builder()
            .name(panelPluginVo.getName())
            .version(panelPluginVo.getVersion())
            .meta("")
            .build();
    }
}
