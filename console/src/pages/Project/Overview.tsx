import React, { useCallback, useEffect, useReducer, useState } from 'react'
import useTranslation from '@/hooks/useTranslation'
import { useProject } from '@project/hooks/useProject'
import Card from '@/components/Card'
import { ICreateProjectSchema } from '@project/schemas/project'
import { fetchProject, changeProject, fetchProjectReadme } from '@project/services/project'
import { Modal, ModalBody, ModalHeader } from 'baseui/modal'
import ProjectForm from '@project/components/ProjectForm'
import { useHistory } from 'react-router-dom'
import { IProjectSchema } from '@/domain/project/schemas/project'
import { createUseStyles } from 'react-jss'
import { useFetchProjectMembers } from '@/domain/project/hooks/useFetchProjectMembers'
import { useQuery } from 'react-query'
import Avatar from '@/components/Avatar'
import WithAuth from '@/api/WithAuth'
import { useFetchProjectRole } from '@/domain/project/hooks/useFetchProjectRole'
import { Button, ExtendButton } from '@starwhale/ui'
import { formatTimestampDateTime } from '@/utils/datetime'
import TiptapEditor from '@starwhale/ui/TiptapEditor'
import _ from 'lodash'
import { useEventCallback } from '@starwhale/core'

type IProjectCardProps = {
    project: IProjectSchema
    onEdit?: () => void
}

const useCardStyles = createUseStyles({
    row: {
        display: 'flex',
        fontSize: '14px',
        lineHeight: '14px',
        alignItems: 'center',
    },
    rowKey: {
        color: 'rgba(2,16,43,0.60)',
        marginRight: '8px',
        flexShrink: 0,
    },
    memberWrapper: {
        display: 'flex',
        gap: '21px',
    },
    member: {
        width: '34px',
        height: '34px',
        display: 'grid',
        placeItems: 'center',
        backgroundColor: 'rgba(38,68,128,0.4)',
        borderRadius: '50%',
        color: '#fff',
    },
})

const ProjectCard = ({ project, onEdit }: IProjectCardProps) => {
    const [t] = useTranslation()
    const styles = useCardStyles()
    const history = useHistory()
    const members = useFetchProjectMembers(project.id)
    const { role } = useFetchProjectRole(project.id)
    const info = useQuery([project.id], () => fetchProjectReadme(project.id), {
        enabled: !!project.id,
    })
    const [content, setContent] = React.useState('')
    const [saving, setSaving] = React.useState(false)
    const [count, forceUpdate] = useReducer((x) => x + 1, 0)

    const contentString = React.useMemo(() => (_.isString(content) ? content : JSON.stringify(content)), [content])

    const handleSave = useEventCallback(() => {
        async function update() {
            setSaving(true)
            await changeProject(project.id, {
                readme: contentString,
            })
            await info.refetch()
        }

        update()
            .then(() => {})
            .finally(() => {
                setSaving(false)
            })
    })

    useEffect(() => {
        if (info.data) {
            setContent(info.data)
        }
    }, [info.data])

    const [readonly, setReadonly] = React.useState(true)

    return (
        <div className='flex flex-col flex-1'>
            <div className='flex text-16px color-[#02102b] lh-none font-bold gap-12px my-20px items-center'>
                <div>{[project.owner?.name, project.name].join('/')}</div>
                <p
                    className='text-12px font-normal py-3px px-10px bg-[#E6FFF4] rounded-[9px]'
                    style={{
                        color: project?.privacy === 'PRIVATE' ? '#4848B3' : '#00B368',
                        backgroundColor: project?.privacy === 'PRIVATE' ? '#EDEDFF' : '#E6FFF4',
                    }}
                >
                    {project.privacy === 'PRIVATE' ? t('Private') : t('Public')}
                </p>
            </div>
            <div className='flex flex-1 gap-20px'>
                {/* readme */}
                <div className='flex flex-col flex-1 border-1 border-[#cfd7e6] px-20px py-12px rounded-4px'>
                    <div className='flex justify-between items-center mb-12px'>
                        <div className='flex-1 text-16px font-bold color-[rgba(2,16,43,0.60)]'>
                            {t('project.readme')}
                        </div>
                        <WithAuth role={role} id='project.update'>
                            {readonly ? (
                                <Button onClick={() => setReadonly(false)} icon='edit' kind='secondary'>
                                    {t('Edit')}
                                </Button>
                            ) : (
                                <div className='flex gap-8px'>
                                    {/* cancel */}
                                    <ExtendButton
                                        onClick={() => {
                                            setReadonly(true)
                                            forceUpdate()
                                        }}
                                        kind='secondary'
                                    >
                                        {t('Cancel')}
                                    </ExtendButton>
                                    {/* save  */}
                                    <ExtendButton disabled={saving || contentString === info.data} onClick={handleSave}>
                                        {t('Save')}
                                    </ExtendButton>
                                </div>
                            )}
                        </WithAuth>
                    </div>
                    <TiptapEditor
                        key={count}
                        id={`project-readme-${project.id}`}
                        initialContent={info.data || ''}
                        onContentChange={(tmp: string) => setContent(tmp)}
                        editable={!readonly}
                    />
                </div>
                {/* right */}
                <div className='basis-420px text-14px lh-none'>
                    {/* basic */}
                    <div className='flex justify-between border-t border-[#cfd7e6] items-center pt-20px mb-20px'>
                        <div className='flex-1 text-16px font-bold color-[rgba(2,16,43,0.60)]'>
                            {t('project.overview.basic')}
                        </div>
                        <WithAuth role={role} id='project.update'>
                            <Button onClick={() => onEdit?.()} icon='edit' kind='secondary'>
                                {t('Edit')}
                            </Button>
                        </WithAuth>
                    </div>
                    <div className='flex justify-start my-20px'>
                        <div className={styles.rowKey}>{t('Created At')}:</div>
                        <div>{project?.createdTime ? formatTimestampDateTime(project?.createdTime) : ''}</div>
                    </div>
                    <div className='flex justify-start my-20px'>
                        <div className={styles.rowKey}>{t('Description')}:</div>
                        <div>{project.description || ' '}</div>
                    </div>
                    {/* member */}
                    <div className='flex justify-between border-t border-[#cfd7e6]  items-center pt-20px mb-20px'>
                        <div className='flex-1 text-16px font-bold color-[rgba(2,16,43,0.60)]'>{t('Member')}</div>
                        <WithAuth role={role} id='member.update'>
                            <Button
                                onClick={() => history.push(`/projects/${project.id}/members`)}
                                icon='a-managemember'
                                kind='secondary'
                            >
                                {t('Manage Member')}
                            </Button>
                        </WithAuth>
                    </div>
                    <div className={styles.row}>
                        <div className={styles.memberWrapper}>
                            {members.data?.map((member, i) => (
                                <Avatar key={i} name={member.user.name} />
                            ))}
                        </div>
                    </div>
                </div>
            </div>
        </div>
    )
}

export default function ProjectOverview() {
    const { project, setProject } = useProject()
    const projectInfo = useQuery(`fetchProject:${project?.id}`, () => fetchProject(project?.id ?? ''), {
        enabled: !!project?.id,
    })

    const [isCreateProjectModalOpen, setIsCreateProjectModalOpen] = useState(false)
    const handleCreateProject = useCallback(
        async (data: ICreateProjectSchema) => {
            if (!project?.id) {
                return
            }
            await changeProject(project.id, data)
            setIsCreateProjectModalOpen(false)
            await projectInfo.refetch()
        },
        [projectInfo, project]
    )

    React.useEffect(() => {
        if (projectInfo.data) {
            setProject(projectInfo.data)
        }
    }, [projectInfo, setProject])

    const [t] = useTranslation()

    return (
        <Card title={[t('Project'), t('Overview')].join('')}>
            {project && <ProjectCard project={project} onEdit={() => setIsCreateProjectModalOpen(true)} />}
            <Modal
                isOpen={isCreateProjectModalOpen}
                onClose={() => setIsCreateProjectModalOpen(false)}
                closeable
                animate
                autoFocus
            >
                <ModalHeader>{t('edit sth', [t('Project')])}</ModalHeader>
                <ModalBody>
                    <ProjectForm project={project} onSubmit={handleCreateProject} />
                </ModalBody>
            </Modal>
        </Card>
    )
}
