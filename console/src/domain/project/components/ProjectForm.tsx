import React, { useCallback, useEffect, useState, useMemo } from 'react'
import { createForm } from '@/components/Form'
import useTranslation from '@/hooks/useTranslation'
import { Button } from 'baseui/button'
import { isModified } from '@/utils'
import Select from '@starwhale/ui/Select'
import { RadioGroup, Radio } from '@starwhale/ui/Radio'
import { FormControl } from 'baseui/form-control'
import { Textarea } from 'baseui/textarea'
import { useCurrentUser } from '@/hooks/useCurrentUser'
import { IUserSchema } from '@user/schemas/user'
import { createUseStyles } from 'react-jss'
import Input from '@starwhale/ui/Input'
import { ICreateProjectSchema, IProjectSchema } from '../schemas/project'
import Checkbox from '@starwhale/ui/Checkbox'
import { useToggle } from 'ahooks'

const { Form, FormItem } = createForm<ICreateProjectSchema>()

const useStyles = createUseStyles({
    project: {},
    projectName: {
        display: 'flex',
        alignContent: 'stretch',
        alignItems: 'flex-start',
    },
})

export interface IProjectFormProps {
    project?: IProjectSchema
    onSubmit: (data: ICreateProjectSchema) => Promise<void>
}

interface IControlledProps {
    value?: string
    onChange?: (val: string) => void
}

type IVisibilityProps = IControlledProps

const Visibility = ({ value, onChange }: IVisibilityProps) => {
    interface IVisibilityItemProps {
        name: string
        desc: string
    }

    const [t] = useTranslation()
    const visPublic: IVisibilityItemProps = useMemo(
        () => ({
            name: 'PUBLIC',
            desc: t('Public Project Desc'),
        }),
        [t]
    )
    const visPrivate: IVisibilityItemProps = useMemo(
        () => ({
            name: 'PRIVATE',
            desc: t('Private Project Desc'),
        }),
        [t]
    )

    const [visibility, setVisibility] = useState<IVisibilityItemProps>(visPublic)
    useEffect(() => {
        const v = value === visPublic.name ? visPublic : visPrivate
        setVisibility(v)
    }, [value, visPublic, visPrivate])

    return (
        <FormControl caption={visibility.desc}>
            <RadioGroup
                align='horizontal'
                value={visibility.name}
                onChange={(e) => {
                    onChange?.(e.target.value)
                }}
            >
                <Radio value={visPublic.name}>{t('Public')}</Radio>
                <Radio value={visPrivate.name}>{t('Private')}</Radio>
            </RadioGroup>
        </FormControl>
    )
}

type IOwnerProps = IControlledProps & {
    data?: IUserSchema
}

const Owner = ({ value, onChange, data }: IOwnerProps) => {
    return (
        <Select
            options={[{ label: data?.name, id: data?.id }]}
            value={[{ id: value }]}
            clearable={false}
            onChange={(o) => onChange?.(o.option?.id as string)}
        />
    )
}

export default function ProjectForm({ project, onSubmit }: IProjectFormProps) {
    const [t] = useTranslation()
    // eslint-disable-next-line react-hooks/exhaustive-deps
    const { currentUser } = useCurrentUser()
    const styles = useStyles()

    const user = React.useMemo(() => {
        return project?.owner ?? currentUser
    }, [project, currentUser])

    const [values, setValues] = useState<ICreateProjectSchema | undefined>({
        ownerId: user?.id,
        projectName: project?.name ?? '',
        privacy: project?.privacy ?? 'PRIVATE',
        description: project?.description ?? '',
    })

    useEffect(() => {
        if (!project) {
            return
        }
        setValues({
            projectName: project.name,
        })
    }, [project])
    const [checked, { toggle }] = useToggle(false)

    const [loading, setLoading] = useState(false)

    const handleValuesChange = useCallback((_changes, values_) => {
        setValues(values_)
    }, [])

    const handleFinish = useCallback(
        async (values_) => {
            setLoading(true)
            try {
                await onSubmit(values_, checked)
            } finally {
                setLoading(false)
            }
        },
        [onSubmit, checked]
    )

    return (
        <Form
            className={styles.project}
            initialValues={values}
            onFinish={handleFinish}
            onValuesChange={handleValuesChange}
        >
            <div className={styles.projectName}>
                <FormItem
                    key='ownerId'
                    name='ownerId'
                    label={t('Owner')}
                    required
                    style={{
                        flexBasis: '160px',
                        marginBottom: 0,
                    }}
                >
                    <Owner data={user} />
                </FormItem>
                <div
                    style={{
                        margin: '38px 6px 0px',
                    }}
                >
                    /
                </div>
                <FormItem key='projectName' name='projectName' label={t('sth name', [t('Project')])} required>
                    <Input size='compact' />
                </FormItem>
            </div>
            <FormItem name='privacy' key='privacy'>
                <Visibility />
            </FormItem>
            <FormItem name='description' key='description' label={t('Description')}>
                <Textarea
                    overrides={{
                        // @ts-ignore
                        Root: {
                            style: {
                                borderTopWidth: '1px',
                                borderBottomWidth: '1px',
                                borderLeftWidth: '1px',
                                borderRightWidth: '1px',
                            },
                        },
                    }}
                />
            </FormItem>
            <div className='my-20px flex flex-col gap-8px'>
                <Checkbox checked={checked} onChange={(v) => toggle(v)}>
                    {t('project.readme.add')}
                </Checkbox>
                <p>{t('project.readme.desc')}</p>
            </div>

            <FormItem key='submit'>
                <div style={{ display: 'flex' }}>
                    <div style={{ flexGrow: 1 }} />
                    <Button size='compact' isLoading={loading} disabled={!isModified(project, values)}>
                        {t('submit')}
                    </Button>
                </div>
            </FormItem>
        </Form>
    )
}
