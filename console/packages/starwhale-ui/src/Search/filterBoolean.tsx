import { FilterTypeOperators, KIND } from './constants'
// eslint-disable-next-line import/no-cycle
import Filter from './filter'
import { FilterT } from './types'

function FilterBoolean(): FilterT {
    return Filter({
        kind: KIND.BOOLEAN,
        operators: FilterTypeOperators[KIND.BOOLEAN],
    })
}
export default FilterBoolean
