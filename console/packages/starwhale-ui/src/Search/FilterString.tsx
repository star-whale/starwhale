import { Popover } from 'baseui/popover'
import Input from '../Input'
import { FilterTypeOperators, KIND } from './constants'
import Filter from './Filter'
import { FilterT } from './types'

function FilterString(): FilterT {
    return Filter({
        kind: KIND.STRING,
        operators: FilterTypeOperators[KIND.STRING],
        renderFieldValue: function StringFieldValue({ value, isEditing, onChange }) {
            return (
                <Popover
                    focusLock
                    returnFocus
                    content={() => {
                        // @ts-ignore
                        return <div>123</div>
                    }}
                    // onClick={handleClick}
                    // onClickOutside={handleClose}
                    // onEsc={handleClose}
                    isOpen={isEditing}
                    ignoreBoundary
                >
                    <div>{value}</div>
                </Popover>
            )
            // if (isEditing) {
            //     return (
            //         <Popover
            //             focusLock
            //             returnFocus
            //             content={() => {
            //                 // @ts-ignore
            //                 return <div>123</div>
            //             }}
            //             // onClick={handleClick}
            //             // onClickOutside={handleClose}
            //             // onEsc={handleClose}
            //             isOpen={isEditing}
            //             ignoreBoundary
            //         >
            //             {value}
            //         </Popover>
            //     )
            // }
            // return <div>{value}</div>
        },
    })
}
export default FilterString
