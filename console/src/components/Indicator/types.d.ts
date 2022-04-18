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
}

export interface IIndicator {
    name: INDICATOR_TYPE
    value: IMBCConfusionMetrics | IMCConfusionMetrics | any
}
