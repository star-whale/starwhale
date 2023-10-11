import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import classNames from 'classnames'
import ConfigViews from '../ConfigViews/ConfigViews'
import React from 'react'
import useGrid from '../../hooks/useGrid'
import Button from '@starwhale/ui/Button'
import useTranslation from '@/hooks/useTranslation'

type IToolBarProps = {
    viewable?: boolean
    filterable?: boolean
    searchable?: boolean
    queryable?: boolean
    columnable?: boolean
    // headlineHeight?: number
}

function ToolBar({ viewable, filterable, searchable, queryable, columnable }: IToolBarProps) {
    const [css] = themedUseStyletron()
    const headlineRef = React.useRef(null)
    // const [headlineHeight, setHeadlineHeight] = React.useState(64)
    // useResizeObserver(headlineRef, (entries) => {
    //     setHeadlineHeight(entries[0].contentRect.height)
    // })
    // debugger

    const {
        isAllRuns,
        changed,
        currentView,
        renderConfigQuery,
        renderStatefulConfigColumns,
        onSave,
        onSaveAs,
        selectedRowIds,
    } = useGrid()

    const query = React.useMemo(() => {
        return queryable && renderConfigQuery()
    }, [queryable, renderConfigQuery])
    const [t] = useTranslation()

    return (
        <div
            data-type='table-toolbar'
            className={css({
                display: viewable || filterable || searchable || queryable || columnable ? 'block' : 'none',
            })}
        >
            <div
                ref={headlineRef}
                className='flex-column flex-row-left g-20'
                style={{ paddingBottom: '8px', gap: '12px' }}
            >
                {viewable && (
                    <div
                        className={classNames(
                            'g-20',
                            css({
                                display: 'grid',
                                gridTemplateColumns: 'minmax(200px, 280px) auto auto',
                            })
                        )}
                    >
                        <ConfigViews />
                        {changed && !selectedRowIds.size && (
                            <div>
                                {!isAllRuns && (
                                    <>
                                        <Button onClick={() => onSave?.(currentView)}> {t('grid.view.save')}</Button>
                                        &nbsp;&nbsp;
                                    </>
                                )}

                                <Button onClick={() => onSaveAs?.(currentView)}> {t('grid.view.saveas')}</Button>
                            </div>
                        )}
                    </div>
                )}
                <div className='flex'>
                    <div className='table-config-query' style={{ flex: 1, minWidth: 0 }}>
                        {query}
                    </div>

                    {columnable && (
                        <div className='table-config-column flex-row-center'>{renderStatefulConfigColumns()}</div>
                    )}
                </div>
            </div>
        </div>
    )
}

export { ToolBar }
export default ToolBar
