import React from 'react'
import Search from '@starwhale/ui/Search'
import { ColumnT, QueryT } from './types'
import { ColumnSchemaDesc } from '@starwhale/core/datastore'
import { currentQueriesSelector, IStore } from './store'
import { StatefulPopover } from 'baseui/popover'
import IconFont from '../../IconFont'
import Button from '@starwhale/ui/Button'
import { DatastoreMixedTypeSearch } from '@starwhale/ui/Search/Search'

type PropsT = {
    columns: ColumnT[]
    value: QueryT[]
    onChange: (args: QueryT[]) => void
}

function ConfigQuery(props: PropsT) {
    const columnTypes = React.useMemo(() => {
        return props?.columns.filter((column) => column.columnType).map((column) => column.columnType) ?? []
    }, [props.columns])

    return <DatastoreMixedTypeSearch fields={columnTypes} value={props.value} onChange={props.onChange} />
}

function ConfigQueryInline(props: PropsT & { width: number }) {
    const [isOpen, setIsOpen] = React.useState(false)
    return (
        <>
            {isOpen && (
                <StatefulPopover
                    focusLock
                    initialState={{
                        isOpen: true,
                    }}
                    overrides={{
                        Body: {
                            style: {
                                width: `${props.width}px`,
                                /* tricky here: to make sure popover to align with rows, if cell
                                 changes ,this should be reset */
                                marginLeft: '-12px',
                                zIndex: '2 !important',
                            },
                        },
                        Inner: {
                            style: {
                                backgroundColor: '#FFF',
                            },
                        },
                    }}
                    dismissOnEsc={false}
                    dismissOnClickOutside={false}
                    placement='topLeft'
                    content={() => <ConfigQuery {...props} />}
                >
                    <span />
                </StatefulPopover>
            )}

            <Button onClick={() => setIsOpen(!isOpen)} as='link'>
                <div
                    style={{
                        padding: '5px',
                        backgroundColor: isOpen ? '#DBE5F9' : 'transparent',
                        borderRadius: '4px',
                    }}
                >
                    <IconFont type='filter' />
                </div>
            </Button>
        </>
    )
}

function useConfigQuery(store: IStore, { columns, queryable }: { columns: ColumnT[]; queryable: boolean | undefined }) {
    const api = store()
    const value = store(currentQueriesSelector)

    const onChange = React.useCallback((items) => api.onCurrentViewQueriesChange(items), [api])

    const renderConfigQuery = React.useCallback(() => {
        if (!queryable) return null
        return <ConfigQuery columns={columns} value={value} onChange={onChange} />
    }, [queryable, columns, value, onChange])

    const renderConfigQueryInline = React.useCallback(
        ({ width }: { width: number }) => {
            return <ConfigQueryInline columns={columns} value={value} onChange={onChange} width={width} />
        },
        [columns, value, onChange]
    )

    return {
        renderConfigQuery,
        renderConfigQueryInline,
        value,
        columns,
        onChange,
    }
}

export default ConfigQuery
export { useConfigQuery }
