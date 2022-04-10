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
                            display: 'flex',
                            flexWrap: 'wrap',
                        }}
                    >
                        <div style={{ width: '40%' }}>
                            {t('Job ID')}: {job?.uuid}
                        </div>
                        <div style={{ width: '40%' }}>
                            {t('Created time')}: {job?.createTime && formatTimestampDateTime(job.createTime)}
                        </div>
                        <div style={{ width: '20%' }}>
                            {t('Owner')}: {job?.owner?.name}
                        </div>
                        <div style={{ width: '40%' }}>
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
