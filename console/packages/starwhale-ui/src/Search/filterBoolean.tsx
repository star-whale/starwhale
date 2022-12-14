import { Popover } from 'baseui/popover'
import Input from '../Input'
import { FilterTypeOperators, KIND } from './constants'
import Filter from './filter'
import { FilterT } from './types'

function FilterBoolean(): FilterT {
    return Filter({
        kind: KIND.BOOLEAN,
        operators: FilterTypeOperators[KIND.BOOLEAN],
    })
}
export default FilterBoolean
