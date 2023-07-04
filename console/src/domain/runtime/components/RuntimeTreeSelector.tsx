import { findTreeNode } from '@starwhale/ui/base/tree-view/utils'
import { DynamicSelector, SelectorItemByTree } from '@starwhale/ui/DynamicSelector'
import { TreeNodeData } from '@starwhale/ui/base/tree-view'
import React, { useEffect } from 'react'
import RuntimeLabel, { getRuntimeLabel } from './RuntimeLabel'
import { themedStyled } from '@starwhale/ui/theme/styletron'
import Button from '@starwhale/ui/Button'
import useTranslation from '@/hooks/useTranslation'
import { useFetchRuntimeTree } from '../hooks/useFetchRuntimeTree'
import _ from 'lodash'

const RuntimeTreeNode = themedStyled('div', () => ({
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'nowrap',
    height: '24px',
    width: '100%',
}))

export function RuntimeTreeSelector(
    props: any & {
        projectId: string
        onDataChange?: (data: any) => void
        getId?: (obj: any) => any
        multiple?: boolean
    }
) {
    const [t] = useTranslation()
    const { projectId, getId = (obj: any) => obj.id, multiple = false } = props

    const runtimeInfo = useFetchRuntimeTree(projectId)

    useEffect(() => {
        props.onDataChange?.(runtimeInfo.data)
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [runtimeInfo?.data])

    const $treeData = React.useMemo(() => {
        if (!runtimeInfo.isSuccess) return []

        const treeData: TreeNodeData[] = runtimeInfo.data.map((runtime) => {
            return {
                id: runtime.runtimeName,
                label: [runtime.ownerName, runtime.projectName, runtime.runtimeName].join('/'),
                isExpanded: true,
                children:
                    runtime.versions?.map((item) => {
                        return {
                            id: getId(item),
                            label: (
                                <RuntimeTreeNode>
                                    <RuntimeLabel version={item} runtime={runtime} />
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
                                labelView: <RuntimeLabel version={item} runtime={runtime} isProjectShow />,
                                labelTitle: getRuntimeLabel(item, runtime),
                            },
                            isExpanded: true,
                        }
                    }) ?? [],
            }
        })

        return treeData
    }, [runtimeInfo, projectId, getId, t])

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
                render: SelectorItemByTree as React.FC<any>,
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
