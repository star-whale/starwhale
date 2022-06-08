import React, { useMemo } from 'react'
import { useParams } from 'react-router-dom'
import { useQuery } from 'react-query'
import { fetchJobDAG } from '@/domain/job/services/job'
import _ from 'lodash'
import useTranslation from '@/hooks/useTranslation'
import BusyPlaceholder from '@/components/BusyLoaderWrapper/BusyPlaceholder'
import DAG from '@/components/DAG/DAG'
import Card from '../../components/Card/index'

function JobDAG() {
    const { jobId, projectId } = useParams<{ jobId: string; projectId: string }>()
    const jobDAG = useQuery(`fetchjobDAG:${projectId}:${jobId}`, () => fetchJobDAG(projectId, jobId), {
        refetchOnWindowFocus: false,
    })

    const [t] = useTranslation()
    jobDAG.data = {
        id: null,
        groupingNodes: {
            Task: [
                {
                    id: 3,
                    type: 'Task',
                    content: 'SUCCESS',
                    group: 'Task',
                    entityId: 58,
                },
                {
                    id: 4,
                    type: 'Task',
                    content: 'SUCCESS',
                    group: 'Task',
                    entityId: 59,
                },
                {
                    id: 6,
                    type: 'Task',
                    content: 'SUCCESS',
                    group: 'Task',
                    entityId: 60,
                },
            ],
            Step: [
                {
                    id: 2,
                    type: 'Step',
                    content: 'RUNNING',
                    group: 'Step',
                    entityId: 39,
                },
                {
                    id: 5,
                    type: 'Step',
                    content: 'FAILED',
                    group: 'Step',
                    entityId: 40,
                },
            ],
            Job: [
                {
                    id: 1,
                    type: 'Job',
                    content: 'CREATED',
                    group: 'Job',
                    entityId: 20,
                },
                {
                    id: 7,
                    type: 'Job',
                    content: 'SUCCESS',
                    group: 'Job',
                    entityId: 20,
                },
            ],
        },
        edges: [
            {
                from: 1,
                to: 2,
                content: null,
            },
            {
                from: 2,
                to: 3,
                content: null,
            },
            {
                from: 2,
                to: 4,
                content: null,
            },

            {
                from: 3,
                to: 5,
                content: null,
            },

            {
                from: 4,
                to: 5,
                content: null,
            },
            {
                from: 5,
                to: 6,
                content: null,
            },
            {
                from: 6,
                to: 7,
                content: null,
            },
        ],
    }

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
        <Card style={{ width: '100%', height: 'auto' }}>
            <DAG nodes={nodes} edges={edges} />
        </Card>
    )
}

export default React.memo(JobDAG)
