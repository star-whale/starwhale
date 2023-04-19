import React, { useMemo } from 'react'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import { CustomColumn, StringColumn } from '@starwhale/ui/base/data-table'
import _ from 'lodash'
import IconFont from '@starwhale/ui/IconFont'
import { useEvaluationCompareStore } from '@starwhale/ui/base/data-table/store'
import { longestCommonSubstring } from '@/utils'
import { RecordListVo } from '@starwhale/core/datastore/schemas/datastore'
import { LabelSmall } from 'baseui/typography'
import Checkbox from '@starwhale/ui/Checkbox'
import { createUseStyles } from 'react-jss'
import cn from 'classnames'
import { DataTypes, isBoolType, isComplexType, isNumbericType, isSearchColumns, isStringType } from '@starwhale/core'
import { GridTable } from '@starwhale/ui/GridTable'
import { sortColumn } from '@starwhale/ui/GridDatastoreTable'
import useTranslation from '@/hooks/useTranslation'

const useStyles = createUseStyles({
    header: { display: 'flex', alignItems: 'center', justifyContent: 'space-between' },
    headerTitle: {
        fontWeight: '600',
        display: 'flex',
        alignItems: 'center',
        fontSize: '14px',
        color: 'rgba(2,16,43,0.60);',
    },
    headerBar: {
        gap: 20,
        height: '52px',
        lineHeight: '1',
        fontWeight: 'bold',
        display: 'flex',
        alignItems: 'center',
        fontSize: '14px',
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
    tableWrapper: {
        flexGrow: 1,
    },
})

type RowT = {
    key: string
    title: string
    name: string
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

function val(r: any) {
    if (r === undefined) return ''
    if (typeof r === 'object' && 'value' in r) {
        return typeof r.value === 'object' ? JSON.stringify(r.value, null) : r.value
    }

    return r
}
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const NoneCompareCell = ({ value, comparedValue, renderedValue, data }: CellT<{ value: any }>) => {
    return (
        <div title={val(renderedValue)} style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {val(renderedValue)}
        </div>
    )
}
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const NumberCompareCell = ({ value, comparedValue, renderedValue, data }: CellT<{ value: any }>) => {
    const valueV = Number(val(value))
    const comparedValueV = Number(val(comparedValue))

    return (
        <div title={val(renderedValue)} style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {val(renderedValue)}{' '}
            {isValidValue(val(renderedValue)) && valueV > comparedValueV && (
                <IconFont type='rise' style={{ color: '#00B368' }} />
            )}
            {isValidValue(val(renderedValue)) && valueV < comparedValueV && (
                <IconFont type='decline' style={{ color: '#CC3D3D' }} />
            )}
        </div>
    )
}
// eslint-disable-next-line @typescript-eslint/no-unused-vars
const StringCompareCell = ({ value, comparedValue, renderedValue, data }: CellT<{ value: any }>) => {
    const longestCommonString = longestCommonSubstring(val(value), val(comparedValue))
    const index = val(value).indexOf(longestCommonString)
    const front = val(value).substring(0, index)
    const end = val(value).substring(index + longestCommonString.length, val(value).length)
    return (
        <div title={val(renderedValue)} style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
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

function MixedCompareCell({ value, comparedValue, renderedValue, data }: CellT<{ value: any; type: DataTypes }>) {
    // too long string not compare
    if (!comparedValue || !value || val(value).length > 100)
        return NoneCompareCell({ value, comparedValue, renderedValue, data })

    if (isNumbericType(comparedValue.type) || isBoolType(comparedValue.type)) {
        return NumberCompareCell({ value, comparedValue, renderedValue, data })
    }
    if (isStringType(comparedValue.type) || isComplexType(comparedValue.type)) {
        return StringCompareCell({ value, comparedValue, renderedValue, data })
    }
    return NoneCompareCell({ value, comparedValue, renderedValue, data })
}

export default function EvaluationListCompare({
    rows = [],
    attrs,
    title = '',
    getId = (r) => r.id,
}: {
    title?: string
    rows: any[]
    attrs: RecordListVo['columnTypes']
}) {
    const store = useEvaluationCompareStore()
    const [t] = useTranslation()
    const { comparePinnedKey, compareShowCellChanges, compareShowDiffOnly } = store.compare ?? {}
    const styles = useStyles()

    React.useEffect(() => {
        const row = rows.find((r) => val(r.id) === store.compare?.comparePinnedKey)
        const pinKey = row ? store.compare?.comparePinnedKey : val(rows[0].id)
        if (
            !_.isEqual(
                store.rowSelectedIds,
                rows.map((r) => val(r.id))
            )
        ) {
            store.onSelectMany(rows.map((v) => val(v.id)))
        }
        if (!store.isInit) {
            store.onCompareUpdate({
                compareShowCellChanges: true,
                compareShowDiffOnly: false,
            })
        }
        if (pinKey !== store.compare?.comparePinnedKey) {
            store.onCompareUpdate({
                comparePinnedKey: row ? store.compare?.comparePinnedKey : val(rows[0].id),
            })
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [rows])

    const comparePinnedRow: any = useMemo(() => {
        return rows.find((r) => val(r.id) === comparePinnedKey) ?? rows[0]
    }, [rows, comparePinnedKey])

    const comparePinnedRowIndex = useMemo(() => {
        return Math.max(
            rows.findIndex((r) => val(r.id) === comparePinnedKey),
            0
        )
    }, [rows, comparePinnedKey])

    console.log('comparePinnedKey', comparePinnedKey, comparePinnedRow, comparePinnedRowIndex)

    const $rowWithAttrs = useMemo(() => {
        const rowWithAttrs: RowT[] = []

        attrs?.forEach((attr: { name: string }) => {
            const values = rows.map((data: any) => _.get(data, [attr.name]))

            if (attr.name.endsWith('time')) {
                rowWithAttrs.push({
                    key: attr.name,
                    title: attr.name,
                    name: attr.name,
                    values,
                    renderValue: (v: any) => (val(v) > 0 ? formatTimestampDateTime(val(v)) : '-'),
                })
                return
            }
            if (attr.name.includes('duration')) {
                rowWithAttrs.push({
                    key: attr.name,
                    title: attr.name,
                    name: attr.name,
                    values,
                    renderValue: (v: any) => (_.isNumber(val(v)) ? durationToStr(val(v)) : '-'),
                })
                return
            }
            rowWithAttrs.push({
                key: attr.name,
                title: attr.name,
                name: attr.name,
                values,
            })
        })

        return rowWithAttrs.sort(sortColumn).filter((r) => isSearchColumns(r.name))
    }, [attrs, rows])

    const $rowsWithDiffOnly = useMemo(() => {
        if (!compareShowDiffOnly) return $rowWithAttrs
        return $rowWithAttrs.filter(({ values }) => {
            const firstValue = val(values[0])
            return values.some((v) => val(v) !== firstValue)
        })
    }, [$rowWithAttrs, compareShowDiffOnly])

    const $columns = useMemo(
        () => [
            StringColumn({
                key: 'Metrics',
                title: t('compare.column.metrics'),
                pin: 'LEFT',
                minWidth: 200,
                fillWidth: false,
                mapDataToValue: (item: any) => item.title,
            }),
            ...rows.map((row: any, index) =>
                CustomColumn({
                    minWidth: 200,
                    key: val(getId(row)),
                    title: [val(row['sys/model_name']), val(row['sys/id'])].join('-'),
                    fillWidth: false,
                    // @ts-ignore
                    renderCell: (props: any) => {
                        const rowLength = $rowsWithDiffOnly?.length ?? 0
                        const data = props.value || {}
                        // eslint-disable-next-line @typescript-eslint/no-unused-vars
                        const { value, renderValue, renderCompare } = data
                        const renderedValue = renderValue ? renderValue(value) : value

                        const newProps = {
                            value,
                            renderedValue,
                            comparedValue: comparePinnedRow?.[data.key],
                            data,
                        }

                        // console.log(newProps, comparePinnedRow, data.key, comparePinnedRow?.[data.key])

                        if (comparePinnedRowIndex === index) {
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

                        if (val(renderedValue) === val(newProps.comparedValue)) {
                            return <div className={cn('cell--eq', styles.cellCompare)}>{NoneCompareCell(newProps)}</div>
                        }

                        if (compareShowCellChanges && comparePinnedRowIndex !== index) {
                            return (
                                <div className={cn('cell--neq', styles.cellCompare, styles.cellNotEqual)}>
                                    {MixedCompareCell(newProps)}
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
        [t, styles, rows, comparePinnedRow, comparePinnedRowIndex, compareShowCellChanges, $rowsWithDiffOnly]
    )

    return (
        <>
            <div className={styles.header}>
                <LabelSmall className={styles.headerTitle}>
                    {title} <span className={styles.compareCount}>{$columns.length}</span>
                </LabelSmall>
                <div className={styles.headerBar}>
                    <Checkbox
                        overrides={{
                            Label: {
                                style: {
                                    fontSize: 'inherit',
                                },
                            },
                        }}
                        checked={store.compare?.compareShowCellChanges}
                        onChange={(e) => {
                            store.onCompareUpdate({
                                // @ts-ignore
                                compareShowCellChanges: e.target.checked,
                            })
                        }}
                    >
                        {t('compare.config.show.changes')}
                    </Checkbox>
                    <Checkbox
                        overrides={{
                            Label: {
                                style: {
                                    fontSize: 'inherit',
                                },
                            },
                        }}
                        checked={store.compare?.compareShowDiffOnly}
                        onChange={(e) => {
                            store.onCompareUpdate({
                                // @ts-ignore
                                compareShowDiffOnly: e.target.checked,
                            })
                        }}
                    >
                        {t('compare.config.show.diff')}
                    </Checkbox>
                </div>
            </div>
            <div className={styles.tableWrapper}>
                <GridTable store={useEvaluationCompareStore} compareable columns={$columns} data={$rowsWithDiffOnly} />
            </div>
        </>
    )
}
