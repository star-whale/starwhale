import Search from '@starwhale/ui/Search'
import { ColumnT } from './types'

type PropsT = {
    columns: ColumnT[]
    filters: any[]
    rows: any[]
    onFilterSet?: (filterParams: any[]) => void
    onSave?: (filterParams: any[]) => void
    onSaveAs?: (filterParams: any[]) => void
}

function ConfigQuery(props: PropsT) {
    return <Search fields={[]} />
}

export default ConfigQuery
