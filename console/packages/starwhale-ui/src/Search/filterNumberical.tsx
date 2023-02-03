import Filter from './filter'
import { FilterT, KIND, FilterTypeOperators } from './types'

function FilterNumberical(): FilterT {
    return Filter({
        kind: KIND.NUMERICAL,
        operators: FilterTypeOperators[KIND.NUMERICAL],
    })
}
export default FilterNumberical
