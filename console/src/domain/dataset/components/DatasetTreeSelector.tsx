import { findTreeNode } from '@starwhale/ui/base/tree-view/utils'
import { DynamicSelector, SelectorItemByTree } from '@starwhale/ui/DynamicSelector'
import { TreeNodeData } from '@starwhale/ui/base/tree-view'
import React, { useEffect } from 'react'
import { useFetchDatasetTree } from '../hooks/useFetchDatasetTree'
import DatasetLabel, { getDatasetLabel } from './DatasetLabel'
import { themedStyled } from '@starwhale/ui/theme/styletron'
import Button from '@starwhale/ui/Button'
import useTranslation from '@/hooks/useTranslation'
import _ from 'lodash'
import QuickGroup from '@/components/QuickGroup'

const DatasetTreeNode = themedStyled('div', () => ({
    display: 'flex',
    justifyContent: 'space-between',
    alignItems: 'center',
    flexWrap: 'nowrap',
    height: '24px',
    width: 'auto',
}))

export function DatasetTree({ ...props }: any) {
    const [t] = useTranslation()
    const OPTIONS = [
        {
            key: 'latest',
            label: t('dataset.selector.lastest'),
        },
        {
            key: 'current',
            label: t('dataset.selector.current'),
        },
        {
            key: 'guest',
            label: t('dataset.selector.shared'),
        },
        {
            key: 'all',
            label: t('dataset.selector.all'),
        },
    ]
    const SearchSlot = React.memo((args: any) => <QuickGroup options={OPTIONS} {...args} />)

    return <SelectorItemByTree {...props} SearchSlot={SearchSlot} />
}

export function DatasetTreeSelector(
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
                label: (
                    <p className='color-[#02102b] text-14px'>
                        {[dataset.ownerName, dataset.projectName, dataset.datasetName].join('/')}
                    </p>
                ),
                isExpanded: true,
                children:
                    dataset.versions?.map((item) => {
                        return {
                            id: getId(item),
                            label: (
                                <DatasetTreeNode>
                                    <DatasetLabel
                                        version={item}
                                        dataset={dataset}
                                        style={{ fontSize: '14px', marginRight: '4px' }}
                                    />
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
                                labelView: (
                                    <DatasetLabel
                                        version={item}
                                        dataset={dataset}
                                        isProjectShow
                                        style={{ fontSize: '14px' }}
                                    />
                                ),
                                labelTitle: getDatasetLabel(item, dataset),
                                data: dataset,
                                version: item,
                            },
                            isExpanded: true,
                        }
                    }) ?? [],
            }
        })

        return treeData
    }, [datasetInfo, t, getId])

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
                render: DatasetTree as React.FC<any>,
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
