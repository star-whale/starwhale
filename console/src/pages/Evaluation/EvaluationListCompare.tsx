import React, { useMemo } from 'react'
import { usePage } from '@/hooks/usePage'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import useTranslation from '@/hooks/useTranslation'
import Table from '@/components/Table/TableTyped'
import { useParams } from 'react-router-dom'
import { useFetchJobs } from '@job/hooks/useFetchJobs'
import { CustomColumn, StringColumn } from '@/components/data-table'
import { IEvaluationAttributeValue } from '@/domain/evaluation/schemas/evaluation'
import _ from 'lodash'
import IconFont from '@/components/IconFont'

function longestCommonSubstring(string1: string, string2: string) {
    // Convert strings to arrays to treat unicode symbols length correctly.
    // For example:
    // '𐌵'.length === 2
    // [...'𐌵'].length === 1
    const s1 = string1.split('')
    const s2 = string2.split('')

    // Init the matrix of all substring lengths to use Dynamic Programming approach.
    const substringMatrix = Array(s2.length + 1)
        .fill(null)
        .map(() => {
            return Array(s1.length + 1).fill(null)
        })

    // Fill the first row and first column with zeros to provide initial values.
    for (let columnIndex = 0; columnIndex <= s1.length; columnIndex += 1) {
        substringMatrix[0][columnIndex] = 0
    }

    for (let rowIndex = 0; rowIndex <= s2.length; rowIndex += 1) {
        substringMatrix[rowIndex][0] = 0
    }

    // Build the matrix of all substring lengths to use Dynamic Programming approach.
    let longestSubstringLength = 0
    let longestSubstringColumn = 0
    let longestSubstringRow = 0

    for (let rowIndex = 1; rowIndex <= s2.length; rowIndex += 1) {
        for (let columnIndex = 1; columnIndex <= s1.length; columnIndex += 1) {
            if (s1[columnIndex - 1] === s2[rowIndex - 1]) {
                substringMatrix[rowIndex][columnIndex] = substringMatrix[rowIndex - 1][columnIndex - 1] + 1
            } else {
                substringMatrix[rowIndex][columnIndex] = 0
            }

            // Try to find the biggest length of all common substring lengths
            // and to memorize its last character position (indices)
            if (substringMatrix[rowIndex][columnIndex] > longestSubstringLength) {
                longestSubstringLength = substringMatrix[rowIndex][columnIndex]
                longestSubstringColumn = columnIndex
                longestSubstringRow = rowIndex
            }
        }
    }

    if (longestSubstringLength === 0) {
        // Longest common substring has not been found.
        return ''
    }

    // Detect the longest substring from the matrix.
    let longestSubstring = ''

    while (substringMatrix[longestSubstringRow][longestSubstringColumn] > 0) {
        longestSubstring = s1[longestSubstringColumn - 1] + longestSubstring
        longestSubstringRow -= 1
        longestSubstringColumn -= 1
    }
    return longestSubstring
}

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

const NoneCompareCell = ({ value, comparedValue, renderedValue, data }: CellT<any>) => {
    return (
        <div title={renderedValue} style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {renderedValue}
        </div>
    )
}
const NumberCompareCell = ({ value, comparedValue, renderedValue, data }: CellT<number>) => {
    return (
        <div title={renderedValue} style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {renderedValue} {value > comparedValue && <IconFont type='rise' style={{ color: '#00B368' }} />}{' '}
            {value < comparedValue && <IconFont type='decline' style={{ color: '#CC3D3D' }} />}
        </div>
    )
}
const StringCompareCell = ({ value, comparedValue, renderedValue, data }: CellT<string>) => {
    const longestCommonString = longestCommonSubstring(value, String(comparedValue))
    const arr = value.split(longestCommonString)
    return (
        <div title={renderedValue} style={{ overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap' }}>
            {arr[0]}
            <span
                style={{
                    color: '#E67300 ',
                }}
            >
                {longestCommonString}
            </span>
            {arr[1]}
        </div>
    )
}

export default function EvaluationListCompare({
    rows = [],
    attrs = [],
}: {
    attrs: IEvaluationAttributeValue[]
    rows: any[]
}) {
    const [t] = useTranslation()
    const [page] = usePage()
    const { projectId } = useParams<{ projectId: string }>()
    const evaluationsInfo = useFetchJobs(projectId, page)

    // const results = useQueries(
    //     rows.map((row: any) => ({
    //         queryKey: `fetchJobResult:${projectId}:${row.id}`,
    //         queryFn: () => fetchJobResult(projectId, row.id),
    //         refetchOnWindowFocus: false,
    //     }))
    // )
    const comparePinnedKey = '20'
    const compareShowCellChanges = true
    const conmparePinnedRow = rows.find((row) => row.id === comparePinnedKey)
    const conmparePinnedRowIndex = rows.findIndex((row) => row.id === comparePinnedKey) // +1 for first column being attrs
    const $columns = useMemo(
        () => [
            StringColumn({
                key: 'attrs',
                title: '',
                pin: 'LEFT',
                mapDataToValue: (item: any) => item.title,
            }),
            ...rows.map((row: any, index) =>
                CustomColumn({
                    minWidth: 200,
                    key: String(row.id),
                    title: `${row.modelName}-${row.id}`,
                    // @ts-ignore
                    renderCell: (props: any) => {
                        const data = props.value || {}
                        const { key, title, value, renderValue, renderCompare } = data
                        const renderedValue = renderValue ? renderValue(value) : value
                        const newProps = {
                            value,
                            renderedValue,
                            comparedValue: conmparePinnedRow?.[data.key],
                            data,
                        }
                        if (compareShowCellChanges && comparePinnedKey && conmparePinnedRowIndex != index) {
                            return (
                                <div
                                    style={{
                                        position: 'absolute',
                                        left: 0,
                                        right: 0,
                                        padding: '0 20px',
                                        display: 'flex',
                                        alignItems: 'center',
                                        height: '100%',
                                        width: '100%',
                                        backgroundColor:
                                            renderedValue !== newProps.comparedValue ? '#FFFAF5' : undefined,
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
                        value: values[index],
                    }),
                })
            ),
        ],
        [rows]
    )

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
                    title: t('Runtime'),
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

        attrs.forEach((attr) => {
            if (!attr.name.startsWith('summary/')) {
                return
            }

            const name = attr.name.split('/').slice(1).join('/')

            rowWithAttrs.push({
                key: attr.name,
                title: name,
                values: rows.map((data: any) => {
                    const attrIndex = data.attributes?.findIndex(
                        (row: IEvaluationAttributeValue) => row.name === attr.name
                    )

                    if (attrIndex >= 0) {
                        return data.attributes?.[attrIndex]?.value ?? '-'
                    }

                    return '-'
                }),
                renderCompare: NumberCompareCell,
            })
        })

        return rowWithAttrs
    }, [$rows, attrs, rows])

    return (
        <>
            <Table
                id='compare'
                isLoading={evaluationsInfo.isLoading}
                columns={$columns}
                // @ts-ignore
                data={$rowWithAttrs}
            />
        </>
    )
}
