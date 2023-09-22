import React from 'react'
import { Pagination as BasePagination, SIZE as PaginationSize } from 'baseui/pagination'
import { usePage } from '@/hooks/usePage'
import useTranslation from '@/hooks/useTranslation'
import { expandBorder, expandBorderRadius, expandPadding } from '../utils'
import Select from '../Select'
import IconFont from '../IconFont'
import Button from '../Button'

export type IPaginationProps = {
    total?: number
    start?: number
    count?: number
    maxPage?: number
    afterPageChange?: (pageNum: number) => void
    page?: any
    setPage?: any
}

const PAGE_SIZE = [20, 50, 100]

function PageSizeSelector({ value, onChange }: any) {
    const [t] = useTranslation()
    const $PAGE_SIZE = [...PAGE_SIZE]

    if (PAGE_SIZE.indexOf(value) === -1) {
        $PAGE_SIZE.push(value)
        $PAGE_SIZE.sort((a, b) => a - b)
    }

    const options = $PAGE_SIZE.map((n) => ({ id: n, label: t('pagination.count.per.page', [n]) }))

    return (
        <Select
            backspaceClearsInputValue={false}
            searchable={false}
            clearable={false}
            overrides={{
                Root: {
                    style: {
                        width: 'fit-content',
                        ...expandBorder('0px'),
                    },
                },
                ControlContainer: {
                    style: {
                        ...expandBorder('0px'),
                    },
                },
                ValueContainer: {
                    style: {
                        ...expandPadding('3px', '8px', '3px', '8px'),
                    },
                },
                SelectArrow: ({ $isOpen }) => {
                    return (
                        <IconFont
                            type='arrow2'
                            style={{
                                color: '#000',
                                transform: $isOpen ? 'rotate(180deg)' : 'rotate(0deg)',
                                transition: 'transform 0.2s ease',
                            }}
                        />
                    )
                },
            }}
            options={options}
            onChange={({ option }) => {
                if (!option) {
                    return
                }
                onChange?.(option.id as string)
            }}
            value={[{ id: value }]}
        />
    )
}

function Pagination(paginationProps: IPaginationProps) {
    const [t] = useTranslation()

    const { total, start, count, page, setPage, maxPage } = paginationProps || {}

    const numPages = maxPage ?? (total && count ? Math.ceil(total / Math.max(count, 1)) : 1)

    const currentPage = start ?? 1

    return (
        <div className='pagination flex items-center mt-20px gap-20px lh-20px'>
            <div className='flex-1' />
            {Boolean(total) && (
                <div className='pagination-counts color-[rgba(2,16,43,0.60)] py-3px'>
                    {t('pagination.total.counts', [total])}
                </div>
            )}
            <div className='pagination-pagesize mr-[-12px]'>
                <PageSizeSelector
                    value={count}
                    onChange={(num) => {
                        setPage?.({
                            ...page,
                            ...(total ? { pageNum: Math.min(page.pageNum, Math.ceil(total / num)) } : {}),
                            pageSize: num,
                        })
                    }}
                />
            </div>
            <BasePagination
                overrides={{
                    PrevButton: (args) => {
                        const { $disabled, children, ...rest } = args
                        return <Button {...rest} kind='tertiary' disabled={$disabled} icon='arrow_left' />
                    },
                    NextButton: (args) => {
                        const { $disabled, children, ...rest } = args
                        return <Button {...rest} kind='tertiary' disabled={$disabled} icon='arrow_right' />
                    },
                    DropdownContainer: {
                        style: {
                            ...expandBorder('1px', 'solid', '#CFD7E6'),
                            ...expandBorderRadius('4px'),
                            outline: 'none',
                            marginLeft: '12px',
                        },
                    },
                    Select: {
                        props: {
                            overrides: {
                                ControlContainer: {
                                    style: {
                                        ...expandBorder('0px'),
                                    },
                                },
                                ValueContainer: {
                                    style: {
                                        ...expandPadding('2px', '4px', '2px', '12px'),
                                        fontSize: '14px',
                                    },
                                },
                                SelectArrow: ({ $isOpen }) => {
                                    return (
                                        <IconFont
                                            type='arrow2'
                                            style={{
                                                color: '#000',
                                                transform: $isOpen ? 'rotate(180deg)' : 'rotate(0deg)',
                                                transition: 'transform 0.2s ease',
                                            }}
                                        />
                                    )
                                },
                            },
                        },
                    },
                    ...(total
                        ? {}
                        : {
                              MaxLabel: {
                                  component: () => <></>,
                              },
                          }),
                }}
                size={PaginationSize.mini}
                numPages={numPages}
                currentPage={currentPage}
                onPageChange={({ nextPage }: any) => {
                    const next = {
                        ...page,
                        pageNum: nextPage,
                    }
                    setPage?.(next)
                    if (paginationProps.afterPageChange) {
                        paginationProps.afterPageChange(next)
                    }
                }}
            />
        </div>
    )
}

function StatefulPagination(paginationProps: IPaginationProps) {
    const [page, setPage] = usePage()

    return <Pagination {...paginationProps} page={page} setPage={setPage} />
}

export { Pagination, StatefulPagination }
export default Pagination
