import { DataTypeT } from '@starwhale/core/datastore'
import FilterString from './FilterString'
import FilterBoolean from './FilterBoolean'
import FilterNumberical from './FilterNumberical'
import { FilterT, KIND } from './types'

export const dataStoreToFilter = (dataStoreKind?: DataTypeT): (() => FilterT) => {
    switch (dataStoreKind) {
        default:
        case 'STRING':
            return FilterString
        case 'BOOL':
            return FilterBoolean
        case 'INT8':
        case 'INT16':
        case 'INT32':
        case 'INT64':
        case 'FLOAT16':
        case 'FLOAT32':
        case 'FLOAT64':
            return FilterNumberical
    }
}

export const dataStoreToFilterKind = (dataStoreKind: DataTypeT): KIND => {
    switch (dataStoreKind) {
        case 'BOOL':
            return KIND.BOOLEAN
        case 'INT8':
        case 'INT16':
        case 'INT32':
        case 'INT64':
        case 'FLOAT16':
        case 'FLOAT32':
        case 'FLOAT64':
            return KIND.NUMERICAL
        case 'STRING':
            return KIND.STRING
        default:
            return KIND.STRING
    }
}
