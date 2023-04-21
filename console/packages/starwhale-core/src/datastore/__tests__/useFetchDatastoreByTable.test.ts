// import { renderHook } from '@testing-library/react-hooks'
// import useFetchDatastoreByTable from '../hooks/useFetchDatastoreByTable'
// import { QueryClientProvider, QueryClient } from 'react-query'

// jest.mock('./useDatastore')
// jest.mock('./useFetchDatastore')

// describe('useFetchDatastoreByTable', () => {
//     const queryClient = new QueryClient()

//     it('should return record and column info from datastore', async () => {
//         const tableName = 'test_table'
//         const filter = [{ query: 'field = "value"' }]
//         const options = { filter }
//         const columnQuery = getColumnQuery(tableName)
//         const recordQuery = getRecordQuery(tableName, options)

//         jest.mock('./useDatastoreQueryParams', () => ({
//             __esModule: true,
//             getQuery: () => ({
//                 columnQuery,
//                 recordQuery,
//             }),
//         }))

//         const { result, waitFor } = renderHook(() => useFetchDatastoreByTable(tableName, options), {
//             wrapper: ({ children }) => <QueryClientProvider client={queryClient}>{children}</QueryClientProvider>,
//         })

//         await waitFor(() => result.current.columnInfo.isSuccess && result.current.recordInfo.isSuccess)

//         expect(result.current.recordQuery).toEqual(recordQuery)
//         expect(result.current.columnInfo.data?.columnTypes).toBeDefined()
//         expect(result.current.columnTypes).toBeDefined()
//         expect(result.current.records).toBeDefined()
//     })
// })
