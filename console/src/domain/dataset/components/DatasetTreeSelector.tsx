import { findTreeNode } from '@starwhale/ui/base/tree-view/utils'
import { DynamicSelector, SelectorItemByTree } from '@starwhale/ui/DynamicSelector'
import { TreeNodeData } from '@starwhale/ui/base/tree-view'
import React from 'react'
import { useFetchDatasetTree } from '../hooks/useFetchDatasetTree'
import { formatTimestampDateTime } from '@/utils/datetime'
import Shared from '@/components/Shared'
import Alias from '@/components/Alias'
import DatasetLabel from './DatasetLabel'

export function DatasetTreeSelector(props: any) {
    const { projectId } = props
    const datasetInfo = useFetchDatasetTree(projectId)
    const treeData = React.useMemo(() => {
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
                            label: <DatasetLabel version={item} dataset={dataset} />,
                            labelView: <DatasetLabel version={item} dataset={dataset} isProjectShow={true} />,
                            labelTitle: '',
                            isExpanded: true,
                        }
                    }) ?? [],
            }
        })

        return treeData
    }, [datasetInfo])

    const options = React.useMemo(() => {
        return [
            {
                id: 'tree',
                info: {
                    data: treeData,
                },
                multiple: true,
                getData: (info: any, id: string) => findTreeNode(info.data, id),
                getDataToLabelView: (data: any) => data?.labelView,
                getDataToLabelTitle: (data: any) => data?.labelTitle,
                getDataToValue: (data: any) => data?.id,
                render: SelectorItemByTree as React.FC<any>,
            },
        ]
    }, [treeData])

    return <DynamicSelector {...props} options={options} />
}

export default DatasetTreeSelector
