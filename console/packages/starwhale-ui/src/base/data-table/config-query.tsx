import React from 'react'
import Search from '@starwhale/ui/Search'
import { ColumnT, QueryT } from './types'
import { ColumnSchemaDesc } from '@starwhale/core/datastore'

type PropsT = {
    columns: ColumnT[]
    value: QueryT[]
    onChange: (args: QueryT[]) => void
}

function ConfigQuery(props: PropsT) {
    const columnTypes = React.useMemo(() => {
        return props?.columns.filter((column) => column.columnType).map((column) => column.columnType) ?? []
    }, [props.columns])

    return <Search fields={columnTypes as ColumnSchemaDesc[]} value={props.value} onChange={props.onChange} />
}

export default ConfigQuery
