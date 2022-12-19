import { FilterTypeOperators, KIND } from './constants'
// eslint-disable-next-line import/no-cycle
import Filter from './filter'
import { FilterT } from './types'

function FilterNumberical(): FilterT {
    return Filter({
        kind: KIND.NUMERICAL,
        operators: FilterTypeOperators[KIND.NUMERICAL],
    })
}
export default FilterNumberical
