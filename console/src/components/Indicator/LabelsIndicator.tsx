import React, { memo, useCallback, useRef, useState } from 'react'
import {
    StatefulDataTable,
    NumericalColumn,
    StringColumn,
    BatchActionT,
    RowActionT,
    CustomColumn,
} from 'baseui/data-table'
import _ from 'lodash'
import { StatefulTooltip } from 'baseui/tooltip'
import useTranslation from '../../hooks/useTranslation'
import { ILabel, ILabels } from './types'
import useResizeObserver from '../../hooks/window/useResizeObserver'
import BusyLoaderWrapper from '../BusyLoaderWrapper/BusyLoaderWrapper'

export interface ILabelsProps {
    style?: React.CSSProperties
    data: ILabels
    isLoading: boolean
}

function LabelsIndicator({ data: rawData, style, isLoading }: ILabelsProps) {
    const [t] = useTranslation()
    const [key, setKey] = useState(0)
    const wrapperRef = useRef<HTMLDivElement>(null)
    const [width, setWidth] = useState(wrapperRef?.current?.offsetWidth)

    const throttled = useRef(
        _.debounce(() => {
            if (wrapperRef?.current?.offsetWidth !== width) {
                setWidth(wrapperRef?.current?.offsetWidth)
                setKey(key + 1)
            }
        }, 100)
    )

    useResizeObserver(() => {
        throttled.current()
    }, wrapperRef)

    const renderCell = (props: any) => {
        return (
            <StatefulTooltip accessibilityType='tooltip' content={props?.value}>
                <span>{props?.value?.toFixed(4)}</span>
            </StatefulTooltip>
        )
    }

    const columns = [
        StringColumn({
            title: t('Label'),
            mapDataToValue: (data: ILabel) => data.id,
        }),
        CustomColumn({
            title: t('Precision'),
            renderCell,
            mapDataToValue: (data: ILabel) => data.precision,
        }),
        CustomColumn({
            title: t('Recall'),
            renderCell,
            mapDataToValue: (data: ILabel) => data.recall,
        }),
        CustomColumn({
            title: t('F1-score'),
            renderCell,
            mapDataToValue: (data: ILabel) => data['f1-score'],
        }),
        NumericalColumn({
            title: t('Support'),
            mapDataToValue: (data: ILabel) => data.support ?? 0,
        }),
    ]

    const hasAttrbute = useCallback(
        (k: string) => {
            return _.values(rawData).find((item) => k in item)
        },
        [rawData]
    )
    if (hasAttrbute('tp'))
        columns.push(
            NumericalColumn({
                title: t('TP'),
                mapDataToValue: (data: ILabel) => data.tp ?? 0,
            })
        )
    if (hasAttrbute('tn'))
        columns.push(
            NumericalColumn({
                title: t('TN'),
                mapDataToValue: (data: ILabel) => data.tn ?? 0,
            })
        )
    if (hasAttrbute('fp'))
        columns.push(
            NumericalColumn({
                title: t('FP'),
                mapDataToValue: (data: ILabel) => data.fp ?? 0,
            })
        )
    if (hasAttrbute('fn'))
        columns.push(
            NumericalColumn({
                title: t('FN'),
                mapDataToValue: (data: ILabel) => data.fn ?? 0,
            })
        )

    const itemsToRowData: Array<{ id: string; data: ILabel }> = _.values(
        _.map(rawData, (value, rawKey) => {
            return {
                id: rawKey,
                data: {
                    id: rawKey,
                    ...value,
                },
            }
        })
    )

    const rowActions: RowActionT[] = []
    const batchActions: BatchActionT[] = []
    const rows = itemsToRowData

    return (
        <BusyLoaderWrapper loaderType='skeleton' isLoading={isLoading}>
            <div
                ref={wrapperRef}
                key={key}
                style={{ width: '100%', minHeight: 200, height: `${120 + rows.length * 36}px`, ...style }}
            >
                <StatefulDataTable batchActions={batchActions} rowActions={rowActions} columns={columns} rows={rows} />
            </div>
        </BusyLoaderWrapper>
    )
}

export default memo<ILabelsProps>(LabelsIndicator)
