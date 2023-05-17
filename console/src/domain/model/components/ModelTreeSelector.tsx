import { findTreeNode } from '@starwhale/ui/base/tree-view/utils'
import { DynamicSelector, SelectorItemByTree } from '@starwhale/ui/DynamicSelector'
import { TreeNodeData } from '@starwhale/ui/base/tree-view'
import React, { useEffect } from 'react'
import ModelLabel from './ModelLabel'
import { themedStyled } from '@starwhale/ui/theme/styletron'
import Button from '@starwhale/ui/Button'
import useTranslation from '@/hooks/useTranslation'
import { useFetchModelTree } from '../hooks/useFetchModelTree'
import _ from 'lodash'

const ModelTreeNode = themedStyled('div', () => ({
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'nowrap',
    height: '24px',
    width: '100%',
}))

export function ModelTreeSelector(
    props: any & {
        projectId: string
        onDataChange?: (data: any) => void
        getId?: (obj: any) => any
        multiple?: boolean
    }
) {
    const [t] = useTranslation()
    const { projectId, getId = (obj: any) => obj.id, multiple } = props
    const modelInfo = useFetchModelTree(projectId)

    useEffect(() => {
        props.onDataChange?.(modelInfo.data)
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [modelInfo?.data])

    const $treeData = React.useMemo(() => {
        if (!modelInfo.isSuccess) return []

        const treeData: TreeNodeData[] = modelInfo.data.map((model) => {
            return {
                id: model.modelName,
                label: [model.ownerName, model.projectName, model.modelName].join('/'),
                isExpanded: true,
                children:
                    model.versions?.map((item) => {
                        return {
                            id: getId(item),
                            label: (
                                <ModelTreeNode>
                                    <ModelLabel version={item} />
                                    <Button
                                        key='version-history'
                                        kind='tertiary'
                                        overrides={{
                                            BaseButton: {
                                                style: {
                                                    flexShrink: 0,
                                                },
                                            },
                                        }}
                                        onClick={() =>
                                            window.open(
                                                `/projects/${projectId}/models/${model.modelName}/versions/${item.id}/overview`
                                            )
                                        }
                                    >
                                        {t('model.selector.view')}
                                    </Button>
                                </ModelTreeNode>
                            ),
                            labelView: <ModelLabel version={item} model={model} isProjectShow />,
                            labelTitle: '',
                            isExpanded: true,
                        }
                    }) ?? [],
            }
        })

        return treeData
    }, [modelInfo, projectId, t, getId])

    const options = React.useMemo(() => {
        return [
            {
                id: 'tree',
                info: {
                    data: $treeData,
                },
                multiple: false,
                getData: (info: any, id: string) => findTreeNode(info.data, id),
                getDataToLabelView: (data: any) => data?.labelView,
                getDataToLabelTitle: (data: any) => data?.labelTitle,
                getDataToValue: (data: any) => getId(data),
                render: SelectorItemByTree as React.FC<any>,
            },
        ]
    }, [$treeData, getId])

    return (
        <DynamicSelector
            {...props}
            value={props.value && !_.isArray(props.value) ? [props.value] : props.value}
            onChange={(v) => props.onChange?.(multiple ? v : v[0])}
            options={options}
        />
    )
}

export default ModelTreeSelector
