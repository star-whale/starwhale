import React from 'react'
import { useStore } from './useStore'
import { ConfigQuery, ConfigQueryInline, ExtraPropsT } from '../components/Query'
import { shallow } from 'zustand/shallow'
import { IGridState } from '../types'
import ConfigSimpleQuery from '../components/Query/ConfigSimpleQuery'
import Button from '@starwhale/ui/Button'
import useTranslation from '@/hooks/useTranslation'

const non: any = []
const selector = (state: IGridState) => ({
    queries: state.currentView?.queries || non,
    onCurrentViewQueriesChange: state.onCurrentViewQueriesChange,
    columns: state.columns,
})

function useGridQuery() {
    const { queries, onCurrentViewQueriesChange: onChange, columns } = useStore(selector, shallow)
    const [isSimpleQuery, setIsSimpleQuery] = React.useState(true)
    const [t] = useTranslation()

    const hasFilter = React.useMemo(() => {
        return columns?.find((column) => column.filterable)
    }, [columns])

    const renderConfigQuery = React.useCallback(() => {
        return (
            <>
                <div className='flex justify-between items-center gap-20px'>
                    <div className='flex flex-1'>
                        {isSimpleQuery ? (
                            <ConfigSimpleQuery columns={columns} value={queries} onChange={onChange} />
                        ) : (
                            <ConfigQuery value={queries} onChange={onChange} columns={columns} />
                        )}
                    </div>
                    {hasFilter && (
                        <Button as='link' onClick={() => setIsSimpleQuery(!isSimpleQuery)}>
                            {!isSimpleQuery ? t('table.config.query.simple') : t('table.config.query.advanced')}
                        </Button>
                    )}
                </div>
            </>
        )
    }, [columns, queries, onChange, isSimpleQuery, hasFilter, t])

    const renderConfigQueryInline = React.useCallback(
        (props: ExtraPropsT) => {
            return <ConfigQueryInline {...props} value={queries} onChange={onChange} columns={columns} />
        },
        [columns, queries, onChange]
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
