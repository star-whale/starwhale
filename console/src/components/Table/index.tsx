import React from 'react'
import { TableProps as BaseTableProps } from 'baseui/table-semantic'
import TableSemantic from './TableSemantic'
import { Skeleton } from 'baseui/skeleton'
import { usePage } from '@/hooks/usePage'
import { IPaginationProps } from '@/components/Table/IPaginationProps'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import { BusyPlaceholder } from '@starwhale/ui'
import { Pagination } from '@starwhale/ui/Pagination'
import { useEventCallback } from '@starwhale/core'
import { PopoverContainer, SingleSelectMenu } from '@starwhale/ui/Popover'
import { useClickAway, useCreation } from 'ahooks'
import { IconTypesT } from '@starwhale/ui/IconFont'
import { expandPadding } from '@starwhale/ui/utils'

function ActionMenu({
    options: renderOptions = [],
    optionFilter = () => true,
    isOpen = false,
    ...rest
}: {
    options: { type: string; label: any }[]
    optionFilter?: (option: any) => boolean
    isOpen?: boolean
    onItemSelect?: (value: string) => void
    placement?: any
}) {
    return (
        <PopoverContainer
            {...rest}
            options={renderOptions.filter(optionFilter)}
            // only option exsit will show popover
            isOpen={isOpen}
            Content={SingleSelectMenu}
            onItemSelect={({ item }) => rest.onItemSelect?.(item.type)}
            contentOverrides={{
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
            }}
        >
            <div />
        </PopoverContainer>
    )
}

export interface ITableProps extends Omit<BaseTableProps, 'data'> {
    paginationProps?: IPaginationProps
    data?: any
    renderActions?: (rowIndex: any) =>
        | {
              icon: IconTypesT | string
              label: any
              access?: boolean
              quickAccess?: boolean
              component: (hasText?: boolean) => any
          }[]
        | undefined
}

export default function Table({ isLoading, columns, data, overrides, paginationProps, renderActions }: ITableProps) {
    const [, theme] = themedUseStyletron()
    const [page, setPage] = usePage()
    const [selectedRowIndex, setSelectedRowIndex] = React.useState<string | number | undefined>(undefined)
    const [rect, setRect] = React.useState<{ left: number; top: number } | null>(null)
    const ref = React.useRef<HTMLElement>(null)

    const handleRowSelect = useEventCallback(({ rowIndex, event }) => {
        setSelectedRowIndex(rowIndex)
        setRect({
            left: event.clientX,
            top: event.clientY,
        })
    })

    useClickAway(() => {
        setSelectedRowIndex(undefined)
    }, ref)

    const actions = useCreation(() => {
        if (!selectedRowIndex || !renderActions) return undefined
        return renderActions?.(selectedRowIndex)
            ?.filter((action) => action.access)
            .map((action) => {
                return {
                    type: action.label,
                    label: action.component(true),
                }
            })
    }, [data, selectedRowIndex])

    return (
        <>
            <ActionMenu
                key={selectedRowIndex}
                isOpen={selectedRowIndex !== undefined}
                options={actions ?? []}
                placement='right'
                // @ts-ignore
                overrides={
                    rect
                        ? {
                              Body: {
                                  props: {
                                      $popoverOffset: {
                                          left: rect?.left + 20,
                                          top: rect?.top - 10,
                                      },
                                  },
                              },
                          }
                        : {}
                }
            />
            <TableSemantic
                isLoading={isLoading}
                columns={columns}
                data={data as any}
                onRowSelect={handleRowSelect}
                overrides={{
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
