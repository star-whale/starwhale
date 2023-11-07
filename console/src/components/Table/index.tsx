import React, { startTransition } from 'react'
import { TableProps as BaseTableProps } from 'baseui/table-semantic'
import TableSemantic from './TableSemantic'
import { Skeleton } from 'baseui/skeleton'
import { usePage } from '@/hooks/usePage'
import { IPaginationProps } from '@/components/Table/IPaginationProps'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import { BusyPlaceholder, ExtendButton } from '@starwhale/ui'
import { Pagination } from '@starwhale/ui/Pagination'
import { useEventCallback } from '@starwhale/core'
import { PopoverContainer, SingleSelectMenu } from '@starwhale/ui/Popover'
import { useClickAway, useCreation } from 'ahooks'
import { expandPadding } from '@starwhale/ui/utils'
import { StatefulPopover } from 'baseui/popover'

const listOverrides = {
    ListItem: {
        style: {
            display: 'flex',
            justifyContent: 'start',
            ...expandPadding('0px', '0px', '0px', '0px'),
        },
    },
    List: {
        style: {
            minWidth: '110px',
        },
    },
}

const getPopoverOverrides = ({ left, top }) => {
    if (!left || !top) return {}

    return {
        Body: {
            props: {
                className: 'filter-popover',
                $popoverOffset: {
                    left,
                    top,
                },
            },
            style: {
                zIndex: 999,
            },
        },
    }
}

function ActionMenu({
    options: renderOptions = [],
    optionFilter = () => true,
    isOpen = false,
    children,
    Content,
    ...rest
}: {
    options: { type: string | number; label: any }[]
    optionFilter?: (option: any) => boolean
    isOpen?: boolean
    onItemSelect?: (value: string) => void
    placement?: any
    children?: React.ReactNode
    Content?: React.FC<any>
}) {
    return (
        <PopoverContainer
            {...rest}
            options={renderOptions.filter(optionFilter)}
            isOpen={isOpen}
            Content={Content ?? SingleSelectMenu}
            onItemSelect={({ item }) => rest.onItemSelect?.(item.type)}
            contentOverrides={listOverrides}
        >
            {children}
        </PopoverContainer>
    )
}

export interface ITableProps extends Omit<BaseTableProps, 'data'> {
    paginationProps?: IPaginationProps
    data?: any
    renderActions?: (rowIndex: any) =>
        | {
              access?: boolean
              quickAccess?: boolean
              component: React.FC<{ hasText?: boolean }>
          }[]
        | undefined
}

export default function Table({ isLoading, columns, data, overrides, paginationProps, renderActions }: ITableProps) {
    const [, theme] = themedUseStyletron()
    const [page, setPage] = usePage()
    const [selectedRowIndex, setSelectedRowIndex] = React.useState<string | number | undefined>(undefined)
    const [rowRect, setRowRect] = React.useState<{ left: number; top: number } | null>(null)
    const [rect, setRect] = React.useState<{ left: number; top: number } | undefined>(undefined)
    const [isFocus, setIsFocus] = React.useState(false)
    const ref = React.useRef<HTMLElement>(null)

    useClickAway(() => {
        setIsFocus(false)
        setSelectedRowIndex(undefined)
    }, ref)

    const actions = useCreation(() => {
        if (selectedRowIndex === undefined || !renderActions) return undefined

        return renderActions?.(selectedRowIndex)
            ?.filter((action) => action.access)
            .map((action, index) => {
                return {
                    type: index,
                    label: <action.component key={index} hasText />,
                }
            })
    }, [data, selectedRowIndex, isFocus])

    const [quickActions, popoverActions] = useCreation(() => {
        if (selectedRowIndex === undefined || !renderActions) return []
        const _actions = renderActions?.(selectedRowIndex)

        return [
            _actions
                ?.filter((action) => action.access && action.quickAccess)
                .map((action, index) => {
                    return <action.component key={index} />
                }),
            _actions
                ?.filter((action) => action.access && !action.quickAccess)
                .map((action, index) => {
                    return {
                        type: index,
                        label: <action.component key={index} hasText />,
                    }
                }),
        ]
    }, [data, selectedRowIndex, isFocus])

    const offset = React.useMemo(() => {
        const len = quickActions?.length ?? 0
        return {
            left: -1 * (len + 1) * 33 - 10,
            top: 5,
        }
    }, [quickActions])

    const handleRowHighlight = useEventCallback(({ event, rowIndex }) => {
        if (isFocus) return

        startTransition(() => {
            const tr = event.currentTarget.getBoundingClientRect()
            setSelectedRowIndex(rowIndex)
            setRowRect({
                left: tr.left + tr.width + offset.left,
                top: tr.top + offset.top,
            })
        })
    })

    const handleRowSelect = useEventCallback(({ rowIndex, event }) => {
        setIsFocus(true)
        setSelectedRowIndex(rowIndex)
        setRect({
            left: event.clientX,
            top: event.clientY + offset.top,
        })
    })

    return (
        <>
            {renderActions && (
                <ActionMenu
                    key={selectedRowIndex}
                    isOpen={selectedRowIndex !== undefined && isFocus}
                    options={actions ?? []}
                    placement='right'
                    // @ts-ignore
                    overrides={getPopoverOverrides(rect || {})}
                />
            )}
            {renderActions && (
                <ActionMenu
                    // key={selectedRowIndex}
                    isOpen={selectedRowIndex !== undefined && !isFocus}
                    options={popoverActions ?? []}
                    placement='right'
                    // @ts-ignore
                    overrides={getPopoverOverrides(rowRect || {})}
                    Content={() => {
                        return (
                            <div className='f-c-c'>
                                {quickActions}{' '}
                                <StatefulPopover
                                    triggerType='hover'
                                    placement='bottom'
                                    overrides={{
                                        Body: {
                                            props: {
                                                className: 'filter-popover',
                                            },
                                            style: {
                                                marginTop: '0px',
                                            },
                                        },
                                    }}
                                    content={() => {
                                        return <SingleSelectMenu overrides={listOverrides} options={popoverActions} />
                                    }}
                                >
                                    <ExtendButton icon='more' styleas={['highlight']} />
                                </StatefulPopover>
                            </div>
                        )
                    }}
                />
            )}
            <TableSemantic
                isLoading={isLoading}
                columns={columns}
                data={data}
                onRowSelect={handleRowSelect}
                onRowHighlight={handleRowHighlight}
                overrides={{
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
                                backgroundColor: $rowIndex === selectedRowIndex ? '#DEE9FF' : 'none',
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
