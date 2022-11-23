import { QueryTableRequest, RecordListVO, ScanTableRequest, ListTablesRequest, TableNameListVO } from '../schemas/datastore';
export declare function queryTable(query: QueryTableRequest): Promise<RecordListVO>;
export declare function scanTable(query: ScanTableRequest): Promise<RecordListVO>;
export declare function listTables(query: ListTablesRequest): Promise<TableNameListVO>;
