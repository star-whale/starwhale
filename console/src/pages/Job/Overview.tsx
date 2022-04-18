import React, { useCallback, useState } from 'react'
import useTranslation from '@/hooks/useTranslation'
import { useJob, useJobLoading } from '@job/hooks/useJob'
import TaskListCard from './TaskListCard'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'

export default function JobOverview() {
    const { job } = useJob()
    const { jobLoading } = useJobLoading()

    const [t] = useTranslation()

    const jobName = job?.name ?? ''

    return (
        <>
            <TaskListCard
                header={
                    <div
                        style={{
                            display: 'grid',
                            gridTemplateColumns: 'repeat(3, 1fr)',
                            gap: '12px',
                            marginBottom: '32px',
                            fontSize: '16px',
                        }}
                    >
                        <div>
                            {t('Job ID')}: {job?.uuid}
                        </div>
                        <div>
                            {t('Created time')}: {job?.createTime && formatTimestampDateTime(job.createTime)}
                        </div>
                        <div>
                            {t('Owner')}: {job?.owner?.name}
                        </div>
                        <div>
                            {t('Run time')}:&nbsp;
                            {typeof job?.duration == 'string' ? '-' : durationToStr(job?.duration ?? 0)}
                        </div>
                        <div>
                            {t('End time')}:{' '}
                            {job?.stopTime && job?.stopTime > 0 ? formatTimestampDateTime(job?.stopTime) : '-'}
                        </div>
                    </div>
                }
            />
        </>
    )
}
