import React, { memo } from 'react'
import Plot from 'react-plotly.js'
import _ from 'lodash'

import { BusyLoaderWrapper } from '../BusyLoaderWrapper'

function Plotly({ data, isLoading }: any) {
    return (
        <BusyLoaderWrapper className='PlotlyLoader' isLoading={!!isLoading} height='100%'>
            <div
                className='PlotlyWrapper'
                style={{
                    height: '100%',
                    overflow: 'auto',
                }}
            >
                {_.isEmpty(data?.data) ? (
                    <div>No Tracked Figures</div>
                ) : (
                    <div
                        className='PlotlyChart'
                        style={{
                            height: '100%',
                            overflow: 'auto',
                            display: 'flex',
                            justifyContent: 'center',
                        }}
                    >
                        <Plot
                            style={{ width: '100%', height: '100%' }}
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

Plotly.displayName = 'Plotly'

export default memo(Plotly)
