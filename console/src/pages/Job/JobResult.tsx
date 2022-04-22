import LabelsIndicator from '@/components/Indicator/LabelsIndicator'
import { Spinner } from 'baseui/spinner'
import React, { useEffect, useMemo } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery } from 'react-query'
import { fetchJobResult } from '@/domain/job/services/job'
import { ILabels, INDICATOR_TYPE } from '@/components/Indicator/types.d'
import _ from 'lodash'
import { getHeatmapConfig } from '@/components/Indicator/utils'
import { LabelLarge, LabelMedium } from 'baseui/typography'
import { useStyletron } from 'baseui'
import BusyPlaceholder from '../../components/BusyLoaderWrapper/BusyPlaceholder'

// import ResponsiveReactGridLayout from 'react-grid-layout'

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
    const jobResult = useQuery(`fetchJobResult:${projectId}:${jobId}`, () => fetchJobResult(projectId, jobId), {
        refetchOnWindowFocus: false,
    })
    useEffect(() => {
        if (jobResult.isSuccess) {
            // console.log(jobResult.data)
        }
    }, [jobResult])

    const [, theme] = useStyletron()

    const indicators = useMemo(() => {
        return _.map(jobResult?.data, (v, k) => {
            let children = null

            switch (k) {
                case INDICATOR_TYPE.KIND:
                    break
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
                case INDICATOR_TYPE.CONFUSION_MATRIX:
                    const heatmapData = getHeatmapConfig(k, _.keys(v?.binarylabel), v?.binarylabel)

                    children = (
                        <React.Suspense fallback={<BusyPlaceholder />}>
                            <PlotlyVisualizer data={heatmapData} />
                        </React.Suspense>
                    )
                    break
                case INDICATOR_TYPE.LABELS:
                    _.forIn(v as ILabels, (subV, subK) => {
                        const [tp, fp, tn, fn] = _.flatten(
                            jobResult?.data?.[INDICATOR_TYPE.CONFUSION_MATRIX]?.mutlilabel?.[Number(subK)]
                        )

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
    }, [jobResult.data, jobResult.isSuccess])

    // const layout = [
    //     { i: 'kind', x: 0, y: 0, w: 2, h: 1, static: true },
    //     { i: 'confusion_matrix', x: 3, y: 0, w: 7, h: 12, minW: 2, maxW: 4 },
    //     { i: 'sumary', x: 4, y: 0, w: 1, h: 2 },
    // ]
    if (jobResult.isFetching) {
        return <BusyPlaceholder />
    }

    return (
        <div style={{ width: '100%', height: 'auto' }}>
            {jobResult.data?.kind && (
                <div
                    key={'kind'}
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
            {/* <ResponsiveReactGridLayout className='layout' cols={12} rowHeight={30} width={1200} layout={layout}>
                {indicators}
            </ResponsiveReactGridLayout> */}
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
            </div>
        </div>
    )
}

export default React.memo(JobResult)
