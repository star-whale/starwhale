import LabelsIndicator from '@/components/Indicator/LabelsIndicator'
import React, { useMemo } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery } from 'react-query'
import { fetchJobResult } from '@/domain/job/services/job'
import { ILabels, INDICATORTYPE } from '@/components/Indicator/types.d'
import _ from 'lodash'
import { getHeatmapConfig } from '@/components/Indicator/utils'
import { LabelLarge, LabelMedium } from 'baseui/typography'
import { useStyletron } from 'baseui'
import BusyPlaceholder from '../../components/BusyLoaderWrapper/BusyPlaceholder'

const PlotlyVisualizer = React.lazy(
    () => import(/* webpackChunkName: "PlotlyVisualizer" */ '../../components/Indicator/PlotlyVisualizer')
)

function flattenObject(o: any, prefix = '', result: any = {}, keepNull = true) {
    if (_.isString(o) || _.isNumber(o) || _.isBoolean(o) || (keepNull && _.isNull(o))) {
        /* eslint-disable no-param-reassign */
        result[prefix] = o
        return result
    }

    if (_.isArray(o) || _.isPlainObject(o)) {
        Object.keys(o).forEach((i) => {
            let pref = prefix
            if (_.isArray(o)) {
                pref += `[${i}]`
            } else if (_.isEmpty(prefix)) {
                pref = i
            } else {
                pref = `${prefix} / ${i}`
            }
            flattenObject(o[i] ?? {}, pref, result, keepNull)
        })
        return result
    }
    return result
}
function JobResult() {
    const { jobId, projectId } = useParams<{ jobId: string; projectId: string }>()
    const jobResult = useQuery(`fetchJobResult:${projectId}:${jobId}`, () => fetchJobResult(projectId, jobId), {
        refetchOnWindowFocus: false,
    })

    const [, theme] = useStyletron()

    const indicators = useMemo(() => {
        return _.map(jobResult?.data, (v, k) => {
            let children = null

            switch (k) {
                default:
                    return <></>
                    break
                case INDICATORTYPE.KIND:
                    break
                case INDICATORTYPE.SUMMARY: {
                    const data = _.isObject(v) ? flattenObject(v) : {}
                    children = _.isObject(v) ? (
                        <div>
                            <LabelMedium
                                $style={{
                                    textOverflow: 'ellipsis',
                                    overflow: 'hidden',
                                    whiteSpace: 'nowrap',
                                    paddingBottom: '12px',
                                    borderBottom: `1px solid ${theme.borders.border400}`,
                                }}
                            >
                                Sumary
                            </LabelMedium>
                            <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr' }}>
                                {_(data)
                                    .map((subV, subK) => (
                                        <>
                                            <p>{subK}</p>
                                            <p>{subV}</p>
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
                }
                case INDICATORTYPE.CONFUSION_MATRIX: {
                    const heatmapData = getHeatmapConfig(k, _.keys(v?.binarylabel), v?.binarylabel)

                    children = (
                        <React.Suspense fallback={<BusyPlaceholder />}>
                            <PlotlyVisualizer data={heatmapData} />
                        </React.Suspense>
                    )
                    break
                }
                case INDICATORTYPE.LABELS:
                    _.forIn(v as ILabels, (subV, subK) => {
                        const [tp, fp, tn, fn] = _.flatten(
                            jobResult?.data?.[INDICATORTYPE.CONFUSION_MATRIX]?.multilabel?.[Number(subK)]
                        )

                        /* eslint-disable no-param-reassign */
                        subV = Object.assign(subV, {
                            tp,
                            fp,
                            tn,
                            fn,
                        })
                    })
                    children = <LabelsIndicator isLoading={jobResult.isLoading} data={v} />
                    break
            }
            return (
                children && (
                    <div key={k} style={{ padding: '20px', background: '#fff', borderRadius: '12px' }}>
                        {children}
                    </div>
                )
            )
        })
    }, [jobResult.data, jobResult.isLoading, theme])

    if (jobResult.isFetching) {
        return <BusyPlaceholder />
    }

    if (jobResult.isError) {
        return <BusyPlaceholder type='notfound' />
    }

    return (
        <div style={{ width: '100%', height: 'auto' }}>
            {jobResult.data?.kind && (
                <div
                    key='kind'
                    style={{
                        width: '100%',
                        lineHeight: 50,
                        padding: '20px',
                        background: '#fff',
                        borderRadius: '12px',
                        marginBottom: '16px',
                        boxSizing: 'border-box',
                    }}
                >
                    <LabelLarge
                        $style={{
                            textOverflow: 'ellipsis',
                            overflow: 'hidden',
                            whiteSpace: 'nowrap',
                        }}
                    >
                        Kind: {jobResult.data?.kind ?? ''}
                    </LabelLarge>
                </div>
            )}
            <div
                style={{
                    width: '100%',
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(500px, 1fr))',
                    gridGap: '16px',
                }}
            >
                {indicators}
            </div>
        </div>
    )
}

export default React.memo(JobResult)
