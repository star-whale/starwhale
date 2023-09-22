import React, { useCallback, useState } from 'react'
import useTranslation from '@/hooks/useTranslation'
import { useProject } from '@project/hooks/useProject'
import Card from '@/components/Card'
import { ICreateProjectSchema } from '@project/schemas/project'
import { fetchProject, changeProject } from '@project/services/project'
import { Modal, ModalBody, ModalHeader } from 'baseui/modal'
import ProjectForm from '@project/components/ProjectForm'
import { StatefulTooltip } from 'baseui/tooltip'
import { useHistory } from 'react-router-dom'
import { IProjectSchema } from '@/domain/project/schemas/project'
import { createUseStyles } from 'react-jss'
import { useFetchProjectMembers } from '@/domain/project/hooks/useFetchProjectMembers'
import { useQuery } from 'react-query'
import Avatar from '@/components/Avatar'
import WithAuth from '@/api/WithAuth'
import { useFetchProjectRole } from '@/domain/project/hooks/useFetchProjectRole'
import { Button } from '@starwhale/ui'

type IProjectCardProps = {
    project: IProjectSchema
    onEdit?: () => void
}

const useCardStyles = createUseStyles({
    card: {
        display: 'flex',
        flexDirection: 'column',
        gap: '22px',
    },
    row: {
        display: 'flex',
        fontSize: '14px',
        lineHeight: '14px',
        alignItems: 'center',
    },
    rowKey: {
        color: 'rgba(2,16,43,0.60)',
        marginRight: '8px',
    },
    rowValue: {
        display: 'flex',
        alignItems: 'center',
        color: '#02102B',
    },
    rowEnd: {
        marginLeft: 'auto',
    },
    tag: {
        fontSize: '12px',
        color: '#00B368',
        backgroundColor: '#E6FFF4',
        borderRadius: '9px',
        padding: '3px 10px',
    },
    divider: {
        height: '1px',
        width: '100%',
        backgroundColor: '#EEF1F6',
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

    return (
        <div className={styles.card}>
            <div className={styles.row}>
                <div className={styles.rowValue}>{[project.owner?.name, project.name].join('/')}</div>
                <div className={styles.rowEnd}>
                    <WithAuth role={role} id='project.update'>
                        <Button onClick={() => onEdit?.()} icon='edit' kind='secondary'>
                            {t('Edit')}
                        </Button>
                    </WithAuth>
                </div>
            </div>
            <div className={styles.row}>
                <div className={styles.rowKey}>{t('Privacy')}: </div>
                <div className={styles.rowValue}>
                    <p
                        className={styles.tag}
                        style={{
                            color: project?.privacy === 'PRIVATE' ? '#4848B3' : '#00B368',
                            backgroundColor: project?.privacy === 'PRIVATE' ? '#EDEDFF' : '#E6FFF4',
                        }}
                    >
                        {project.privacy === 'PRIVATE' ? t('Private') : t('Public')}
                    </p>
                </div>
            </div>
            <div className={styles.row}>
                <div className={styles.rowKey}>{t('Description')}</div>
                <div className={styles.rowValue}>
                    <StatefulTooltip content='desc' placement='bottom'>
                        {project.description ?? ' '}
                    </StatefulTooltip>
                </div>
            </div>
            <div className={styles.divider} />
            <div className={styles.row}>
                <div className={styles.rowKey}>{t('Member')}: </div>
                <div className={styles.rowEnd}>
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
            </div>
            <div className={styles.row}>
                <div className={styles.memberWrapper}>
                    {members.data?.map((member, i) => (
                        <Avatar key={i} name={member.user.name} />
                    ))}
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
