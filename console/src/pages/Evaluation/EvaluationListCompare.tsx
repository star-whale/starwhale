import React, { useMemo } from 'react'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import { CustomColumn, StringColumn } from '@starwhale/ui/base/data-table'
import _ from 'lodash'
import IconFont from '@starwhale/ui/IconFont'
import { useEvaluationCompareStore } from '@starwhale/ui/base/data-table/store'
import { longestCommonSubstring } from '@/utils'
import { RecordListVO } from '@starwhale/core/datastore/schemas/datastore'
import { LabelSmall } from 'baseui/typography'
import Checkbox from '@starwhale/ui/Checkbox'
import { createUseStyles } from 'react-jss'
import cn from 'classnames'
import { DataTypes } from '@starwhale/core'
import { GridTable } from '@starwhale/ui/GridTable'

const useStyles = createUseStyles({
    header: {},
    headerTitle: {
        fontWeight: 'bold',
        display: 'flex',
        alignItems: 'center',
    },
    headerBar: {
        gap: 20,
        height: '52px',
        lineHeight: '1',
        marginTop: '35px',
        fontWeight: 'bold',
        display: 'flex',
        alignItems: 'center',
        paddingBottom: '20px',
    },
    cellCompare: {
        position: 'absolute',
        left: 0,
        right: 0,
        padding: '0 12px',
        display: 'flex',
        alignItems: 'center',
        height: '100%',
        width: '100%',
    },
    cellPinned: { borderLeft: '1px dashed blue', borderRight: '1px dashed blue' },
    cellNotEqual: { backgroundColor: '#FFFAF5' },
    compareCount: {
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
    },
})

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

const isValidValue = (str: string) => str !== '-'

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
            {renderedValue}{' '}
            {isValidValue(renderedValue) && value > comparedValue && (
                <IconFont type='rise' style={{ color: '#00B368' }} />
            )}
            {isValidValue(renderedValue) && value < comparedValue && (
                <IconFont type='decline' style={{ color: '#CC3D3D' }} />
            )}
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
    const store = useEvaluationCompareStore()
    const { comparePinnedKey, compareShowCellChanges, compareShowDiffOnly } = store.compare ?? {}
    const styles = useStyles()

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
        return rows.find((r) => r.id === comparePinnedKey) ?? {}
    }, [rows, comparePinnedKey])

    const comparePinnedRowIndex = useMemo(() => {
        return rows.findIndex((r) => r.id === comparePinnedKey)
    }, [rows, comparePinnedKey])

    const $rowWithAttrs = useMemo(() => {
        const rowWithAttrs: RowT[] = []

        attrs?.forEach((attr) => {
            if (attr.name.endsWith('time')) {
                rowWithAttrs.push({
                    key: attr.name,
                    title: attr.name,
                    values: rows.map((data: any) => data?.[attr.name].value),
                    renderValue: (v: any) => (v > 0 ? formatTimestampDateTime(v) : '-'),
                    renderCompare: NoneCompareCell,
                })
                return
            }
            if (attr.name.includes('duration')) {
                rowWithAttrs.push({
                    key: attr.name,
                    title: attr.name,
                    values: rows.map((data: any) => data?.[attr.name].value),
                    renderValue: (v: any) => (_.isNumber(v) ? durationToStr(v) : '-'),
                    renderCompare: NumberCompareCell,
                })
                return
            }

            switch (attr.type) {
                case DataTypes.BOOL:
                case DataTypes.STRING:
                    rowWithAttrs.push({
                        key: attr.name,
                        title: attr.name,
                        values: rows.map((data: any) => data?.[attr.name].value ?? '-'),
                        renderCompare: StringCompareCell,
                    })
                    break
                case DataTypes.INT8:
                case DataTypes.INT16:
                case DataTypes.INT32:
                case DataTypes.INT64:
                case DataTypes.FLOAT16:
                case DataTypes.FLOAT32:
                case DataTypes.FLOAT64:
                    rowWithAttrs.push({
                        key: attr.name,
                        title: attr.name,
                        values: rows.map((data: any) => data?.[attr.name].value ?? '-'),
                        renderCompare: NumberCompareCell,
                    })
                    break
                default:
                    break
            }
        })

        return rowWithAttrs.sort((a: RowT, b: RowT) => {
            if (a.key > b.key) return -1
            return 1
        })
    }, [attrs, rows])

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
                    title: row.id,
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

                        if (comparePinnedKey && comparePinnedRowIndex === index) {
                            return (
                                <div
                                    className={cn('cell--pinned', styles.cellCompare, styles.cellPinned)}
                                    style={{
                                        borderBottom: props.y === rowLength - 1 ? '1px dashed blue' : undefined,
                                    }}
                                >
                                    {NoneCompareCell(newProps)}
                                </div>
                            )
                        }

                        if (renderedValue === newProps.comparedValue) {
                            return <div className={cn('cell--eq', styles.cellCompare)}>{NoneCompareCell(newProps)}</div>
                        }

                        if (compareShowCellChanges && comparePinnedKey && comparePinnedRowIndex !== index) {
                            return (
                                <div className={cn('cell--neq', styles.cellCompare, styles.cellNotEqual)}>
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
        [
            styles,
            rows,
            conmparePinnedRow,
            comparePinnedRowIndex,
            compareShowCellChanges,
            comparePinnedKey,
            $rowsWithDiffOnly,
        ]
    )

    return (
        <>
            <div className={styles.header}>
                <LabelSmall className={styles.headerTitle}>
                    {title} <span className={styles.compareCount}>{$columns.length}</span>
                </LabelSmall>
                <div className={styles.headerBar}>
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
            </div>
            <GridTable store={useEvaluationCompareStore} compareable columns={$columns} data={$rowsWithDiffOnly} />
        </>
    )
}
