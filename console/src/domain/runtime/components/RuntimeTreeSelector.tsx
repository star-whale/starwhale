import { findTreeNode } from '@starwhale/ui/base/tree-view/utils'
import { DynamicSelector, SelectorItemByTree } from '@starwhale/ui/DynamicSelector'
import { TreeNodeData } from '@starwhale/ui/base/tree-view'
import React, { useEffect } from 'react'
import RuntimeLabel, { getRuntimeLabel } from './RuntimeLabel'
import { themedStyled } from '@starwhale/ui/theme/styletron'
import Button from '@starwhale/ui/Button'
import useTranslation from '@/hooks/useTranslation'
import { useFetchRecentRuntimeTree, useFetchRuntimeTree } from '../hooks/useFetchRuntimeTree'
import _ from 'lodash'
import QuickGroup, { QuickGroupEnum } from '@/components/QuickGroup'
import { useProject } from '@/domain/project/hooks/useProject'

const RuntimeTreeNode = themedStyled('div', () => ({
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'nowrap',
    height: '24px',
    width: 'auto',
}))
export function RuntimeTree({ ...props }: any) {
    const [t] = useTranslation()
    const OPTIONS = [
        {
            key: 'latest',
            label: t('runtime.selector.lastest'),
        },
        {
            key: 'current',
            label: t('runtime.selector.current'),
        },
        {
            key: 'guest',
            label: t('runtime.selector.shared'),
        },
        {
            key: 'all',
            label: t('runtime.selector.all'),
        },
    ]
    const { project } = useProject()
    const info = useFetchRecentRuntimeTree(project?.id)
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

export function RuntimeTreeSelector(
    props: any & {
        projectId: string
        onDataChange?: (data: any) => void
        getId?: (obj: any) => any
        multiple?: boolean
        clearable?: boolean
    }
) {
    const [t] = useTranslation()
    const { projectId, getId = (obj: any) => obj.id, multiple = false } = props

    const runtimeInfo = useFetchRuntimeTree(projectId)

    useEffect(() => {
        props.onDataChange?.(runtimeInfo.data)
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [runtimeInfo?.data])

    // check value if exists in runtimeInfo
    useEffect(() => {
        if (!runtimeInfo.isSuccess || !props.value) return
        const r = runtimeInfo.data.find((runtime) => {
            return runtime.versions?.find((item) => getId(item) === props.value)
        })
        if (!r) {
            props?.onChange(undefined)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [runtimeInfo?.data, props.value])

    const $treeData = React.useMemo(() => {
        if (!runtimeInfo.isSuccess) return []

        const treeData: TreeNodeData[] = runtimeInfo.data.map((runtime) => {
            return {
                id: runtime.runtimeName,
                label: (
                    <p className='color-[#02102b] text-14px'>
                        {[runtime.ownerName, runtime.projectName, runtime.runtimeName].join('/')}{' '}
                    </p>
                ),
                isExpanded: true,
                children:
                    runtime.versions?.map((item) => {
                        return {
                            id: getId(item),
                            label: (
                                <RuntimeTreeNode>
                                    <RuntimeLabel
                                        version={item}
                                        runtime={runtime}
                                        style={{ fontSize: '14px', marginRight: '4px' }}
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
                                            window.open(
                                                `/projects/${runtime.projectName}/runtimes/${runtime.runtimeName}/versions/${item.id}/overview`
                                            )
                                        }
                                    >
                                        {t('runtime.selector.view')}
                                    </Button>
                                </RuntimeTreeNode>
                            ),
                            info: {
                                labelView: (
                                    <RuntimeLabel
                                        version={item}
                                        runtime={runtime}
                                        isProjectShow
                                        style={{ fontSize: '14px' }}
                                    />
                                ),
                                labelTitle: getRuntimeLabel(item, runtime),
                                data: runtime,
                                version: item,
                            },
                            isExpanded: true,
                        }
                    }) ?? [],
            }
        })

        return treeData
    }, [runtimeInfo, getId, t])

    const options = React.useMemo(() => {
        return [
            {
                id: 'tree',
                info: {
                    data: $treeData,
                },
                multiple,
                getData: (info: any, id: string) => findTreeNode(info.data, id),
                getDataToLabelView: (data: any) => data?.info?.labelView,
                getDataToLabelTitle: (data: any) => data?.info?.labelTitle,
                getDataToValue: (data: any) => data?.id,
                render: RuntimeTree as React.FC<any>,
            },
        ]
    }, [$treeData, multiple])

    return (
        <DynamicSelector
            {...props}
            value={props.value && !_.isArray(props.value) ? [props.value] : props.value}
            onChange={(v) => props.onChange?.(multiple ? v : v[0])}
            options={options}
        />
    )
}

export default RuntimeTreeSelector
