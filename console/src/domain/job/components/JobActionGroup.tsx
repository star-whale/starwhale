import React, { useState } from 'react'
import useTranslation from '@/hooks/useTranslation'
import { useProject } from '@project/hooks/useProject'
import { useJob } from '@/domain/job/hooks/useJob'
import ExposedLink from '@/domain/job/components/ExposedLink'
import { createJobTemplate, doJobAction } from '@job/services/job'
import { useHistory } from 'react-router-dom'
import { toaster } from 'baseui/toast'
import { ButtonGroup, ExtendButton, IExtendButtonProps } from '@starwhale/ui/Button'
import { ConfirmButton, Input } from '@starwhale/ui'
import qs from 'qs'
import { JobActionType, JobStatusType } from '@/domain/job/schemas/job'
import { WithCurrentAuth, useAccess } from '@/api/WithAuth'
import { Modal, ModalHeader, ModalBody, ModalFooter } from 'baseui/modal'
import _ from 'lodash'
import { useEventCallback } from '@starwhale/core/utils'
import { IJobVo, IProjectVo, api } from '@/api'

function JobSaveAsTemplateButton({ hasText = false, job, project }) {
    const as = hasText ? undefined : 'link'
    const kind = hasText ? 'secondary' : undefined
    const sharedProps = { as, kind } as any
    const [t] = useTranslation()
    const [isShow, setIsShow] = useState(false)
    const [name, setName] = useState('')
    const projectId = project?.id
    const jobId = job?.id
    const len = _.toArray(name).length

    return (
        <>
            <WithCurrentAuth id='job.saveas'>
                <ExtendButton
                    tooltip={
                        <div>
                            <div>{t('job.saveas.title')}</div>
                            <div>{t('job.saveas.describe')}</div>
                        </div>
                    }
                    icon='a-saveas'
                    {...sharedProps}
                    onClick={() => setIsShow(true)}
                >
                    {hasText ? t('job.saveas') : ''}
                </ExtendButton>
            </WithCurrentAuth>
            <Modal isOpen={isShow} onClose={() => setIsShow(false)} closeable animate autoFocus>
                <ModalHeader $style={{ display: 'flex', gap: '5px', fontWeight: 'normal' }}>
                    {t('job.saveas.title')}
                </ModalHeader>
                <ModalBody>
                    <div className='flex items-center gap-12px py-47px px-37px'>
                        {t('job.saveas.template.name')} *
                        <div
                            style={{
                                display: 'flex',
                                marginTop: '20px',
                                marginBottom: '20px',
                                gap: '12px',
                                alignItems: 'center',
                                flex: 1,
                            }}
                        >
                            <Input value={name} onChange={(e) => setName(e.target.value)} />
                        </div>
                    </div>
                </ModalBody>
                <ModalFooter>
                    <div style={{ display: 'grid', gap: '20px', gridTemplateColumns: '1fr 79px 79px' }}>
                        <div style={{ flexGrow: 1 }} />
                        <ExtendButton
                            size='default'
                            isFull
                            kind='secondary'
                            onClick={() => {
                                setIsShow(false)
                            }}
                        >
                            {t('Cancel')}
                        </ExtendButton>
                        <ExtendButton
                            size='default'
                            isFull
                            disabled={len < 2 || len > 20}
                            onClick={async () => {
                                if (!jobId || !projectId) return

                                setIsShow(false)
                                await createJobTemplate(projectId, {
                                    name,
                                    jobUrl: jobId,
                                })
                                toaster.positive(t('job.saveas.template.success'), { autoHideDuration: 1000 })
                            }}
                        >
                            {t('submit')}
                        </ExtendButton>
                    </div>
                </ModalFooter>
            </Modal>
        </>
    )
}

export interface IJobActionsProps {
    hasSaveAs?: boolean
    hasText?: boolean
    onRefresh?: () => void
}

export interface IJobActionParams {
    job?: IJobVo
    project?: IProjectVo
}

export interface IJobActionComponentProps {
    hasText?: boolean
    hasIcon?: boolean
    styleas?: IExtendButtonProps['styleas']
    onDone?: () => void
}

export interface IJobAction {
    access: boolean
    key?: string
    component: React.FC<IJobActionComponentProps>
}

export function useJobActions({ hasSaveAs = false, onRefresh }: IJobActionsProps = {}) {
    const [t] = useTranslation()
    const history = useHistory()
    const handleAction = useEventCallback(async (projectId, jid, type: JobActionType) => {
        if (!projectId) return

        await doJobAction(projectId, jid, type)
        toaster.positive(t('job action done'), { autoHideDuration: 2000 })
        onRefresh?.()
    })
    const isAccessCancel = useAccess('job.cancel')
    const isAccessPause = useAccess('job.pause')
    const isAccessPauseGlobal = useAccess('job-pause')
    const isAccessResume = useAccess('job.resume')
    const isAccessResumeGlobal = useAccess('job-resume')

    const getActions = ({ job, project }: IJobActionParams = {}): IJobAction[] => {
        if (!job || !project) return []
        const projectId = project?.id
        const jobId = job?.id

        const CancelButton = {
            key: 'cancel',
            access: isAccessCancel,
            component: ({ hasText, hasIcon, styleas = [], onDone = () => {} }: IJobActionComponentProps) => (
                <ConfirmButton
                    tooltip={t('Cancel')}
                    icon={hasIcon ? 'cancel' : undefined}
                    styleas={['menuoption', hasText ? undefined : 'highlight', ...styleas]}
                    onClick={() => {
                        handleAction(projectId, jobId, JobActionType.CANCEL)
                        onDone()
                    }}
                    title={t('Cancel.Confirm')}
                >
                    {hasText ? t('Cancel') : undefined}
                </ConfirmButton>
            ),
        }

        const PauseButton = {
            key: 'pause',
            access: isAccessPause && isAccessPauseGlobal,
            component: ({ hasText }: IJobActionComponentProps) => (
                <ConfirmButton
                    tooltip={t('Pause')}
                    icon='pause'
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() => handleAction(projectId, jobId, JobActionType.PAUSE)}
                    title={t('Pause.Confirm')}
                >
                    {hasText ? t('Pause') : undefined}
                </ConfirmButton>
            ),
        }

        const ResumeButton = {
            key: 'resume',
            access: isAccessResume && isAccessResumeGlobal,
            component: ({ hasText }: IJobActionComponentProps) => (
                <ExtendButton
                    tooltip={!hasText ? t('Resume') : undefined}
                    icon='Resume'
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() => handleAction(projectId, jobId, JobActionType.RESUME)}
                >
                    {hasText ? t('Resume') : undefined}
                </ExtendButton>
            ),
        }

        const Rerun = {
            key: 'rerun',
            access: true,
            component: ({ hasText }: IJobActionComponentProps) => (
                <ExtendButton
                    tooltip={!hasText ? t('job.rerun') : undefined}
                    icon='Rerun'
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() => history.push(`/projects/${projectId}/new_job?${qs.stringify({ rid: jobId })}`)}
                >
                    {hasText ? t('job.rerun') : undefined}
                </ExtendButton>
            ),
        }

        const Saveas = {
            key: 'saveas',
            access: hasSaveAs,
            component: ({ hasText }) => <JobSaveAsTemplateButton hasText={hasText} job={job} project={project} />,
        }

        const getJobActions = () => {
            const _actions = {
                [JobStatusType.CREATED]: [CancelButton, PauseButton],
                [JobStatusType.RUNNING]: [CancelButton, PauseButton],
                [JobStatusType.PAUSED]: [CancelButton, ResumeButton],
                [JobStatusType.FAIL]: [ResumeButton],
            }

            return _actions[job?.jobStatus] ?? []
        }

        const getExposedLinks = () =>
            job?.exposedLinks?.map((exposed) => {
                return {
                    key: exposed.type,
                    access: true,
                    component: () => <ExposedLink key={exposed.link} data={exposed} />,
                }
            }) ?? []

        return [...getJobActions(), Rerun, Saveas, ...getExposedLinks()].filter((v) => v.access)
    }

    return {
        getActions,
        renderActionsComponent: (props: IJobActionParams & { hasText?: boolean }) => {
            const actions = getActions(props)
            return actions.map((action, index) => {
                const Component = action.component
                return <Component key={index} hasText={props.hasText} />
            })
        },
    }
}

export function JobActionGroupByJobId({
    children,
    hasSaveAs,
    onRefresh,
    jobId,
    ...props
}: IJobActionsProps & { children?: any; jobId?: string }) {
    const { project } = useProject()
    const job = api.useGetJob(project?.id as string, jobId as string).data
    const { renderActionsComponent } = useJobActions({ hasSaveAs, onRefresh })

    return (
        <ButtonGroup key='action'>
            {renderActionsComponent({ ...props, job, project })}
            {children}
        </ButtonGroup>
    )
}

export default function JobActionGroup({
    children,
    hasSaveAs,
    onRefresh,
    ...props
}: IJobActionsProps & { children?: any }) {
    const { job } = useJob()
    const { project } = useProject()
    const { renderActionsComponent } = useJobActions({ hasSaveAs, onRefresh })

    return (
        <ButtonGroup key='action'>
            {renderActionsComponent({ ...props, job, project })}
            {children}
        </ButtonGroup>
    )
}
