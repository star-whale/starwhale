import React from 'react'
import { useStore, useStoreApi } from './useStore'
import { ConfigQuery, ConfigQueryInline } from '../components/Query'
import shallow from 'zustand/shallow'
import { IGridState } from '../types'
import { sortColumn } from '../../GridDatastoreTable'
import useGirdData from './useGridData'
import ConfigSimpleQuery from '../components/Query/ConfigSimpleQuery'
import Button from '@starwhale/ui/Button'
import useTranslation from '@/hooks/useTranslation'

const selector = (state: IGridState) => ({
    queries: state.currentView?.queries ?? [],
    onCurrentViewQueriesChange: state.onCurrentViewQueriesChange ?? [],
})

function useGridQuery() {
    const { queries, onCurrentViewQueriesChange: onChange } = useStore(selector, shallow)
    const { columnTypes } = useStoreApi().getState()
    const { originalColumns } = useGirdData()
    const [isSimpleQuery, setIsSimpleQuery] = React.useState(true)
    const [t] = useTranslation()

    const sortedColumnTypes = React.useMemo(() => {
        // @FIXME why columnTypes is frozen?
        return [...(columnTypes ?? [])]?.sort(sortColumn)
    }, [columnTypes])

    const hasFilter = React.useMemo(() => {
        return originalColumns?.find((column) => column.filterable)
    }, [originalColumns])

    const renderConfigQuery = React.useCallback(() => {
        return (
            <div
                className='flex'
                style={{
                    justifyContent: 'space-between',
                    alignItems: 'center',
                    gap: '20px',
                }}
            >
                <div className='flex'>
                    {isSimpleQuery && hasFilter ? (
                        <ConfigSimpleQuery columns={originalColumns} value={queries} onChange={onChange} />
                    ) : (
                        <ConfigQuery value={queries} onChange={onChange} columnTypes={sortedColumnTypes} />
                    )}
                </div>
                {hasFilter && (
                    <Button as='link' onClick={() => setIsSimpleQuery(!isSimpleQuery)}>
                        {!isSimpleQuery ? t('table.config.query.simple') : t('table.config.query.advanced')}
                    </Button>
                )}
            </div>
        )
    }, [originalColumns, queries, onChange, isSimpleQuery, hasFilter, sortedColumnTypes, t])

    const renderConfigQueryInline = React.useCallback(
        ({ width }: { width: number }) => {
            return (
                <ConfigQueryInline value={queries} onChange={onChange} width={width} columnTypes={sortedColumnTypes} />
            )
        },
        [sortedColumnTypes, queries, onChange]
    )

    return {
        renderConfigQuery,
        renderConfigQueryInline,
        value: queries,
        onChange,
    }
}

export { useGridQuery }
export default useGridQuery
