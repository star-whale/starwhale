import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import classNames from 'classnames'
import ConfigViews from '../ConfigViews/ConfigViews'
import ConfigColumns from '../ConfigColumns'
import { useStore } from '../../hooks/useStore'
import React from 'react'
import useGrid from '../../hooks/useGrid'
import Button from '@starwhale/ui/Button'
import { IGridState } from '../../types'

type IToolBarProps = {
    viewable?: boolean
    filterable?: boolean
    searchable?: boolean
    queryable?: boolean
    columnable?: boolean
    // headlineHeight?: number
}

const selector = (s: IGridState) => ({
    currentView: s.currentView,
    rowSelectedIds: s.rowSelectedIds,
    onCurrentViewColumnsChange: s.onCurrentViewColumnsChange,
})

function ToolBar({ viewable, filterable, searchable, queryable, columnable }: IToolBarProps) {
    const [css] = themedUseStyletron()
    const { onCurrentViewColumnsChange } = useStore(selector)
    const headlineRef = React.useRef(null)
    // const [headlineHeight, setHeadlineHeight] = React.useState(64)
    // useResizeObserver(headlineRef, (entries) => {
    //     setHeadlineHeight(entries[0].contentRect.height)
    // })
    // debugger

    const { originalColumns, isAllRuns, changed, currentView, renderConfigQuery, onSave, onSaveAs, selectedRowIds } =
        useGrid()

    const query = React.useMemo(() => {
        return queryable && renderConfigQuery()
    }, [queryable, renderConfigQuery])

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
                                        <Button onClick={() => onSave?.(currentView)}>Save</Button>
                                        &nbsp;&nbsp;
                                    </>
                                )}

                                <Button onClick={() => onSaveAs?.(currentView)}>Save As</Button>
                            </div>
                        )}
                    </div>
                )}
                <div
                    className={css({
                        display: 'flex',
                    })}
                >
                    <div className='table-config-query' style={{ flex: 1 }}>
                        {query}
                    </div>

                    {columnable && (
                        <div className='table-config-column flex-row-center'>
                            <ConfigColumns
                                view={currentView}
                                columns={originalColumns}
                                onColumnsChange={onCurrentViewColumnsChange}
                            />
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}

export { ToolBar }
export default ToolBar
