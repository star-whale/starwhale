import React, { memo, useCallback, useEffect, useMemo, useRef, useState } from 'react'
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
import { ILabel, ILabels } from './types'
import useResizeObserver from '../../hooks/window/useResizeObserver'
import BusyLoaderWrapper from '../BusyLoaderWrapper/BusyLoaderWrapper'

export interface ILabelsProps {
    style?: React.CSSProperties
    data: ILabels
    isLoading: boolean
    onSelectRows?: () => void
}

function LabelsIndicator({ data, style, isLoading }: ILabelsProps) {
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
            mapDataToValue: (data: ILabel) => data['id'],
        }),
        NumericalColumn({
            title: t('Precision'),
            mapDataToValue: (data: ILabel) => data['precision'],
        }),
        NumericalColumn({
            title: t('Recall'),
            mapDataToValue: (data: ILabel) => data['recall'],
        }),
        NumericalColumn({
            title: t('F1-score'),
            mapDataToValue: (data: ILabel) => data['f1-score'],
        }),
        NumericalColumn({
            title: t('Support'),
            mapDataToValue: (data: ILabel) => data['support'],
        }),
    ]

    const hasAttrbute = useCallback(
        (k: string) => {
            return _.values(data).find((item) => k in item)
        },
        [data]
    )
    hasAttrbute('tp') &&
        columns.push(
            NumericalColumn({
                title: t('TP'),
                mapDataToValue: (data: ILabel) => data['tp'],
            })
        )

    hasAttrbute('tn') &&
        columns.push(
            NumericalColumn({
                title: t('TN'),
                mapDataToValue: (data: ILabel) => data['tn'],
            })
        )
    hasAttrbute('fp') &&
        columns.push(
            NumericalColumn({
                title: t('FP'),
                mapDataToValue: (data: ILabel) => data['fp'],
            })
        )
    hasAttrbute('fn') &&
        columns.push(
            NumericalColumn({
                title: t('FN'),
                mapDataToValue: (data: ILabel) => data['fn'],
            })
        )

    const [rows, setRows] = React.useState([] as Array<{ id: string; data: ILabel }>)

    useEffect(() => {
        const itemsToRowData: Array<{ id: string; data: ILabel }> = _.values(
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
        <BusyLoaderWrapper loaderType='skeleton' isLoading={isLoading}>
            <div
                ref={wrapperRef}
                key={key}
                style={{ width: '100%', minHeight: 200, height: 120 + rows.length * 36 + `px`, ...style }}
            >
                <StatefulDataTable batchActions={batchActions} rowActions={rowActions} columns={columns} rows={rows} />
            </div>
        </BusyLoaderWrapper>
    )
}

export default memo<ILabelsProps>(LabelsIndicator)
