import Filter from './Filter'
import { FilterT, KIND, FilterTypeOperators } from '../types'

function FilterNumberical(): FilterT {
    return Filter({
        kind: KIND.NUMERICAL,
        operators: FilterTypeOperators[KIND.NUMERICAL],
        // notice: disable for now, support operator not in / in etc
        // @ts-ignore
        // renderValue: React.forwardRef((props: any, ref: React.RefObject<HTMLInputElement>) => {
        //     return (
        //         <NumberInput
        //             {...props}
        //             inputRef={ref}
        //             type='float'
        //             overrides={{
        //                 Root: {
        //                     style: {
        //                         borderTopWidth: '0px',
        //                         borderBottomWidth: '0px',
        //                         borderLeftWidth: '0px',
        //                         borderRightWidth: '0px',
        //                         paddingTop: '1px',
        //                         paddingBottom: '1px',
        //                         paddingLeft: '0px',
        //                         paddingRight: '0px',
        //                     },
        //                 },
        //                 Input: {
        //                     style: {
        //                         paddingTop: '1px',
        //                         paddingBottom: '1px',
        //                         paddingLeft: '0px',
        //                         paddingRight: '0px',
        //                     },
        //                 },
        //             }}
        //         />
        //     )
        // }),
    })
}
export default FilterNumberical
