import React, { useState } from 'react'
import _ from 'lodash'
import useTranslation from '@/hooks/useTranslation'
import { fetchTask, fetchTaskOfflineFileLog, fetchTaskOfflineLogFiles } from '@/domain/job/services/task'
import { getToken } from '@/api'
import { TaskStatusType } from '@/domain/job/schemas/task'
import TaskListCard from './TaskListCard'
import { toaster } from 'baseui/toast'
import { BusyPlaceholder } from '@starwhale/ui'
import { Tabs, Tab } from 'baseui/tabs'
import { mergeOverrides, expandPadding, expandBorder } from '@starwhale/ui/utils'
import { useQueryArgs } from '@starwhale/core'
import { fetchJobEvents } from '@/domain/job/services/job'
import { useParams } from 'react-router-dom'
import TaskEventListCard from './TaskEventListCard'
import { useQuery } from 'react-query'
import { useUpdateEffect } from 'ahooks'

const ComplexToolbarLogViewer = React.lazy(() => import('@/components/LogViewer/LogViewer'))

export interface IScrollProps {
    scrollTop: number
    scrollHeight: number
    clientHeight: number
}

enum ActiveType {
    LOG = 'log',
    EVENT = 'event',
}

function TaskLogViewer({ taskId, sources, activeKey }: any) {
    if (activeKey !== 'log') return null

    const currentOnlineLogUrl = `${window.location.protocol === 'http:' ? 'ws:' : 'wss:'}//${
        window.location.host
    }/api/v1/log/online/${taskId}?Authorization=${getToken()}`

    const $sources = Object.entries(sources).map(([fileName, content]: [string, string]) => {
        return {
            id: fileName,
            type: '',
            data: content.startsWith('ws') ? '' : content,
            ws: content.startsWith('ws') ? currentOnlineLogUrl : undefined,
        }
    })

    return (
        <React.Suspense fallback={<BusyPlaceholder />}>
            <ComplexToolbarLogViewer sources={$sources} />
        </React.Suspense>
    )
}

export default function JobTasks() {
    const [t] = useTranslation()
    const [currentLogFiles, setCurrentLogFiles] = useState<Record<string, any>>({})
    const [currentEvents, setCurrentEvents] = useState<Record<string, any>>({})
    const { projectId, jobId } = useParams<{ projectId: string; jobId: string }>()
    const { query, updateQuery } = useQueryArgs()
    const { taskId, active: activeKey } = query
    const { data: task } = useQuery('fetchTask', () => fetchTask(projectId, jobId, taskId), {
        enabled: !!taskId,
    })

    useUpdateEffect(() => {
        if (!task) return

        async function load() {
            if (!task) return

            let files: Record<string, any> = {}

            if (activeKey === ActiveType.EVENT) {
                const data = await fetchJobEvents(projectId, jobId, task.id)
                if (data.length === 0) return

                files = _.chain(data)
                    .groupBy((x) =>
                        [_.get(x, 'relatedResource.eventResourceType'), _.get(x, 'relatedResource.id')].join('-')
                    )
                    .value()

                setCurrentEvents({
                    ...files,
                })
                return
            }

            if ([TaskStatusType.RUNNING].includes(task.taskStatus)) {
                const key = [task?.stepName, task?.id].join('@')
                files[key] = 'ws'
            }

            const data = await fetchTaskOfflineLogFiles(task?.id)
            if (!_.isEmpty(data)) {
                await Promise.all(
                    data.map(async (v: string) => {
                        const content = await fetchTaskOfflineFileLog(task?.id, v)
                        const key = [task?.stepName, v].join('@')
                        files[key] = content ?? ''
                    })
                )
            }

            if (Object.keys(files).length === 0) {
                toaster.negative('No logs collected for this task', { autoHideDuration: 2000 })
            }
            setCurrentLogFiles({
                ...files,
            })
        }

        load()
    }, [task, activeKey])

    const hasSource = Object.keys(currentLogFiles).length > 0 || Object.keys(currentEvents).length > 0

    return (
        <div data-type='job-tasks' className='flex-1 w-full flex-col min-h-0 overflow-auto'>
            <div className='min-h-320px'>
                <TaskListCard />
            </div>
            <div className='h-580px content-full'>
                {hasSource && (
                    <Tabs
                        onChange={(args) => {
                            updateQuery({
                                active: args.activeKey,
                            })
                        }}
                        activeKey={activeKey}
                        overrides={mergeOverrides({
                            TabBar: {
                                style: {
                                    'backgroundColor': 'none',
                                    'paddingLeft': 0,
                                    '& > div:first-child': {
                                        borderTopLeftRadius: '4px',
                                    },
                                    '& > div': {
                                        borderTopRightRadius: '4px',
                                        borderRight: '1px solid #E2E7F0',
                                    },
                                },
                            },
                            Tab: {
                                style: ({ $active }) => {
                                    return {
                                        position: 'relative',
                                        zIndex: 1,
                                        marginBottom: '-1px',
                                        backgroundColor: $active ? '#fff' : '#F7F8FA;',
                                        color: $active ? '#2B65D9' : 'rgba(2,16,43,0.60)',
                                        width: '72px',
                                        textAlign: 'center',
                                        paddingTop: '12px',
                                        paddingBottom: '12px',
                                        lineHeight: '1',
                                        marginLeft: 0,
                                        marginRight: 0,
                                        borderBottom: $active ? '0px' : '1px solid #E2E7F0',
                                        borderTop: '1px solid #E2E7F0',
                                        borderLeft: '1px solid #E2E7F0',
                                    }
                                },
                            },
                            Root: {
                                style: {
                                    height: '100%',
                                    flex: 1,
                                    flexDirection: 'column',
                                    // overflow: 'hidden',
                                    position: 'relative',
                                },
                            },
                            TabContent: {
                                style: () => {
                                    return {
                                        flex: 1,
                                        flexDirection: 'column',
                                        position: 'relative',
                                        overflow: 'auto',
                                        ...expandBorder('1px', 'solid', '#E2E7F0'),
                                        ...expandPadding('20px', '20px', '20px', '20px'),
                                    }
                                },
                            },
                        })}
                    >
                        <Tab key={ActiveType.LOG} title={t('job.tasks.tab.log')}>
                            <div className='flex flex-col flex-1 relative'>
                                <TaskLogViewer taskId={taskId} sources={currentLogFiles} activeKey={activeKey} />
                            </div>
                        </Tab>
                        <Tab key={ActiveType.EVENT} title={t('job.tasks.tab.event')}>
                            <TaskEventListCard sources={currentEvents} />
                        </Tab>
                    </Tabs>
                )}
            </div>
        </div>
    )
}
