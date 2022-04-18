export interface ITraceVisualizerProps {
    isLoading?: boolean
    activeTraceContext?: string
    data: any
}

export interface IImagesVisualizerProps extends ITraceVisualizerProps {}

export interface IDistributionVisualizerProps extends ITraceVisualizerProps {}

export interface ITextsVisualizerProps extends ITraceVisualizerProps {}

export interface IPlotlyVisualizerProps extends ITraceVisualizerProps {}
