import { useFetchReport } from '@/domain/report/hooks/useReport'
import User from '@/domain/user/components/User'
import useTranslation from '@/hooks/useTranslation'
import Input from '@starwhale/ui/Input'
import React, { useEffect } from 'react'
import { useHistory, useParams } from 'react-router-dom'
import { formatTimestampDateTime } from '@/utils/datetime'
import { Button, IconFont } from '@starwhale/ui'
import TiptapEditor, { SaveStatus } from '@starwhale/ui/TiptapEditor'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import { Breadcrumbs } from 'baseui/breadcrumbs'
import { INavItem } from '@/components/BaseSidebar'
import Avatar from '@/components/Avatar'
import { createReport, updateReport } from '@/domain/report/services/report'
import { toaster } from 'baseui/toast'

export default function ReportEdit() {
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    const { projectId, reportId } = useParams<{
        projectId: string
        reportId: string
    }>()

    const rid = reportId === 'new' ? '' : reportId
    const info = useFetchReport(projectId, rid)
    // eslint-disable-next-line
    const { currentUser } = useCurrentUser()
    const [title, setTitle] = React.useState('')
    const [description, setDescription] = React.useState('')
    const [content, setContent] = React.useState('')
    const [t] = useTranslation()
    const { data } = info
    const history = useHistory()
    const [readonly, setReadonly] = React.useState(false)
    const [status, setStatus] = React.useState(SaveStatus.SAVED)
    const statusT = {
        [SaveStatus.SAVED]: t('report.save.saved'),
        [SaveStatus.SAVING]: t('report.save.saving'),
        [SaveStatus.UNSAVED]: t('report.save.unsaved'),
    }
    const handleSave = React.useCallback(() => {
        const contentString = JSON.stringify(content)
        async function save() {
            await createReport(projectId, {
                title,
                description,
                content: contentString,
            })
        }

        async function update() {
            await updateReport(projectId, rid, {
                title,
                description,
                content: contentString,
            })
        }
        if (!title) {
            toaster.negative(t('report.title.error.required'), { autoHideDuration: 2000 })
            return
        }

        if (!rid)
            save().then(() => {
                // history.push(`/projects/${projectId}/reports`)
                toaster.negative(t('report.publist.publised'), { autoHideDuration: 2000 })
            })
        else
            update().then(() => {
                toaster.negative(t('report.publist.publised'), { autoHideDuration: 2000 })
            })
    }, [projectId, t, rid, title, description, content])

    useEffect(() => {
        if (data) {
            setTitle(data.title)
            setDescription(data.description ?? '')
            setContent(data.content)
        }
    }, [data])

    // console.log(content)

    return (
        <div className='flex flex-column flex-1'>
            {/* eslint-disable-next-line */}
            <Breadcrumb
                extra={
                    <div className='flex gap-15px items-center'>
                        {statusT[status]}
                        <Button onClick={() => setReadonly(!readonly)}>
                            {readonly ? t('report.mode.edit') : t('report.mode.preview')}
                        </Button>
                        <Button onClick={handleSave} disabled={status !== SaveStatus.SAVED}>
                            {t('report.publish')}
                        </Button>
                    </div>
                }
            />
            <h1 className='mb-20px '>
                <Input
                    overrides={{
                        Root: {
                            style: {
                                borderTopWidth: '0px',
                                borderBottomWidth: '0px',
                                borderLeftWidth: '0px',
                                borderRightWidth: '0px',
                            },
                        },
                        Input: {
                            style: {
                                paddingLeft: 0,
                                paddingRight: 0,
                                fontSize: '34px',
                                fontWeight: 600,
                                color: '#02102B',
                            },
                        },
                    }}
                    placeholder={t('report.title.placeholder')}
                    value={title}
                    onChange={(e) => setTitle(e.currentTarget.value)}
                />
            </h1>
            <div className='flex gap-10px items-center font-14px'>
                <div className='flex gap-5px'>
                    <Avatar name={currentUser?.name} size={28} />
                    <User user={!rid ? currentUser : info.data?.owner} />|{' '}
                </div>
                <span style={{ color: 'rgba(2,16,43,0.60)' }}>
                    <IconFont type='time' style={{ marginRight: '4px' }} />
                    {formatTimestampDateTime(data?.createdTime || Date.now())}
                </span>
            </div>
            <p className='mt-20px'>
                <Input
                    overrides={{
                        Root: {
                            style: {
                                borderTopWidth: '0px',
                                borderBottomWidth: '0px',
                                borderLeftWidth: '0px',
                                borderRightWidth: '0px',
                            },
                        },
                        Input: {
                            style: {
                                paddingLeft: 0,
                                paddingRight: 0,
                                fontSize: '16px',
                                color: '#02102B',
                            },
                        },
                    }}
                    placeholder={t('report.description.placeholder')}
                    value={description}
                    onChange={(e) => setDescription(e.currentTarget.value)}
                />
            </p>
            <div className='mb-20px flex-1'>
                <TiptapEditor
                    initialContent={data?.content}
                    onContentChange={(tmp: string) => setContent(tmp)}
                    editable={!readonly}
                    onSaveStatusChange={(tmp: SaveStatus) => setStatus(tmp)}
                />
            </div>
        </div>
    )
}

function Breadcrumb({ extra }: any) {
    const [t] = useTranslation()

    const { projectId, reportId } = useParams<{
        projectId: string
        reportId: string
    }>()
    const history = useHistory()

    const breadcrumbItems: INavItem[] = React.useMemo(() => {
        const items = [
            {
                title: t('Reports'),
                path: `/projects/${projectId}/models`,
            },
            {
                title: reportId === 'new' ? t('report.create.title') : t('report.edit.title'),
                path: `/projects/${projectId}/models/${reportId}`,
            },
        ]
        return items
    }, [projectId, reportId, t])

    return (
        <div style={{ marginBottom: 13, display: 'flex', alignItems: 'center' }}>
            {breadcrumbItems && (
                <div style={{ flexShrink: 0 }}>
                    <Breadcrumbs
                        overrides={{
                            List: {
                                style: {
                                    display: 'flex',
                                    alignItems: 'center',
                                },
                            },
                            ListItem: {
                                style: {
                                    display: 'flex',
                                    alignItems: 'center',
                                },
                            },
                        }}
                    >
                        {breadcrumbItems.map((item, idx) => {
                            const Icon = item.icon
                            return (
                                <div
                                    role='button'
                                    tabIndex={0}
                                    style={{
                                        fontSize: '14px',
                                        display: 'flex',
                                        alignItems: 'center',
                                        gap: 6,
                                        cursor: idx !== breadcrumbItems.length - 1 ? 'pointer' : undefined,
                                    }}
                                    key={item.path}
                                    onClick={
                                        item.path && idx !== breadcrumbItems.length - 1
                                            ? () => {
                                                  if (item.path) {
                                                      history.push(item.path)
                                                  }
                                              }
                                            : undefined
                                    }
                                >
                                    {Icon}
                                    <span>{item.title}</span>
                                </div>
                            )
                        })}
                    </Breadcrumbs>
                </div>
            )}
            <div style={{ flexGrow: 1 }} />
            <div style={{ flexShrink: 0 }}>{extra}</div>
        </div>
    )
}
