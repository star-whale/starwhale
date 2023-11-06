import React, { useEffect } from 'react'
import { useModelVersionContext } from '@/pages/Model/ModelOverviewLayout'
import { Select } from '@starwhale/ui'
import _ from 'lodash'

interface IServingProps {
    jobId: string
    name: string
    link: string
}

export default function ModelVersionServings() {
    const modelVersionCtx = useModelVersionContext()

    const [links, setLinks] = React.useState<IServingProps[]>([])
    const [currentLink, setCurrentLink] = React.useState<IServingProps>(links?.[0])

    useEffect(() => {
        if (modelVersionCtx.servingJobs) {
            const _links: IServingProps[] = _.flatten(
                modelVersionCtx.servingJobs?.map(
                    (job) => job.exposedLinks?.map((link): IServingProps => ({ jobId: job.id, ...link })) ?? []
                )
            )
            setLinks(_links)
            setCurrentLink(_links?.[0])
        }
    }, [modelVersionCtx.servingJobs])

    return (
        <div>
            <Select
                options={links.map((link) => ({
                    id: link.jobId,
                    label: `${link.jobId}-${link.name}`,
                }))}
                onChange={(params) => {
                    const link = links.find((v) => v?.jobId === params.option?.id)
                    if (link) {
                        setCurrentLink(link)
                    }
                }}
                value={
                    currentLink
                        ? [
                              {
                                  id: currentLink?.jobId,
                              },
                          ]
                        : []
                }
            />
            {currentLink && (
                <iframe
                    title='job-servings'
                    src={`${currentLink?.link}?__theme=light`}
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
