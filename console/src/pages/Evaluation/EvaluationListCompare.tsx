import React, { useMemo } from 'react'
import { usePage } from '@/hooks/usePage'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import Table from '@/components/Table/TableTyped'
import { useParams } from 'react-router-dom'
import { useFetchJobs } from '@job/hooks/useFetchJobs'
import { CustomColumn, StringColumn } from '@/components/data-table'
import _ from 'lodash'
import IconFont from '@/components/IconFont'
import { useEvaluationCompareStore } from '@/components/data-table/store'
import { Checkbox } from 'baseui/checkbox'
import { longestCommonSubstring } from '@/utils'
import { RecordListVO } from '@/domain/datastore/schemas/datastore'
import { LabelSmall } from 'baseui/typography'

type RowT = {
    key: string
    title: string
    values: any[]
    value?: any
    renderValue?: (data: any) => any
    renderCompare?: (data: any) => any
}

type CellT<T> = {
    value: T
    renderedValue: any
    comparedValue: T
    data: any
}

// eslint-disable-next-line @typescript-eslint/no-unused-vars
const NoneCompareCell = ({ value, comparedValue, renderedValue, data }: CellT<any>) => {
    return (
        <div title={renderedValue} style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {renderedValue}
        </div>
    )
}
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const NumberCompareCell = ({ value, comparedValue, renderedValue, data }: CellT<number>) => {
    // eslint-disable-next-line no-param-reassign
    value = Number(value)
    // eslint-disable-next-line no-param-reassign
    comparedValue = Number(comparedValue)
    return (
        <div title={renderedValue} style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {renderedValue} {value > comparedValue && <IconFont type='rise' style={{ color: '#00B368' }} />}{' '}
            {value < comparedValue && <IconFont type='decline' style={{ color: '#CC3D3D' }} />}
        </div>
    )
}
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const StringCompareCell = ({ value, comparedValue, renderedValue, data }: CellT<string>) => {
    const longestCommonString = longestCommonSubstring(value, String(comparedValue))
    const index = value.indexOf(longestCommonString)
    const front = value.substring(0, index)
    const end = value.substring(index + longestCommonString.length, value.length)
    return (
        <div title={renderedValue} style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {front}
            <span
                style={{
                    color: '#E67300 ',
                }}
            >
                {longestCommonString}
            </span>
            {end}
        </div>
    )
}

export default function EvaluationListCompare({
    rows = [],
    attrs,
    title = '',
}: {
    title?: string
    rows: any[]
    attrs: RecordListVO['columnTypes']
}) {
    const [t] = useTranslation()
    const [page] = usePage()
    const { projectId } = useParams<{ projectId: string }>()
    const evaluationsInfo = useFetchJobs(projectId, page)
    const store = useEvaluationCompareStore()
    const { comparePinnedKey, compareShowCellChanges, compareShowDiffOnly } = store.compare ?? {}

    React.useEffect(() => {
        const row = rows.find((r) => r.id === store.compare?.comparePinnedKey)
        const pinKey = row ? store.compare?.comparePinnedKey : rows[0].id
        if (
            !_.isEqual(
                store.rowSelectedIds,
                rows.map((r) => r.id)
            )
        ) {
            store.onSelectMany(rows.map((v) => v.id))
        }
        if (!store.isInit) {
            store.onCompareUpdate({
                compareShowCellChanges: true,
                compareShowDiffOnly: false,
            })
        }
        if (pinKey !== store.compare?.comparePinnedKey) {
            store.onCompareUpdate({
                comparePinnedKey: row ? store.compare?.comparePinnedKey : rows[0].id,
            })
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [rows])

    const conmparePinnedRow: any = useMemo(() => {
        const row = rows.find((r) => r.id === comparePinnedKey) ?? {}
        return {
            ...row,
            ...row.attributes,
        }
    }, [rows, comparePinnedKey])

    const conmparePinnedRowIndex = useMemo(() => {
        return rows.findIndex((r) => r.id === comparePinnedKey)
    }, [rows, comparePinnedKey])

    const $rows = useMemo(
        () =>
            [
                {
                    key: 'uuid',
                    title: t('Evaluation ID'),
                    values: rows.map((data: any) => data.uuid),
                    renderCompare: StringCompareCell,
                },
                {
                    key: 'modelName',
                    title: t('sth name', [t('Model')]),
                    values: rows.map((data: any) => data.modelName),
                    renderCompare: StringCompareCell,
                },
                {
                    key: 'modelVersion',
                    title: t('Version'),
                    values: rows.map((data: any) => data.modelVersion),
                    renderCompare: StringCompareCell,
                },
                {
                    key: 'owner',
                    title: t('Owner'),
                    values: rows.map((data: any) => data.owner),
                    renderCompare: StringCompareCell,
                },
                {
                    key: 'createdTime',
                    title: t('Created'),
                    values: rows.map((data: any) => data.createdTime),
                    renderValue: (v: any) => formatTimestampDateTime(v),
                    renderCompare: NoneCompareCell,
                },
                {
                    key: 'duration',
                    title: t('Elapsed Time'),
                    values: rows.map((data: any) => data.duration),
                    renderValue: (v: any) => (_.isNumber(v) ? durationToStr(v) : '-'),
                    renderCompare: NumberCompareCell,
                },
                {
                    key: 'stopTime',
                    title: t('End Time'),
                    values: rows.map((data: any) => data.stopTime),
                    renderValue: (v: any) => (v > 0 ? formatTimestampDateTime(v) : '-'),
                    renderCompare: NoneCompareCell,
                },
            ] as RowT[],
        [t, rows]
    )

    const $rowWithAttrs = useMemo(() => {
        const rowWithAttrs = [...$rows]

        attrs?.forEach((attr) => {
            rowWithAttrs.push({
                key: attr.name,
                title: attr.name,
                values: rows.map((data: any) => data.attributes?.[attr.name] ?? '-'),
                renderCompare: NumberCompareCell,
            })
        })

        return rowWithAttrs
    }, [$rows, attrs, rows])

    const $rowsWithDiffOnly = useMemo(() => {
        if (!compareShowDiffOnly) return $rowWithAttrs

        return $rowWithAttrs.filter((row) => new Set(row.values).size !== 1)
    }, [$rowWithAttrs, compareShowDiffOnly])

    const $columns = useMemo(
        () => [
            StringColumn({
                key: 'attrs',
                title: '',
                pin: 'LEFT',
                minWidth: 200,
                mapDataToValue: (item: any) => item.title,
            }),
            ...rows.map((row: any, index) =>
                CustomColumn({
                    minWidth: 200,
                    key: String(row.id),
                    title: `${row.modelName}-${row.id}`,
                    // @ts-ignore
                    renderCell: (props: any) => {
                        const rowLength = $rowsWithDiffOnly.length
                        const data = props.value || {}
                        // eslint-disable-next-line @typescript-eslint/no-unused-vars
                        const { value, renderValue, renderCompare } = data
                        const renderedValue = renderValue ? renderValue(value) : value
                        const newProps = {
                            value,
                            renderedValue,
                            comparedValue: conmparePinnedRow?.[data.key],
                            data,
                        }

                        if (comparePinnedKey && conmparePinnedRowIndex === index) {
                            return (
                                <div
                                    style={{
                                        position: 'absolute',
                                        left: 0,
                                        right: 0,
                                        padding: '0 12px',
                                        display: 'flex',
                                        alignItems: 'center',
                                        height: '100%',
                                        width: '100%',
                                        borderLeft: '1px dashed blue',
                                        borderRight: '1px dashed blue',
                                        borderBottom: props.y === rowLength - 1 ? '1px dashed blue' : undefined,
                                    }}
                                >
                                    {NoneCompareCell(newProps)}
                                </div>
                            )
                        }

                        if (renderedValue === newProps.comparedValue) {
                            return NoneCompareCell(newProps)
                        }

                        if (compareShowCellChanges && comparePinnedKey && conmparePinnedRowIndex !== index) {
                            return (
                                <div
                                    style={{
                                        position: 'absolute',
                                        left: 0,
                                        right: 0,
                                        padding: '0 12px',
                                        display: 'flex',
                                        alignItems: 'center',
                                        height: '100%',
                                        width: '100%',
                                        backgroundColor: '#FFFAF5',
                                    }}
                                >
                                    {renderCompare(newProps)}
                                </div>
                            )
                        }
                        return NoneCompareCell(newProps)
                    },
                    mapDataToValue: ({ values, ...item }: any) => ({
                        ...item,
                        values,
                        index,
                        value: values[index],
                    }),
                })
            ),
        ],
        [rows, conmparePinnedRow, conmparePinnedRowIndex, compareShowCellChanges, comparePinnedKey, $rowsWithDiffOnly]
    )

    return (
        <>
            <div style={{ display: 'flex', alignItems: 'center', height: '36px', gap: 20 }}>
                <LabelSmall $style={{ fontWeight: 'bold', display: 'flex', alignItems: 'center' }}>
                    {title}{' '}
                    <span
                        style={{
                            display: 'inline-block',
                            borderRadius: '12px',
                            background: '#F0F5FF',
                            width: '26px',
                            height: '18px',
                            lineHeight: '18px',
                            textAlign: 'center',
                            color: 'rgba(2,16,43,0.60)',
                            fontSize: '12px',
                            marginLeft: '8px',
                        }}
                    >
                        {$columns.length}
                    </span>
                </LabelSmall>
                <Checkbox
                    checked={store.compare?.compareShowCellChanges}
                    onChange={(e) => {
                        store.onCompareUpdate({
                            // @ts-ignore
                            compareShowCellChanges: e.target.checked,
                        })
                    }}
                >
                    Show cell changes
                </Checkbox>
                <Checkbox
                    checked={store.compare?.compareShowDiffOnly}
                    onChange={(e) => {
                        store.onCompareUpdate({
                            // @ts-ignore
                            compareShowDiffOnly: e.target.checked,
                        })
                    }}
                >
                    Rows with diff only
                </Checkbox>
            </div>
            <Table
                useStore={useEvaluationCompareStore}
                isLoading={evaluationsInfo.isLoading}
                columns={$columns}
                compareable
                // @ts-ignore
                data={$rowsWithDiffOnly}
            />
        </>
    )
}
