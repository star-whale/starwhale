import { ScanTableRequest } from '../schemas/datastore'
import useFetchDatastoreByTable from './useFetchDatastoreByTable'

export function useFetchDatastoreByTables(recordQuery: ScanTableRequest, enabled = true) {
    return useFetchDatastoreByTable(recordQuery, enabled)
}

export default useFetchDatastoreByTables
