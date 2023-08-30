import React from 'react'
import useTranslation from '@/hooks/useTranslation'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import { useProject } from '@project/hooks/useProject'
import { useJob } from '@/domain/job/hooks/useJob'
import JobStatus from '@/domain/job/components/JobStatus'
import { MonoText } from '@/components/Text'

export default function JobOverview() {
    const { job } = useJob()
    const { project } = useProject()
    const [t] = useTranslation()

    const items = [
        {
            label: t('Job ID'),
            value: job?.id ?? '-',
        },
        {
            label: t('Resource Pool'),
            value: job?.resourcePool,
        },
        {
            label: t('sth name', [t('Model')]),
            value: job?.modelName,
        },
        {
            label: t('Model Version'),
            value: <MonoText maxWidth='400px'>{job?.modelVersion}</MonoText>,
        },
        {
            label: t('Elapsed Time'),
            value: typeof job?.duration === 'string' ? '-' : durationToStr(job?.duration as any),
        },
        {
            label: t('Created'),
            value: job?.createdTime && job?.createdTime > 0 && formatTimestampDateTime(job?.createdTime),
        },
        {
            label: t('End Time'),
            value: job?.stopTime && job?.stopTime > 0 ? formatTimestampDateTime(job?.stopTime) : '-',
        },
        {
            label: t('Status'),
            value: job?.jobStatus && <JobStatus key='jobStatus' status={job.jobStatus as any} />,
        },
    ].filter((item) => {
        // hide shared if project is private
        return project?.privacy === 'PUBLIC' || item.label !== t('dataset.overview.shared')
    })

    return (
        <div className='flex-column'>
            {items.map((v) => (
                <div
                    key={v?.label}
                    style={{
                        display: 'flex',
                        gap: '20px',
                        borderBottom: '1px solid #EEF1F6',
                        lineHeight: '44px',
                        flexWrap: 'nowrap',
                        fontSize: '14px',
                        paddingLeft: '12px',
                    }}
                >
                    <div
                        style={{
                            flexBasis: '110px',
                            color: 'rgba(2,16,43,0.60)',
                        }}
                    >
                        {v?.label}:
                    </div>
                    <div style={{ display: 'flex', alignItems: 'center' }}>{v?.value}</div>
                </div>
            ))}
        </div>
    )
}
