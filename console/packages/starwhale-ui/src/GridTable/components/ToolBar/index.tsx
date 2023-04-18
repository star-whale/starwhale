import { StatefulDataTablePropsT } from '@starwhale/ui/base/data-table/types'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import classNames from 'classnames'
import ConfigViews from '../ConfigViews/ConfigViews'

type IToolBarProps = {
    columns: StatefulDataTablePropsT['columns']
    rows: StatefulDataTablePropsT['rows']
    viewable?: boolean
    filterable?: boolean
    searchable?: boolean
    queryable?: boolean
    columnable?: boolean
    headlineHeight?: number
}

function ToolBar({
    headlineRef,
    viewable,
    filterable,
    searchable,
    queryable,
    columnable,
    headlineHeight = 60,
}: IToolBarProps) {
    const [css] = themedUseStyletron()

    return (
        <div
            data-type='table-toolbar'
            className={css({
                height: `${headlineHeight}px`,
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
                    {viewable && <ConfigViews columns={props.columns} rows={props.rows} />}
                    {filterable && (
                        <FilterOperateMenu
                            filters={store.currentView?.filters ?? []}
                            columns={props.columns}
                            rows={props.rows}
                            onFilterSet={handeFilterSet}
                        />
                    )}

                    {searchable && <QueryInput onChange={onTextQueryChange} />}

                    {viewable && changed && !$rowSelectedIds.size && (
                        <div>
                            {!isAllRuns && (
                                <>
                                    <Button onClick={() => handleSave(store.currentView)}>Save</Button>
                                    &nbsp;&nbsp;
                                </>
                            )}

                            <Button onClick={() => handleSaveAs(store.currentView)}>Save As</Button>
                        </div>
                    )}
                </div>
                <div
                    className={classNames(
                        css({
                            gridTemplateColumns: 'minmax(200px,1fr) auto',
                            display: 'grid',
                            marginBottom: queryable || columnable ? '20px' : '0px',
                        })
                    )}
                >
                    <div className='table-config-query' style={{ flex: 1 }}>
                        {renderConfigQuery()}
                    </div>

                    {columnable && !$rowSelectedIds.size && (
                        <div className='table-config-column flex-row-center'>
                            <ConfigManageColumns view={currentView} columns={props.columns} onApply={handleApply} />
                        </div>
                    )}
                </div>
            </div>
        </div>
    )
}

export { ToolBar }
export default ToolBar
