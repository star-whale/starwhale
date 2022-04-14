import React, { memo } from 'react'
import Plot from 'react-plotly.js'
import _ from 'lodash'

import BusyLoaderWrapper from '@/components/BusyLoaderWrapper/BusyLoaderWrapper'
import ErrorBoundary from '@/components/ErrorBoundary/ErrorBoundary'
// import IllustrationBlock from 'components/IllustrationBlock/IllustrationBlock'

import { IPlotlyVisualizerProps } from './types'

import './PlotlyVisualizer.scss'

function PlotlyVisualizer(props: IPlotlyVisualizerProps) {
    return (
        <ErrorBoundary>
            <BusyLoaderWrapper className='VisualizationLoader' isLoading={!!props.isLoading}>
                <div className='PlotlyVisualizer'>
                    {_.isEmpty(props.data?.data) ? (
                        // <IllustrationBlock size='xLarge' title='No Tracked Figures' />
                        <div>No Tracked Figures</div>
                    ) : (
                        <div className='PlotlyVisualizer__recordCnt'>
                            <Plot
                                data={props.data?.data}
                                layout={props.data?.layout}
                                frames={props.data?.frames}
                                // config={{ responsive: true }}
                                // onInitialized={(figure) => this.setState(figure)}
                                // onUpdate={(figure) => this.setState(figure)}
                                useResizeHandler={true}
                            />
                        </div>
                    )}
                </div>
            </BusyLoaderWrapper>
        </ErrorBoundary>
    )
}

PlotlyVisualizer.displayName = 'PlotlyVisualizer'

export default memo<IPlotlyVisualizerProps>(PlotlyVisualizer)
