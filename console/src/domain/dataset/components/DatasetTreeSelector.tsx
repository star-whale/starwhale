import { findTreeNode } from '@starwhale/ui/base/tree-view/utils'
import { DynamicSelector, SelectorItemByTree } from '@starwhale/ui/DynamicSelector'
import { TreeNodeData } from '@starwhale/ui/base/tree-view'
import React, { useEffect } from 'react'
import { useFetchDatasetTree } from '../hooks/useFetchDatasetTree'
import DatasetLabel, { getDatastLabel } from './DatasetLabel'
import { themedStyled } from '@starwhale/ui/theme/styletron'
import Button from '@starwhale/ui/Button'
import useTranslation from '@/hooks/useTranslation'
import _ from 'lodash'

const DatasetTreeNode = themedStyled('div', () => ({
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'nowrap',
    height: '24px',
    width: '100%',
}))

export function DatasetTreeSelector(
    props: any & {
        projectId: string
        onDataChange?: (data: any) => void
        getId?: (obj: any) => any
        multiple?: boolean
    }
) {
    const [t] = useTranslation()
    const { projectId, getId = (obj: any) => obj.id, multiple = false } = props
    const datasetInfo = useFetchDatasetTree(projectId)

    useEffect(() => {
        props.onDataChange?.(datasetInfo.data)
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [datasetInfo?.data])

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
                            id: getId(item),
                            label: (
                                <DatasetTreeNode>
                                    <DatasetLabel version={item} dataset={dataset} />
                                    <Button
                                        overrides={{
                                            BaseButton: {
                                                style: {
                                                    flexShrink: 0,
                                                },
                                            },
                                        }}
                                        key='version-history'
                                        kind='tertiary'
                                        onClick={() =>
                                            window.open(
                                                `/projects/${dataset.projectName}/datasets/${dataset.datasetName}/versions/${item.id}/overview`
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
    }, [datasetInfo, projectId, t, getId])

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

export default DatasetTreeSelector
