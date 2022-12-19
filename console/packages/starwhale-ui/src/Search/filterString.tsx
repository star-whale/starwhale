import { FilterTypeOperators, KIND } from './constants'
// eslint-disable-next-line import/no-cycle
import Filter from './filter'
import { FilterT } from './types'

function FilterString(): FilterT {
    return Filter({
        kind: KIND.STRING,
        operators: FilterTypeOperators[KIND.STRING],
    })
}
export default FilterString
