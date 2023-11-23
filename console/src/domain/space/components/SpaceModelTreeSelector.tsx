import { findTreeNode } from '@starwhale/ui/base/tree-view/utils'
import { DynamicSelector, SelectorItemByTree } from '@starwhale/ui/DynamicSelector'
import { TreeNodeData } from '@starwhale/ui/base/tree-view'
import React, { useEffect } from 'react'
import ModelLabel, { getModelLabel } from '@model/components/ModelLabel'
import Button from '@starwhale/ui/Button'
import useTranslation from '@/hooks/useTranslation'
import _ from 'lodash'
import { DynamicSelectorPropsT, SelectorItemValueT } from '@starwhale/ui/DynamicSelector/types'
import QuickGroup, { QuickGroupEnum } from '@/components/QuickGroup'
import { api } from '@/api'
import { useEventCallback } from '@starwhale/core'

export function ModelTree({ ...props }: any) {
    const [t] = useTranslation()
    const OPTIONS = [
        {
            key: 'zone',
            label: t('ft.space'),
        },
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
    const SearchSlot = (args: any) => (
        <QuickGroup
            options={OPTIONS}
            {...args}
            value={args.value ?? QuickGroupEnum.zone}
            filters={{
                [QuickGroupEnum.latest]: [(node: any) => node.info?.version?.filter === QuickGroupEnum.latest],
                [QuickGroupEnum.all]: [(node: any) => node.info?.version?.filter === QuickGroupEnum.all],
                [QuickGroupEnum.current]: [(node: any) => node.info?.version?.filter === QuickGroupEnum.current],
                [QuickGroupEnum.zone]: [(node: any) => node.info?.version?.filter === QuickGroupEnum.zone],
            }}
        />
    )

    return <SelectorItemByTree {...props} SearchSlot={SearchSlot} />
}

const getParentNodeLabel = ({ model }) => (
    <p className='color-[#02102b] text-14px'>{[model.ownerName, model.projectName, model.modelName].join('/')}</p>
)

const getTreeNodeLabel = ({ t, item, model }) => (
    <div className='flex justify-between items-center h-24px w-auto flex-nowrap'>
        <ModelLabel
            version={item}
            style={{ fontSize: '14px', marginRight: '4px' }}
            hasDraft={item.filter === QuickGroupEnum.zone}
        />
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
                window.open(`/projects/${model.projectName}/models/${model.modelName}/versions/${item.id}/overview`)
            }
        >
            {t('model.selector.view')}
        </Button>
    </div>
)

const getSelectorLabelView = ({ item, model }) => {
    return (
        <ModelLabel
            version={item}
            model={model}
            isProjectShow
            style={{ fontSize: '14px' }}
            hasDraft={item.filter === QuickGroupEnum.zone}
        />
    )
}

export function ZoneModelTreeSelector(
    props: {
        projectId: any
        spaceId: any
        onDataChange?: (data: any) => void
        getId?: (obj: any) => any
        multiple?: boolean
        clearable?: boolean
    } & Partial<DynamicSelectorPropsT<any>>
) {
    const [t] = useTranslation()
    const { projectId, spaceId, getId = (obj: any) => obj.id, multiple = false } = props
    const modelInfo = api.useListModelTree(projectId, { scope: 'all' })
    const projectModelInfo = api.useListModelTree(projectId, { scope: 'project' })
    const sharedModelInfo = api.useListModelTree(projectId, { scope: 'shared' })
    const recentInfo = api.useRecentModelTree(projectId)
    const zoneInfo = api.useListModelTree1(projectId, spaceId)
    const updateVersion = useEventCallback(
        (data, type) =>
            data?.map((model) => {
                return {
                    ...model,
                    versions: model.versions?.map((item) => {
                        return {
                            ...item,
                            filter: type,
                        }
                    }),
                }
            }) ?? []
    )

    const $combinedData = React.useMemo(() => {
        const data = [
            ...updateVersion(modelInfo.data, QuickGroupEnum.all),
            ...updateVersion(recentInfo.data, QuickGroupEnum.latest),
            ...updateVersion(projectModelInfo.data, QuickGroupEnum.current),
            ...updateVersion(sharedModelInfo.data, QuickGroupEnum.guest),
            ...updateVersion(zoneInfo.data, QuickGroupEnum.zone),
        ]
        return data
    }, [modelInfo.data, zoneInfo.data, recentInfo.data, updateVersion, projectModelInfo.data, sharedModelInfo.data])

    useEffect(() => {
        props.onDataChange?.($combinedData)
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [$combinedData])

    const $treeData = React.useMemo(() => {
        const treeData: TreeNodeData[] = $combinedData.map((model) => {
            return {
                id: model.modelName,
                label: getParentNodeLabel({ model }),
                isExpanded: true,
                children:
                    model.versions?.map((item) => {
                        return {
                            id: getId(item),
                            label: getTreeNodeLabel({ t, item, model }),
                            info: {
                                labelView: getSelectorLabelView({ item, model }),
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
    }, [$combinedData, t, getId])

    const options = React.useMemo(() => {
        return [
            {
                id: 'tree',
                info: { data: $treeData },
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

export default ZoneModelTreeSelector
