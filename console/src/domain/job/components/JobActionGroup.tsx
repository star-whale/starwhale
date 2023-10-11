import React, { useCallback, useState } from 'react'
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
import { WithCurrentAuth } from '@/api/WithAuth'
import { Modal, ModalHeader, ModalBody, ModalFooter } from 'baseui/modal'
import _ from 'lodash'

function JobSaveAsTemplateButton({ hasText = false }) {
    const as = hasText ? undefined : 'link'
    const kind = hasText ? 'secondary' : undefined
    const sharedProps = { as, kind } as any
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
                    <div className='flex items-center gap-12px py-47px px-37px '>
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

export default function JobActionGroup({
    children,
    hasText = false,
    hasSaveAs = false,
}: {
    children?: React.ReactNode
    hasText?: boolean
    hasSaveAs?: boolean
}) {
    const { job } = useJob()
    const { project } = useProject()
    const [t] = useTranslation()
    const projectId = project?.id
    const jobId = job?.id
    const history = useHistory()

    const handleAction = useCallback(
        async (jid, type: JobActionType) => {
            if (!projectId) return

            await doJobAction(projectId, jid, type)
            toaster.positive(t('job action done'), { autoHideDuration: 2000 })
        },
        [projectId, t]
    )

    if (!job) {
        return null
    }

    const as = hasText ? undefined : 'link'
    const kind = hasText ? 'secondary' : undefined
    const sharedProps = { as, kind } as any

    const CancelButton = () => (
        <WithCurrentAuth id='job.cancel'>
            <ConfirmButton
                tooltip={t('Cancel')}
                icon='cancel'
                {...sharedProps}
                onClick={() => handleAction(jobId, JobActionType.CANCEL)}
                title={t('Cancel.Confirm')}
            >
                {hasText ? t('Cancel') : ''}
            </ConfirmButton>
        </WithCurrentAuth>
    )

    const PauseButton = () => (
        <WithCurrentAuth id='job-pause'>
            <WithCurrentAuth id='job.pause'>
                <ConfirmButton
                    tooltip={t('Pause')}
                    icon='pause'
                    {...sharedProps}
                    onClick={() => handleAction(jobId, JobActionType.PAUSE)}
                    title={t('Pause.Confirm')}
                >
                    {hasText ? t('Pause') : ''}
                </ConfirmButton>
            </WithCurrentAuth>
        </WithCurrentAuth>
    )

    const ResumeButton = () => (
        <WithCurrentAuth id='job-resume'>
            <WithCurrentAuth id='job.resume'>
                <ExtendButton
                    tooltip={t('Resume')}
                    icon='Resume'
                    {...sharedProps}
                    onClick={() => handleAction(jobId, JobActionType.RESUME)}
                >
                    {hasText ? t('Resume') : ''}
                </ExtendButton>
            </WithCurrentAuth>
        </WithCurrentAuth>
    )

    const actions: Partial<Record<JobStatusType, React.ReactNode>> = {
        [JobStatusType.CREATED]: (
            <>
                <CancelButton />
                <PauseButton />
            </>
        ),
        [JobStatusType.RUNNING]: (
            <>
                <CancelButton />
                <PauseButton />
            </>
        ),
        [JobStatusType.PAUSED]: (
            <>
                <CancelButton />
                <ResumeButton />
            </>
        ),
        [JobStatusType.FAIL]: (
            <>
                <ResumeButton />
            </>
        ),
    }

    const rerun = (
        <ExtendButton
            tooltip={t('job.rerun')}
            icon='Rerun'
            {...sharedProps}
            onClick={() =>
                history.push(
                    `/projects/${projectId}/new_job?${qs.stringify({
                        rid: job?.id,
                    })}`
                )
            }
        >
            {hasText ? t('job.rerun') : ''}
        </ExtendButton>
    )

    return (
        <ButtonGroup key='action'>
            {actions[job.jobStatus] ?? ''}
            {rerun}
            {job.exposedLinks?.map((exposed) => (
                <ExposedLink key={exposed.link} data={exposed} />
            ))}
            {hasSaveAs && <JobSaveAsTemplateButton hasText={hasText} />}
            {children}
        </ButtonGroup>
    )
}
