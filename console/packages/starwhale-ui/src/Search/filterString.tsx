import { FilterTypeOperators, KIND } from './constants'
import Filter from './filter'
import { FilterT } from './types'

function FilterString(): FilterT {
    return Filter({
        kind: KIND.STRING,
        operators: FilterTypeOperators[KIND.STRING],
    })
}
export default FilterString
