import * as React from 'react'
import { StatefulPopover, PLACEMENT, TRIGGER_TYPE } from 'baseui/popover'
import { StatefulMenu } from 'baseui/menu'
import IconFont from '../../../IconFont'
import Button from '../../../Button'
import { LocaleContext } from 'baseui/locale'
import { DataTableLocaleT } from '../locale'
import { IGridState } from '../../../GridTable/types'
import { useStore } from '../../../GridTable/hooks/useStore'
import useGridQuery from '@starwhale/ui/GridTable/hooks/useGridQuery'
import useGridConfigColumns from '@starwhale/ui/GridTable/hooks/useGridConfigColumns'

const selector = (s: IGridState) => ({
    queryinline: s.queryinline,
    columnleinline: s.columnleinline,
    onCurrentViewColumnsChange: s.onCurrentViewColumnsChange,
    wrapperRef: s.wrapperRef,
    sortable: s.sortable,
    removable: s.removable,
    selectable: s.selectable,
})

function HeaderBar(props: { wrapperWidth: any }) {
    // @ts-ignore
    const locale: { datatable: DataTableLocaleT } = React.useContext(LocaleContext)
    const { wrapperRef, queryinline, columnleinline, removable, selectable } = useStore(selector)
    const { renderConfigQueryInline } = useGridQuery()
    const { renderConfigColumns } = useGridConfigColumns()
    const [isShowQuery, setIsShowQuery] = React.useState(false)
    const [isShowConfigColumns, setIsShowConfigColumns] = React.useState(false)

    const COLUMN_OPTIONS = React.useMemo(
        () =>
            [
                queryinline && {
                    label: locale.datatable.columnQuery,
                    type: 'query',
                },
                columnleinline && { label: locale.datatable.columnConfig, type: 'column' },
            ].filter(Boolean),
        [queryinline, locale, columnleinline]
    )

    const handleColumnOptionSelect = React.useCallback((option: any) => {
        if (option.type === 'query') {
            setIsShowQuery(true)
        } else if (option.type === 'column') {
            setIsShowConfigColumns(true)
        }
    }, [])

    if (!columnleinline && !queryinline) {
        if (!removable && !selectable) {
            return null
        }
        return <p className='w-38px' />
    }

    return (
        <>
            <div>
                {renderConfigQueryInline({
                    width: props.wrapperWidth,
                    isOpen: isShowQuery,
                    setIsOpen: setIsShowQuery as any,
                    mountNode: wrapperRef?.current,
                })}
                {renderConfigColumns({
                    isAction: false,
                    isOpen: isShowConfigColumns,
                    setIsOpen: setIsShowConfigColumns as any,
                    mountNode: wrapperRef?.current,
                })}
            </div>
            <StatefulPopover
                focusLock
                triggerType={TRIGGER_TYPE.hover}
                placement={PLACEMENT.bottom}
                content={({ close }) => (
                    <StatefulMenu
                        items={COLUMN_OPTIONS}
                        onItemSelect={({ item }) => {
                            handleColumnOptionSelect(item)
                            close()
                        }}
                        overrides={{
                            List: { style: { height: '130px', width: '150px' } },
                            Option: {
                                props: {
                                    getItemLabel: (item: { label: string; type: string }) => {
                                        const icon = {
                                            query: <IconFont type='filter' />,
                                            column: <IconFont type='setting' />,
                                        }

                                        return (
                                            <div className='flex gap-10px items-center'>
                                                {icon?.[item.type as keyof typeof icon]}
                                                {item.label}
                                            </div>
                                        )
                                    },
                                },
                            },
                        }}
                    />
                )}
            >
                {/* usesd for popover postion ref  */}
                <Button as='link'>
                    <div
                        style={{
                            alignItems: 'center',
                            marginLeft: 'auto',
                            right: 0,
                            top: -6,
                            display: 'flex',
                            width: '30px',
                            justifyContent: 'center',
                            marginRight: '8px',
                        }}
                    >
                        <IconFont type='more' />
                    </div>
                </Button>
            </StatefulPopover>
        </>
    )
}

export { HeaderBar }
export default HeaderBar
