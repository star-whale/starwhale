export declare type IQueryArgs = Record<string, string>;
export declare type IUpdateQueryArgs = (query: Record<string, string | number | undefined>) => void;
export declare const useQueryArgs: () => {
    query: IQueryArgs;
    updateQuery: IUpdateQueryArgs;
};
