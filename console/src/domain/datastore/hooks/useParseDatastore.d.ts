import { RecordListVO } from '../schemas/datastore';
export declare function useParseConfusionMatrix(data?: RecordListVO): {
    labels: string[];
    binarylabel: any[][];
};
export declare function useParseRocAuc(data?: RecordListVO): {
    records: Record<string, any>[];
    fpr: number[];
    tpr: number[];
};
