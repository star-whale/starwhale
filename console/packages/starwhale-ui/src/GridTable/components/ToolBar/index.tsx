import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import classNames from 'classnames'
import ConfigViews from '../ConfigViews/ConfigViews'
import ConfigColumns from '../ConfigColumns'
import { useStore, useStoreApi } from '../../hooks/useStore'
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
    const { rowSelectedIds, onCurrentViewColumnsChange } = useStore(selector)
    const headlineRef = React.useRef(null)
    // const [headlineHeight, setHeadlineHeight] = React.useState(64)
    // useResizeObserver(headlineRef, (entries) => {
    //     setHeadlineHeight(entries[0].contentRect.height)
    // })

    const { columns, rows } = useStoreApi().getState()
    const { isAllRuns, changed, currentView, renderConfigQuery, onSave, onSaveAs } = useGrid()

    return (
        <div
            data-type='table-toolbar'
            className={css({
                // height: `${headlineHeight}px`,
                display: viewable || filterable || searchable || queryable || columnable ? 'block' : 'none',
            })}
        >
            <div ref={headlineRef} className='flex-row-left g-20' style={{ paddingBottom: '20px' }}>
                <div
                    className={classNames(
                        'g-20 ',
                        css({
                            'display': 'grid',
                            'gridTemplateColumns': 'minmax(200px, 280px) auto auto',
                            ':first-child': {
                                marginBottom: '20px',
                            },
                        })
                    )}
                >
                    {viewable && <ConfigViews columns={columns} rows={rows} />}
                    {viewable && changed && !rowSelectedIds.size && (
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
                    {/* {searchable && <QueryInput onChange={onTextQueryChange} />} */}
                    {/* 
                    {filterable && (
                        <FilterOperateMenu
                            filters={store.currentView?.filters ?? []}
                            columns={props.columns}
                            rows={props.rows}
                            onFilterSet={handeFilterSet}
                        />
                    )} */}
                </div>
                <div
                    className={classNames(
                        css({
                            display: 'flex',
                            // marginBottom: queryable || columnable ? '20px' : '0px',
                        })
                    )}
                >
                    <div className='table-config-query' style={{ flex: 1 }}>
                        {queryable && renderConfigQuery()}
                    </div>

                    {columnable && !rowSelectedIds.size && (
                        <div className='table-config-column flex-row-center'>
                            <ConfigColumns
                                view={currentView}
                                columns={columns}
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
