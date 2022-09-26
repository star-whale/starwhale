import React, { useMemo } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery } from 'react-query'
import { fetchJobDAG } from '@/domain/job/services/job'
import _ from 'lodash'
import BusyPlaceholder from '@/components/BusyLoaderWrapper/BusyPlaceholder'
import DAG from '@/components/DAG/DAG'
import Card from '../../components/Card/index'

function JobDAG() {
    const { jobId, projectId } = useParams<{ jobId: string; projectId: string }>()
    const jobDAG = useQuery(`fetchjobDAG:${projectId}:${jobId}`, () => fetchJobDAG(projectId, jobId), {
        refetchOnWindowFocus: false,
    })

    const nodes = useMemo(() => {
        if (!jobDAG.data?.groupingNodes) return []

        return _.flatMap(jobDAG.data?.groupingNodes).map((node: any) => {
            return {
                id: node.id,
                data: node,
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
        <Card style={{ width: '100%', height: 'auto', flex: 1, position: 'relative' }}>
            <DAG nodes={nodes} edges={edges} />
        </Card>
    )
}

export default React.memo(JobDAG)
