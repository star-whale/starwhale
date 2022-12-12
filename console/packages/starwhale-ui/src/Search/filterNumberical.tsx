import { Popover } from 'baseui/popover'
import Input from '../Input'
import { FilterTypeOperators, KIND } from './constants'
import Filter from './filter'
import { FilterT } from './types'

function FilterNumberical(): FilterT {
    return Filter({
        kind: KIND.NUMERICAL,
        operators: FilterTypeOperators[KIND.NUMERICAL],
    })
}
export default FilterNumberical
