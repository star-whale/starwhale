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
export interface ILabel {
    'id'?: string
    'support': number
    'precision': number
    'recall': number
    'f1-score': number
}
export type ILabels = Record<string, IConfusionMetric>
export type IConfusionMatrixBinarylabel = number[]
export type IConfusionMatrixMutlilabel = number[][] // 2*2

// const dataMBCConfusionMetrics = useMemo((): IMBCConfusionMetrics => {
//     const indicator = jobResult?.data?.indicators.find(
//         (v: IIndicator) => v.name === INDICATOR_TYPE.MBCConfusionMetrics
//     )
//     return indicator?.value ?? {}
// }, [jobResult, jobResult.data, jobResult.isSuccess])

// const dataMCConfusionMetrics = useMemo((): IMCConfusionMetrics => {
//     const indicator = jobResult?.data?.indicators.find(
//         (v: IIndicator) => v.name === INDICATOR_TYPE.MCConfusionMetrics
//     )
//     return indicator?.value ?? {}
// }, [jobResult, jobResult.data, jobResult.isSuccess])

// const labels = useMemo(() => {
//     return _.keys(dataMBCConfusionMetrics) ?? []
// }, [dataMBCConfusionMetrics])

// const [heatmap, maxValue] = useMemo(() => {
//     let metrics: number[][] = Array(labels.length)
//         .fill(0)
//         .map(() => Array(labels.length).fill(0))

//     let maxValue = 0

//     _.map(dataMCConfusionMetrics, (v, k) => {
//         metrics[_.toNumber(v.label)][_.toNumber(v.prediction)] = v.value
//         if (v.value > maxValue) {
//             maxValue = v.value
//         }
//     })
//     return [metrics, maxValue]
// }, [dataMCConfusionMetrics])

// const heatmapData = usetHeatmapConfig(labels, heatmap)
