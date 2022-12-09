import { Popover } from 'baseui/popover'
import Input from '../Input'
import { FilterTypeOperators, KIND } from './constants'
import Filter from './Filter'
import { FilterT } from './types'

function FilterString(): FilterT {
    return Filter({
        kind: KIND.STRING,
        operators: FilterTypeOperators[KIND.STRING],
    })
}
export default FilterString
