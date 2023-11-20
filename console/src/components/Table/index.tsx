import React, { startTransition } from 'react'
import { TableProps as BaseTableProps } from 'baseui/table-semantic'
import TableSemantic from './TableSemantic'
import { Skeleton } from 'baseui/skeleton'
import { usePage } from '@/hooks/usePage'
import { IPaginationProps } from '@/components/Table/IPaginationProps'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import { BusyPlaceholder } from '@starwhale/ui'
import { Pagination } from '@starwhale/ui/Pagination'
import { useEventCallback } from '@starwhale/core'
import { useClickAway, useCreation } from 'ahooks'
import TableActions, { TableActionsT } from '@starwhale/ui/GridTable/components/TableActions'

export interface ITableProps extends Omit<BaseTableProps, 'data'> {
    paginationProps?: IPaginationProps
    data?: any
    renderActions?: (rowIndex: any) => TableActionsT
}

export default function Table({ isLoading, columns, data, overrides, paginationProps, renderActions }: ITableProps) {
    const [, theme] = themedUseStyletron()
    const [page, setPage] = usePage()
    const [selectedRowIndex, setSelectedRowIndex] = React.useState<number | undefined>(undefined)
    const [rowRect, setRowRect] = React.useState<{ left: number; top: number } | null>(null)
    const [rect, setRect] = React.useState<{ left: number; top: number } | undefined>(undefined)
    const [isFocus, setIsFocus] = React.useState(false)
    const ref = React.useRef<HTMLElement>(null)
    const rootRef = React.useRef<HTMLElement>(null)

    useClickAway(() => {
        if (!isFocus) setSelectedRowIndex(undefined)
        setIsFocus(false)
    }, ref)

    const handleRowHighlight = useEventCallback(({ event, rowIndex }) => {
        if (isFocus) return

        startTransition(() => {
            const tr = event.currentTarget.getBoundingClientRect()
            const table = rootRef.current?.getBoundingClientRect()
            if (!table) return

            setSelectedRowIndex(rowIndex)
            setRowRect({
                left: table?.left + table?.width,
                top: tr.top,
            })
        })
    })

    const handleRowSelect = useEventCallback(({ rowIndex, event }) => {
        if (isFocus) {
            setIsFocus(false)
            return
        }

        setIsFocus(true)
        setSelectedRowIndex(rowIndex)
        setRect({
            left: event.clientX,
            top: event.clientY,
        })
    })

    const actions = useCreation(() => {
        if (selectedRowIndex === undefined || !renderActions) return undefined

        return renderActions?.(selectedRowIndex)
    }, [selectedRowIndex, isFocus])

    return (
        <>
            <TableActions
                actions={actions}
                isFocus={isFocus}
                selectedRowIndex={selectedRowIndex}
                focusRect={rect}
                rowRect={rowRect}
            />
            <TableSemantic
                isLoading={isLoading}
                columns={columns}
                data={data}
                onRowSelect={handleRowSelect}
                onRowHighlight={handleRowHighlight}
                overrides={{
                    Root: {
                        props: {
                            ref: rootRef,
                        },
                        style: {
                            flex: 1,
                        },
                    },
                    TableBody: {
                        props: {
                            ref,
                        },
                    },
                    TableBodyRow: {
                        style: {
                            'cursor': 'pointer',
                            'borderRadius': '4px',
                            ':hover': {
                                backgroundColor: '#EBF1FF',
                            },
                            ':hover .pin-button': {
                                display: 'block !important',
                            },
                        },
                    },
                    // @ts-ignore
                    TableHeadCell: {
                        style: {
                            backgroundColor: theme.brandTableHeaderBackground,
                            fontWeight: 'bold',
                            borderBottomWidth: 0,
                            fontSize: 14,
                            lineHeight: '16px',
                            paddingTop: '14px',
                            paddingBottom: '14px',
                            paddingLeft: '20px',
                            paddingRight: '0px',
                        },
                    },
                    TableHeadRow: {
                        style: {
                            borderRadius: '4px',
                        },
                    },
                    TableBodyCell: {
                        style: ({ $rowIndex }) => {
                            return {
                                paddingTop: 0,
                                paddingBottom: 0,
                                paddingLeft: '20px',
                                paddingRight: '0px',
                                lineHeight: '44px',
                                whiteSpace: 'nowrap',
                                textOverflow: 'ellipsis',
                                overflow: 'hidden',
                                borderBottomColor: '#EEF1F6',
                                verticalAlign: 'middle',
                                backgroundColor: isFocus && $rowIndex === selectedRowIndex ? '#DEE9FF' : 'none',
                            }
                        },
                    },
                    ...overrides,
                }}
                loadingMessage={<Skeleton rows={3} height='100px' width='100%' animation />}
                emptyMessage={
                    <div
                        style={{
                            display: 'flex',
                            flexDirection: 'column',
                            alignItems: 'center',
                            justifyContent: 'center',
                            gap: 8,
                            height: 100,
                        }}
                    >
                        <BusyPlaceholder type='notfound' />
                    </div>
                }
            />
            {/* @ts-ignore */}
            {paginationProps && <Pagination {...paginationProps} page={page} setPage={setPage} />}
        </>
    )
}
