import { IListQuerySchema } from '@/domain/base/schemas/list';
export declare function useScanDatastore(query: any, enabled?: boolean): import("react-query").UseQueryResult<import("../schemas/datastore").RecordListVO, unknown>;
export declare function useQueryDatastore(query: any, enabled?: boolean): import("react-query").UseQueryResult<import("../schemas/datastore").RecordListVO, unknown>;
export declare function useListDatastoreTables(query: any, enabled?: boolean): import("react-query").UseQueryResult<import("../schemas/datastore").TableNameListVO, unknown>;
export declare function useQueryDatasetList(tableName?: string, page?: IListQuerySchema, rawResult?: boolean): import("react-query").UseQueryResult<import("../schemas/datastore").RecordListVO, unknown>;
