import React, { useState } from 'react'
import useTranslation from '@/hooks/useTranslation'
import { useProject } from '@project/hooks/useProject'
import { useJob } from '@/domain/job/hooks/useJob'
import ExposedLink from '@/domain/job/components/ExposedLink'
import { createJobTemplate, doJobAction } from '@job/services/job'
import { useHistory } from 'react-router-dom'
import { toaster } from 'baseui/toast'
import { ButtonGroup, ExtendButton } from '@starwhale/ui/Button'
import { ConfirmButton, Input } from '@starwhale/ui'
import qs from 'qs'
import { JobActionType, JobStatusType } from '@/domain/job/schemas/job'
import { WithCurrentAuth, useAccess } from '@/api/WithAuth'
import { Modal, ModalHeader, ModalBody, ModalFooter } from 'baseui/modal'
import _ from 'lodash'
import { IJobVo } from '@/api'
import { useEventCallback } from '@starwhale/core'

function JobSaveAsTemplateButton({ hasText = false }) {
    const [t] = useTranslation()
    const [isShow, setIsShow] = useState(false)
    const [name, setName] = useState('')
    const { job } = useJob()
    const { project } = useProject()
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
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
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

export interface IFineTuneJobActionParams {
    projectId?: string
    jobId?: string
    spaceId?: string
    fineTuneId?: string
    job?: IJobVo
}

export interface IFineTuneJobActionComponentProps {
    hasText?: boolean
}

export interface IFineTuneJobActionsProps {
    hasSaveAs?: boolean
    onRefresh?: () => void
}

export function useFineTuneJobActions({ hasSaveAs = false, onRefresh }: IFineTuneJobActionsProps = {}) {
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

    const getActions = ({ job, projectId, jobId, fineTuneId, spaceId }: IFineTuneJobActionParams = {}) => {
        if (!job) return []

        const CancelButton = {
            access: isAccessCancel,
            component: ({ hasText }: IFineTuneJobActionComponentProps) => (
                <ConfirmButton
                    tooltip={t('Cancel')}
                    icon='cancel'
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() => handleAction(projectId, jobId, JobActionType.CANCEL)}
                    title={t('Cancel.Confirm')}
                >
                    {hasText ? t('Cancel') : undefined}
                </ConfirmButton>
            ),
        }

        const PauseButton = {
            access: isAccessPause && isAccessPauseGlobal,
            component: ({ hasText }: IFineTuneJobActionComponentProps) => (
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
            access: isAccessResume && isAccessResumeGlobal,
            component: ({ hasText }: IFineTuneJobActionComponentProps) => (
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
            access: true,
            component: ({ hasText }: IFineTuneJobActionComponentProps) => (
                <ExtendButton
                    tooltip={!hasText ? t('job.rerun') : undefined}
                    icon='Rerun'
                    styleas={['menuoption', hasText ? undefined : 'highlight']}
                    onClick={() =>
                        history.push(
                            `/projects/${projectId}/new_fine_tune/${spaceId}?${qs.stringify({
                                fineTuneId,
                            })}`
                        )
                    }
                >
                    {hasText ? t('job.rerun') : undefined}
                </ExtendButton>
            ),
        }

        const Saveas = {
            access: hasSaveAs,
            component: ({ hasText }) => <JobSaveAsTemplateButton hasText={hasText} />,
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
                    access: true,
                    component: () => <ExposedLink key={exposed.link} data={exposed} />,
                }
            }) ?? []

        return [...getJobActions(), Rerun, Saveas, ...getExposedLinks()].filter((v) => v.access)
    }

    return {
        getActions,
        renderActionsComponent: (props: IFineTuneJobActionParams) => {
            const actions = getActions(props)
            return actions.map((action, index) => {
                const Component = action.component
                return <Component key={index} />
            })
        },
    }
}

export default function FineTuneJobActionGroup({
    children,
    hasSaveAs,
    ...props
}: IFineTuneJobActionsProps & { children: any }) {
    const { renderActionsComponent } = useFineTuneJobActions({ hasSaveAs })

    return (
        <ButtonGroup key='action'>
            {renderActionsComponent(props)}
            {children}
        </ButtonGroup>
    )
}
