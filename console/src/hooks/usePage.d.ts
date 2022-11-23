import { IListQuerySchema } from '@/domain/base/schemas/list';
import { IQueryArgs, IUpdateQueryArgs } from './useQueryArgs';
export declare function usePage(opt?: {
    query?: IQueryArgs;
    updateQuery?: IUpdateQueryArgs;
    defaultCount?: number;
}): [IListQuerySchema, (page: IListQuerySchema) => void];
