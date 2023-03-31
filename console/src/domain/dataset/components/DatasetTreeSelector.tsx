import { findTreeNode } from '@starwhale/ui/base/tree-view/utils'
import { DynamicSelector, SelectorItemByTree } from '@starwhale/ui/DynamicSelector'
import { TreeNodeData } from '@starwhale/ui/base/tree-view'
import React from 'react'
import { useFetchDatasetTree } from '../hooks/useFetchDatasetTree'
import DatasetLabel, { getDatastLabel } from './DatasetLabel'
import { themedStyled } from '@starwhale/ui/theme/styletron'
import Button from '@starwhale/ui/Button'
import useTranslation from '@/hooks/useTranslation'

const DatasetTreeNode = themedStyled('div', () => ({
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'nowrap',
    height: '24px',
    width: '100%',
}))

export function DatasetTreeSelector(props: any) {
    const [t] = useTranslation()
    const { projectId } = props
    const datasetInfo = useFetchDatasetTree(projectId)
    const $treeData = React.useMemo(() => {
        if (!datasetInfo.isSuccess) return []

        const treeData: TreeNodeData[] = datasetInfo.data.map((dataset) => {
            return {
                id: dataset.datasetName,
                label: [dataset.ownerName, dataset.projectName, dataset.datasetName].join('/'),
                isExpanded: true,
                children:
                    dataset.versions?.map((item) => {
                        return {
                            id: item.id,
                            label: (
                                <DatasetTreeNode>
                                    <DatasetLabel version={item} dataset={dataset} />
                                    <Button
                                        key='version-history'
                                        kind='tertiary'
                                        onClick={() =>
                                            window.open(
                                                `/projects/${projectId}/datasets/${dataset.datasetName}/versions/${item.id}/overview`
                                            )
                                        }
                                    >
                                        {t('dataset.selector.view')}
                                    </Button>
                                </DatasetTreeNode>
                            ),
                            info: {
                                labelView: <DatasetLabel version={item} dataset={dataset} isProjectShow />,
                                labelTitle: getDatastLabel(item, dataset),
                            },
                            isExpanded: true,
                        }
                    }) ?? [],
            }
        })

        return treeData
    }, [datasetInfo, projectId, t])

    const options = React.useMemo(() => {
        return [
            {
                id: 'tree',
                info: {
                    data: $treeData,
                },
                multiple: true,
                getData: (info: any, id: string) => findTreeNode(info.data, id),
                getDataToLabelView: (data: any) => data?.info.labelView,
                getDataToLabelTitle: (data: any) => data?.info.labelTitle,
                getDataToValue: (data: any) => data?.id,
                render: SelectorItemByTree as React.FC<any>,
            },
        ]
    }, [$treeData])

    return <DynamicSelector {...props} options={options} />
}

export default DatasetTreeSelector
