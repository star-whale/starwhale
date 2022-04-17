import React, { useEffect } from 'react'
import {
    StatefulDataTable,
    BooleanColumn,
    CategoricalColumn,
    NumericalColumn,
    StringColumn,
    NUMERICAL_FORMATS,
    BatchActionT,
    RowActionT,
} from 'baseui/data-table'
import { Alert, Check } from 'baseui/icon'
import useTranslation from '../../hooks/useTranslation'
import _ from 'lodash'

export interface IMBCConfusionMetric {
    id?: string
    tp: number
    tn: number
    fp: number
    fn: number
    accuracy: number
    precision: number
    recall: number
}

export type IMBCConfusionMetrics = Record<string, IMBCConfusionMetric>
export interface IMBCConfusionMetricsProps {
    style?: React.CSSProperties
    items: IMBCConfusionMetrics
    onSelectRows?: () => void
}

export function MBCConfusionMetricsIndicator({ items, style }: IMBCConfusionMetricsProps) {
    const [t] = useTranslation()

    const columns = [
        StringColumn({
            title: t('Label'),
            mapDataToValue: (data: IMBCConfusionMetric) => data['id'],
        }),
        NumericalColumn({
            title: t('TP'),
            mapDataToValue: (data: IMBCConfusionMetric) => data['tp'],
        }),
        NumericalColumn({
            title: t('TN'),
            mapDataToValue: (data: IMBCConfusionMetric) => data['tn'],
        }),
        NumericalColumn({
            title: t('FP'),
            mapDataToValue: (data: IMBCConfusionMetric) => data['fp'],
        }),
        NumericalColumn({
            title: t('FN'),
            mapDataToValue: (data: IMBCConfusionMetric) => data['fn'],
        }),
        NumericalColumn({
            title: t('Accuracy'),
            mapDataToValue: (data: IMBCConfusionMetric) => data['accuracy'],
        }),
        NumericalColumn({
            title: t('Precision'),
            mapDataToValue: (data: IMBCConfusionMetric) => data['precision'],
        }),
        NumericalColumn({
            title: t('Recall'),
            mapDataToValue: (data: IMBCConfusionMetric) => data['recall'],
        }),
    ]

    const [rows, setRows] = React.useState([] as Array<{ id: string; data: IMBCConfusionMetric }>)

    useEffect(() => {
        const itemsToRowData: Array<{ id: string; data: IMBCConfusionMetric }> = _.values(
            _.map(items, function (value, key) {
                return {
                    id: key,
                    data: {
                        id: key,
                        ...value,
                    },
                }
            })
        )
        setRows(itemsToRowData)
    }, [items])

    function flagRows(ids: Array<string | number>) {
        const nextRows = rows.map((row) => {
            if (ids.includes(row.id)) {
                const nextData = { ...row.data }
                return { ...row, data: nextData }
            }
            return row
        })
        setRows(nextRows)
    }
    function removeRows(ids: Array<string | number>) {
        const nextRows = rows.filter((row) => !ids.includes(row.id))
        setRows(nextRows)
    }
    function flagRow(id: string | number) {
        flagRows([id])
    }
    function removeRow(id: string | number) {
        removeRows([id])
    }
    const rowActions: RowActionT[] = []
    const batchActions: BatchActionT[] = [
        // {
        //     label: 'Check',
        //     onClick: ({ selection, clearSelection }) => {
        //         flagRows(selection.map((r) => r.id))
        //         clearSelection()
        //     },
        //     renderIcon: Check,
        // },
        // {
        //     label: 'Remove',
        //     onClick: ({ selection, clearSelection }) => {
        //         removeRows(selection.map((r) => r.id))
        //         clearSelection()
        //     },
        //     renderIcon: Alert,
        // },
    ]

    //TODO: selected rows interactive
    console.log('rows', rows)
    return (
        <div style={{ height: 300, ...style }}>
            <StatefulDataTable batchActions={batchActions} rowActions={rowActions} columns={columns} rows={rows} />
        </div>
    )
}
