import React from 'react'
import { useParams } from 'react-router-dom'
import { useQuery } from 'react-query'
import { fetchJob } from '@job/services/job'
import { ExposedLinkType, IExposedLinkSchema } from '@job/schemas/job'

export default function JobServings() {
    const { jobId, projectId } = useParams<{ jobId: string; projectId: string }>()
    const jobInfo = useQuery(['fetchJob', projectId, jobId], () => fetchJob(projectId, jobId))
    const [serving, setServing] = React.useState<IExposedLinkSchema>()
    React.useEffect(() => {
        if (jobInfo.isSuccess) {
            const job = jobInfo.data
            if (job) {
                const links = job?.exposedLinks ?? []
                links.forEach((link) => {
                    if (link.type === ExposedLinkType.WEB_HANDLER) {
                        setServing(link)
                    }
                })
            }
        }
    }, [jobInfo.data, jobInfo.isSuccess])

    return (
        <div>
            {serving && (
                <iframe
                    title='job-servings'
                    src={`${serving.link}?__theme=light`}
                    style={{
                        position: 'absolute',
                        height: '100%',
                        width: '100%',
                        border: 'none',
                    }}
                />
            )}
        </div>
    )
}
