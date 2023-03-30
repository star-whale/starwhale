import { findTreeNode } from '@starwhale/ui/base/tree-view/utils'
import { DynamicSelector, SelectorItemByTree } from '@starwhale/ui/DynamicSelector'
import { TreeNodeData } from '@starwhale/ui/base/tree-view'
import React from 'react'
import RuntimeLabel from './RuntimeLabel'
import { themedStyled } from '@starwhale/ui/theme/styletron'
import Button from '@starwhale/ui/Button'
import useTranslation from '@/hooks/useTranslation'
import { useFetchRuntimeTree } from '../hooks/useFetchRuntimeTree'

const RuntimeTreeNode = themedStyled('div', () => ({
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'nowrap',
    height: '24px',
    width: '100%',
}))

export function RuntimeTreeSelector(props: any) {
    const [t] = useTranslation()
    const { projectId } = props
    const runtimeInfo = useFetchRuntimeTree(projectId)
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
                            id: item.id,
                            label: (
                                <RuntimeTreeNode>
                                    <RuntimeLabel version={item} runtime={runtime} />
                                    <Button
                                        key='version-history'
                                        kind='tertiary'
                                        onClick={() =>
                                            window.open(
                                                `/projects/${projectId}/runtimes/${runtime.runtimeName}/versions/${item.id}/overview`
                                            )
                                        }
                                    >
                                        {t('runtime.selector.view')}
                                    </Button>
                                </RuntimeTreeNode>
                            ),
                            labelView: <RuntimeLabel version={item} runtime={runtime} isProjectShow />,
                            labelTitle: '',
                            isExpanded: true,
                        }
                    }) ?? [],
            }
        })

        return treeData
    }, [runtimeInfo, projectId, t])

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
                getDataToValue: (data: any) => data?.id,
                render: SelectorItemByTree as React.FC<any>,
            },
        ]
    }, [$treeData])

    return <DynamicSelector {...props} options={options} />
}

export default RuntimeTreeSelector
