import React, { useCallback, useState } from 'react'
import useTranslation from '@/hooks/useTranslation'
import { useJob, useJobLoading } from '@job/hooks/useJob'
import TaskListCard from './TaskListCard'
import { durationToStr, formatTimestampDateTime } from '@/utils/datetime'
import Card from '@/components/Card'
import { ScrollFollow, LazyLog } from 'react-lazylog'
import { Accordion, Panel } from 'baseui/accordion'
import { Grid, Cell } from 'baseui/layout-grid'

export default function JobOverview() {
    const { job } = useJob()
    const { jobLoading } = useJobLoading()

    const [t] = useTranslation()

    const jobName = job?.name ?? ''

    const items = [
        {
            label: t('Job ID'),
            value: job?.id ?? '',
        },
        {
            label: t('Owner'),
            value: job?.owner?.name ?? '-',
        },
        {
            label: t('Run time'),
            value: job?.stopTime && job?.stopTime > 0 ? formatTimestampDateTime(job?.stopTime) : '-',
        },
        {
            label: t('Created time'),
            value: job?.createTime && formatTimestampDateTime(job.createTime),
        },
        ,
        {
            label: t('End time'),
            value: job?.createTime && formatTimestampDateTime(job.createTime),
        },
    ]

    const [currentLog, setCurrentLog] = useState('')
    const [expanded, setExpanded] = useState(false)
    const onAction = useCallback((type, values) => {
        setCurrentLog(values?.uuid)
        setExpanded(true)
    }, [])

    const content = new Array(1000).fill('test')
    return (
        <>
            <div
                style={{
                    width: '100%',
                    display: 'grid',
                    gridTemplateColumns: 'repeat(auto-fit, minmax(500px, 1fr))',
                    gridGap: '16px',
                }}
            >
                <TaskListCard header={null} onAction={onAction} />

                <Card
                    style={{
                        display: 'grid',
                        gridTemplateColumns: 'repeat(auto-fit, minmax(500px, 1fr))',
                        gap: '12px',
                        fontSize: '16px',
                    }}
                >
                    {items.map((v) => (
                        <div key={v?.label} style={{ display: 'flex' }}>
                            <div style={{ flexBasis: '200px' }}>{v?.label}</div>
                            <div>: {v?.value}</div>
                        </div>
                    ))}
                </Card>
            </div>
            <Accordion
                overrides={{
                    Content: {
                        style: {
                            height: '800px',
                            paddingBottom: '20px',
                        },
                    },
                }}
                onChange={({ expanded }) => {
                    setExpanded(expanded.includes('0'))
                }}
            >
                <Panel title={`Logs ${currentLog ? ':' + currentLog : ''}`} expanded={expanded ? true : undefined}>
                    <ScrollFollow
                        startFollowing
                        render={({ follow }) => (
                            <LazyLog
                                enableSearch
                                // selectableLines
                                text={content.length > 0 ? content.join('\n') : ' '}
                                follow={follow}
                                // scrollToLine={scrollToLine}
                                // onScroll={handleScroll}
                            />
                        )}
                    />
                </Panel>
            </Accordion>
        </>
    )
}
