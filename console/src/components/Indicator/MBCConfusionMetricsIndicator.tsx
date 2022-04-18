import React, { memo, useCallback, useEffect, useRef, useState } from 'react'
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
import { useWindowResize } from '../../hooks/window/useWindowResize'
import { IMBCConfusionMetric, IMBCConfusionMetrics } from './types'
import useResizeObserver from '../../hooks/window/useResizeObserver'
import BusyLoaderWrapper from '../BusyLoaderWrapper/BusyLoaderWrapper'

export interface IMBCConfusionMetricsProps {
    style?: React.CSSProperties
    data: IMBCConfusionMetrics
    isLoading: boolean
    onSelectRows?: () => void
}

function MBCConfusionMetricsIndicator({ data, style, isLoading }: IMBCConfusionMetricsProps) {
    const [t] = useTranslation()
    const [key, setKey] = useState(0)
    const wrapperRef = useRef<HTMLDivElement>(null)
    const [width, setWidth] = useState(wrapperRef?.current?.offsetWidth)

    const throttled = useRef(
        _.debounce(() => {
            if (wrapperRef?.current?.offsetWidth != width) {
                setWidth(wrapperRef?.current?.offsetWidth)
                setKey(key + 1)
            }
        }, 100)
    )

    useResizeObserver((entries) => {
        throttled.current()
    }, wrapperRef)

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
            _.map(data, function (value, key) {
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
    }, [data])

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
    const batchActions: BatchActionT[] = []

    //TODO: selected rows interactive
    return (
        <BusyLoaderWrapper loaderType='skeleton' isLoading={true}>
            <div ref={wrapperRef} key={key} style={{ width: '100%', height: 130 + rows.length * 36 + `px`, ...style }}>
                <StatefulDataTable batchActions={batchActions} rowActions={rowActions} columns={columns} rows={rows} />
            </div>
        </BusyLoaderWrapper>
    )
}

MBCConfusionMetricsIndicator.displayName = 'MBCConfusionMetricsIndicator'

export default memo<IMBCConfusionMetricsProps>(MBCConfusionMetricsIndicator)
