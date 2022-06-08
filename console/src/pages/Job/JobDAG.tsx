import React, { useMemo } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery } from 'react-query'
import { fetchJobDAG } from '@/domain/job/services/job'
import _ from 'lodash'
import useTranslation from '@/hooks/useTranslation'
import BusyPlaceholder from '@/components/BusyLoaderWrapper/BusyPlaceholder'
import { Canvas, Node, Edge, Port, MarkerArrow, Label } from 'reaflow'

// const PlotlyVisualizer = React.lazy(
//     () => import(/* webpackChunkName: "PlotlyVisualizer" */ '../../components/Indicator/PlotlyVisualizer')
// )

function JobDAG() {
    const { jobId, projectId } = useParams<{ jobId: string; projectId: string }>()
    const jobDAG = useQuery(`fetchjobDAG:${projectId}:${jobId}`, () => fetchJobDAG(projectId, jobId), {
        refetchOnWindowFocus: false,
    })

    const [t] = useTranslation()

    const nodes = useMemo(() => {
        if (!jobDAG.data?.groupingNodes) return []

        return _.flatMap(jobDAG.data?.groupingNodes).map((node: any) => {
            return {
                id: node.id,
                text: [node.type, node.content].join(': '),
            }
        })
    }, [jobDAG.data])

    const edges = useMemo(() => {
        if (!jobDAG.data?.edges) return []

        return jobDAG.data?.edges.map((edge: any) => {
            return {
                id: `${edge.from}-${edge.to}`,
                from: edge.from,
                to: edge.to,
            }
        })
    }, [jobDAG.data])

    if (jobDAG.isFetching) {
        return <BusyPlaceholder />
    }

    if (jobDAG.isError) {
        return <BusyPlaceholder type='notfound' />
    }

    return (
        <div style={{ width: '100%', height: 'auto' }}>
            <div
                style={{
                    width: '100%',
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(800px, 1fr))',
                    gridGap: '16px',
                }}
            >
                <Canvas
                    fit={true}
                    maxHeight={1200}
                    nodes={nodes}
                    edges={edges}
                    node={
                        <Node
                            style={{ stroke: '#1a192b', fill: 'white', strokeWidth: 1 }}
                            label={<Label style={{ fill: 'black' }} />}
                            port={<Port style={{ fill: 'blue', stroke: 'white' }} rx={10} ry={10} />}
                        />
                    }
                    arrow={<MarkerArrow style={{ fill: '#b1b1b7' }} />}
                    edge={<Edge className='edge' />}
                />
            </div>
        </div>
    )
}

export default React.memo(JobDAG)
