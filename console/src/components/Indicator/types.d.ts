export interface ITraceVisualizerProps {
    isLoading?: boolean
    activeTraceContext?: string
    data: any
}

export interface IImagesVisualizerProps extends ITraceVisualizerProps {}

export interface IDistributionVisualizerProps extends ITraceVisualizerProps {}

export interface ITextsVisualizerProps extends ITraceVisualizerProps {}

export interface IPlotlyVisualizerProps extends ITraceVisualizerProps {}

export interface IMBCConfusionMetric {
    id?: string
    tp: number
    tn: number
    fp: number
    fn: number
    accuracy: number
    precision: number
    recall: number
}

export type IMBCConfusionMetrics = Record<string, IMBCConfusionMetric>

export type IMCConfusionMetric = {
    label: string
    prediction: string
    name: string
    value: number
}

export type IMCConfusionMetrics = Array<IMCConfusionMetric>

export enum INDICATOR_TYPE {
    MCConfusionMetrics = 'MCConfusionMetrics',
    CohenKappa = 'CohenKappa',
    MBCConfusionMetrics = 'MBCConfusionMetrics',
    KIND = 'kind',
    LABELS = 'labels',
    CONFUSION_MATRIX = 'confusion_matrix',
    SUMMARY = 'summary',
}

export interface IIndicator {
    // name: INDICATOR_TYPE
    // value: IMBCConfusionMetrics | IMCConfusionMetrics | any
}

export interface ISysIndicator extends IIndicator {}
export interface ICustomIndicator extends IIndicator {}

export type IKind = string
export type ISummary = Record<string, any>

export type IBinaryLabel = {
    'id'?: string
    'support': number
    'precision': number
    'recall': number
    'f1-score': number
}
export type IMultiLabel = {
    tp?: number
    tn?: number
    fp?: number
    fn?: number
}
export type ILabel = IBinaryLabel & IMultiLabel
export type ILabels = Record<string, ILabel>
export type IConfusionMatrixBinarylabel = number[]
export type IConfusionMatrixMutlilabel = number[][] // 2*2
