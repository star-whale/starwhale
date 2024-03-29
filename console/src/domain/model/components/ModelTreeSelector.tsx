import { findTreeNode } from '@starwhale/ui/base/tree-view/utils'
import { DynamicSelector, SelectorItemByTree } from '@starwhale/ui/DynamicSelector'
import { TreeNodeData } from '@starwhale/ui/base/tree-view'
import React, { useEffect } from 'react'
import ModelLabel, { getModelLabel } from './ModelLabel'
import { themedStyled } from '@starwhale/ui/theme/styletron'
import Button from '@starwhale/ui/Button'
import useTranslation from '@/hooks/useTranslation'
import { useFetchModelTree, useFetchRecentModelTree } from '../hooks/useFetchModelTree'
import _ from 'lodash'
import { DynamicSelectorPropsT, SelectorItemValueT } from '@starwhale/ui/DynamicSelector/types'
import QuickGroup, { QuickGroupEnum } from '@/components/QuickGroup'
import { useProject } from '@/domain/project/hooks/useProject'

const ModelTreeNode = themedStyled('div', () => ({
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'nowrap',
    height: '24px',
    width: 'auto',
}))

export function ModelTree({ ...props }: any) {
    const [t] = useTranslation()
    const OPTIONS = [
        {
            key: 'latest',
            label: t('model.selector.lastest'),
        },
        {
            key: 'current',
            label: t('model.selector.current'),
        },
        {
            key: 'guest',
            label: t('model.selector.shared'),
        },
        {
            key: 'all',
            label: t('model.selector.all'),
        },
    ]
    const { project } = useProject()
    const info = useFetchRecentModelTree(project?.id)
    const SearchSlot = (args: any) => (
        <QuickGroup
            options={OPTIONS}
            {...args}
            filters={{
                [QuickGroupEnum.latest]: [
                    (node: any) =>
                        info.data?.find((item) =>
                            item.versions.find((version) => {
                                return version.id === node?.info?.version?.id
                            })
                        ),
                ],
            }}
        />
    )

    return <SelectorItemByTree {...props} SearchSlot={SearchSlot} />
}

export function ModelTreeSelector(
    props: {
        projectId: string
        onDataChange?: (data: any) => void
        getId?: (obj: any) => any
        multiple?: boolean
        clearable?: boolean
    } & Partial<DynamicSelectorPropsT<any>>
) {
    const [t] = useTranslation()
    const { projectId, getId = (obj: any) => obj.id, multiple = false } = props
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
                label: (
                    <p className='color-[#02102b] text-14px'>
                        {[model.ownerName, model.projectName, model.modelName].join('/')}
                    </p>
                ),
                isExpanded: true,
                children:
                    model.versions?.map((item) => {
                        return {
                            id: getId(item),
                            label: (
                                <ModelTreeNode>
                                    <ModelLabel version={item} style={{ fontSize: '14px', marginRight: '4px' }} />
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
                                                `/projects/${model.projectName}/models/${model.modelName}/versions/${item.id}/overview`
                                            )
                                        }
                                    >
                                        {t('model.selector.view')}
                                    </Button>
                                </ModelTreeNode>
                            ),
                            info: {
                                labelView: (
                                    <ModelLabel
                                        version={item}
                                        model={model}
                                        isProjectShow
                                        style={{ fontSize: '14px' }}
                                    />
                                ),
                                labelTitle: getModelLabel(item, model),
                                data: model,
                                version: item,
                            },
                            isExpanded: true,
                        }
                    }) ?? [],
            }
        })

        return treeData
    }, [modelInfo, t, getId])

    const options = React.useMemo(() => {
        return [
            {
                id: 'tree',
                info: {
                    data: $treeData,
                },
                multiple,
                getData: (info: any, id: string) => findTreeNode(info.data, id),
                getDataToLabelView: (data: any) => data?.info.labelView,
                getDataToLabelTitle: (data: any) => data?.info.labelTitle,
                getDataToValue: (data: any) => data?.id,
                render: ModelTree as React.FC<any>,
            },
        ]
    }, [$treeData, multiple])

    return (
        <DynamicSelector
            {...props}
            value={(props.value && !_.isArray(props.value) ? [props.value] : props.value) as SelectorItemValueT[]}
            onChange={(v) => props.onChange?.((multiple ? v : v[0]) as any)}
            options={options as any}
        />
    )
}

export default ModelTreeSelector
