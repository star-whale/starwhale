import BusyLoaderWrapper from '@/components/BusyLoaderWrapper/BusyLoaderWrapper'
import Card from '@/components/Card'
import MBCConfusionMetricsIndicator from '@/components/Indicator/MBCConfusionMetricsIndicator'
import { Spinner } from 'baseui/spinner'
import React, { useEffect, useMemo } from 'react'
import Plot from 'react-plotly.js'
import { useParams } from 'react-router-dom'
import { useQuery } from 'react-query'
import { fetchJobResult } from '@/domain/job/services/job'
import { IIndicator, IMBCConfusionMetrics, IMCConfusionMetrics, INDICATOR_TYPE } from '@/components/Indicator/types.d'
import _ from 'lodash'
import { getHeatmapConfig } from '@/components/Indicator/utils'
import { DisplayMedium, DisplayXSmall, LabelMedium } from 'baseui/typography'

const PlotlyVisualizer = React.lazy(
    () => import(/* webpackChunkName: "PlotlyVisualizer" */ '../../components/Indicator/PlotlyVisualizer')
)
function flattenObject(o: any, prefix = '', result: any = {}, keepNull = true) {
    if (_.isString(o) || _.isNumber(o) || _.isBoolean(o) || (keepNull && _.isNull(o))) {
        result[prefix] = o
        return result
    }

    if (_.isArray(o) || _.isPlainObject(o)) {
        for (let i in o) {
            let pref = prefix
            if (_.isArray(o)) {
                pref = pref + `[${i}]`
            } else {
                if (_.isEmpty(prefix)) {
                    pref = i
                } else {
                    pref = prefix + ' / ' + i
                }
            }
            flattenObject(o[i] ?? {}, pref, result, keepNull)
        }
        return result
    }
    return result
}
function JobResult() {
    const { jobId, projectId } = useParams<{ jobId: string; projectId: string }>()
    const jobResult = useQuery('fetchJobResult', () => fetchJobResult(projectId, jobId))
    useEffect(() => {
        if (jobResult.isSuccess) {
            console.log(jobResult.data)
        }
    }, [jobResult])

    const indicators = useMemo(() => {
        return _.map(jobResult?.data, (v, k) => {
            let children = null

            switch (k) {
                case INDICATOR_TYPE.KIND:
                    return (
                        <div
                            style={{
                                width: 200,
                                height: 50,
                                padding: '20px',
                                background: '#fff',
                                borderRadius: '12px',
                            }}
                        >
                            <LabelMedium
                                $style={{
                                    textOverflow: 'ellipsis',
                                    overflow: 'hidden',
                                    whiteSpace: 'nowrap',
                                }}
                            >
                                Kind: {v}
                            </LabelMedium>
                        </div>
                    )
                case INDICATOR_TYPE.SUMMARY:
                    const data = _.isObject(v) ? flattenObject(v) : {}
                    children = _.isObject(v) ? (
                        <div>
                            <LabelMedium
                                $style={{
                                    textOverflow: 'ellipsis',
                                    overflow: 'hidden',
                                    whiteSpace: 'nowrap',
                                    paddingBottom: '12px',
                                    borderBottom: '1px solid #000',
                                }}
                            >
                                Sumary
                            </LabelMedium>
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr' }}>
                                {_(data)
                                    .map((v, k) => (
                                        <>
                                            <p>{k}</p>
                                            <p>{v}</p>
                                        </>
                                    ))
                                    .value()}
                            </div>
                        </div>
                    ) : (
                        <LabelMedium
                            $style={{
                                textOverflow: 'ellipsis',
                                overflow: 'hidden',
                                whiteSpace: 'nowrap',
                            }}
                        >
                            Sumary: {v}
                        </LabelMedium>
                    )
                    break
                case INDICATOR_TYPE.CONFUSION_MATRIX:
                    const heatmapData = getHeatmapConfig(k, _.keys(v?.binarylabel), v?.binarylabel)

                    children = (
                        <React.Suspense fallback={<Spinner />}>
                            <PlotlyVisualizer data={heatmapData} />
                        </React.Suspense>
                    )
                    break
            }

            return (
                children && <div style={{ padding: '20px', background: '#fff', borderRadius: '12px' }}>{children}</div>
            )
        })
    }, [jobResult.data, jobResult.isSuccess])

    return (
        <>
            <div
                style={{
                    width: '100%',
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(500px, 1fr))',
                    // gridAutoRows: '460px',
                    gridGap: '16px',
                }}
            >
                {indicators}
                {/* {dataMBCConfusionMetrics && (
                    <div
                        style={{
                            padding: '20px',
                            background: '#fff',
                            borderRadius: '12px',
                        }}
                    >
                        <MBCConfusionMetricsIndicator isLoading={jobResult.isLoading} data={dataMBCConfusionMetrics} />
                    </div>
                )}

                <div style={{ padding: '20px', background: '#fff', borderRadius: '12px' }}>
                    <React.Suspense fallback={<Spinner />}>
                        <PlotlyVisualizer data={heatmapData} />
                    </React.Suspense>
                </div> */}
            </div>
        </>
    )
}

export default React.memo(JobResult)
