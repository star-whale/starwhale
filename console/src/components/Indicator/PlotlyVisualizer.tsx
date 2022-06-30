import React, { memo } from 'react'
import Plot from 'react-plotly.js'
import _ from 'lodash'

import BusyLoaderWrapper from '@/components/BusyLoaderWrapper/BusyLoaderWrapper'

import { IPlotlyVisualizerProps } from './types'

import './PlotlyVisualizer.scss'

function PlotlyVisualizer({ data, isLoading }: IPlotlyVisualizerProps) {
    return (
        <BusyLoaderWrapper className='VisualizationLoader' isLoading={!!isLoading}>
            <div className='PlotlyVisualizer'>
                {_.isEmpty(data?.data) ? (
                    <div>No Tracked Figures</div>
                ) : (
                    <div className='PlotlyVisualizer__chart'>
                        <Plot
                            data={data?.data}
                            layout={data?.layout}
                            frames={data?.frames}
                            // config={{ responsive: true }}
                            // onInitialized={(figure) => this.setState(figure)}
                            // onUpdate={(figure) => this.setState(figure)}
                            useResizeHandler
                        />
                    </div>
                )}
            </div>
        </BusyLoaderWrapper>
    )
}

PlotlyVisualizer.displayName = 'PlotlyVisualizer'

export default memo<IPlotlyVisualizerProps>(PlotlyVisualizer)
