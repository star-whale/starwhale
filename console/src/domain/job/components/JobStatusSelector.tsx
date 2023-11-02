import Select, { ISelectProps } from '@starwhale/ui/Select'
import _ from 'lodash'
import React, { useState } from 'react'
import useTranslation from '@/hooks/useTranslation'
import { JobStatusType } from '../schemas/job'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import { Tag } from 'baseui/tag'
import { expandBorderRadius } from '@starwhale/ui/utils'
import IconFont from '@starwhale/ui/IconFont'

export interface IJobStatusSelectorProps {
    value?: string[]
    onChange?: (newValue: string[]) => void
    overrides?: ISelectProps['overrides']
    disabled?: boolean
    clearable?: boolean
    multiple?: boolean
    placeholder?: React.ReactNode
}

export default function JobStatusSelector({
    value,
    onChange,
    overrides,
    disabled,
    clearable = false,
    multiple = false,
    placeholder,
}: IJobStatusSelectorProps) {
    const [t] = useTranslation()
    const [css] = themedUseStyletron()
    const cls = css({
        'borderRadius': '12px',
        'padding': '3px 10px',
        'fontSize': '12px',
        'lineHeight': '12px',
        'width': 'max-content',
        ':before': {
            content: '"\u2022"',
            display: 'inline-block',
            marginRight: '4px',
        },
    })

    const JOB_STATUS_COLOR = {
        [JobStatusType.CREATED]: '#2B65D9',
        [JobStatusType.PAUSED]: '#6C48B3',
        [JobStatusType.RUNNING]: '#E67F17',
        [JobStatusType.CANCELLING]: '#E67F17',
        [JobStatusType.CANCELED]: '#4D576A',
        [JobStatusType.SUCCESS]: '#00B368',
        [JobStatusType.FAIL]: '#CC3D3D',
        [JobStatusType.READY]: '#4D576A',
    }

    const JOB_STATUS_BACKGROUND_COLOR = {
        [JobStatusType.CREATED]: '#EBF1FF',
        [JobStatusType.PAUSED]: '#F3EDFF',
        [JobStatusType.RUNNING]: '#FFF3E8',
        [JobStatusType.CANCELLING]: '#FFF3E8',
        [JobStatusType.CANCELED]: '#EBF1FF',
        [JobStatusType.SUCCESS]: '#E6FFF4',
        [JobStatusType.FAIL]: '#FFEDED',
        [JobStatusType.READY]: '#EBF1FF',
    }

    const JOB_STATUS = {
        [JobStatusType.CREATED]: (
            <p className={cls} style={{ color: '#2B65D9', backgroundColor: '#EBF1FF' }}>
                {t('job.status.created')}
            </p>
        ),
        [JobStatusType.PAUSED]: (
            <p className={cls} style={{ color: '#6C48B3', backgroundColor: '#F3EDFF' }}>
                {t('job.status.paused')}
            </p>
        ),
        [JobStatusType.RUNNING]: (
            <p className={cls} style={{ color: '#E67F17', backgroundColor: '#FFF3E8' }}>
                {t('job.status.running')}
            </p>
        ),
        [JobStatusType.CANCELLING]: (
            <p className={cls} style={{ color: '#E67F17', backgroundColor: '#FFF3E8' }}>
                {t('job.status.cancelling')}
            </p>
        ),
        [JobStatusType.CANCELED]: (
            <p className={cls} style={{ color: '#4D576A', backgroundColor: '#EBF1FF' }}>
                {t('job.status.cancelled')}
            </p>
        ),
        [JobStatusType.SUCCESS]: (
            <p className={cls} style={{ color: '#00B368', backgroundColor: '#E6FFF4' }}>
                {t('job.status.succeeded')}
            </p>
        ),
        [JobStatusType.FAIL]: (
            <p className={cls} style={{ color: '#CC3D3D', backgroundColor: '#FFEDED' }}>
                {t('job.status.fail')}
            </p>
        ),
        [JobStatusType.READY]: (
            <p className={cls} style={{ color: '#4D576A', backgroundColor: '#EBF1FF' }}>
                {t('job.status.ready')}
            </p>
        ),
    }

    const defaultOverrides = {
        Tag: (args) => {
            const { id } = args.value
            return (
                <Tag
                    {...args}
                    overrides={{
                        Root: {
                            style: {
                                cursor: 'pointer',
                                color: 'rgba(2, 16, 43, 0.2)',
                                backgroundColor: JOB_STATUS_BACKGROUND_COLOR[id],
                                marginTop: '2px',
                                marginBottom: '2px',
                                marginRight: '2px',
                                marginLeft: '2px',
                                ...expandBorderRadius('12px'),
                            },
                        },
                        Action: {
                            style: {
                                marginLeft: 0,
                            },
                        },
                        ActionIcon: () => <IconFont type='close' size={12} style={{ color: JOB_STATUS_COLOR[id] }} />,
                    }}
                />
            )
        },
    }

    const [options, setOptions] = useState<{ id: string }[]>(
        Object.entries(JOB_STATUS).map(([id, label]) => ({ id, label }))
    )

    const handleJobStatusInputChange = _.debounce((term: string) => {
        if (!term) {
            setOptions([])
        }
    })

    const $value = React.useMemo(() => {
        if (!value) return []
        if (typeof value === 'string')
            return String(value)
                .split(',')
                .filter((item) => item in JOB_STATUS)
                .map((item: any) => ({ id: item }))

        return (
            value?.map((item) => ({
                id: item,
            })) ?? []
        )
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [value])

    return (
        <Select
            multi={multiple}
            placeholder={placeholder ?? t('job.status.selector.placeholder')}
            disabled={disabled}
            overrides={{
                ...overrides,
                ...defaultOverrides,
            }}
            clearable={clearable}
            searchable={false}
            options={options}
            onChange={(params) => {
                if (params.type === 'clear') {
                    onChange?.([])
                    return
                }
                if (!params.option) {
                    return
                }
                onChange?.(params.value.map((item) => (item.id as string) ?? '').filter((name) => name !== ''))
            }}
            onInputChange={(e) => {
                const target = e.target as HTMLInputElement
                handleJobStatusInputChange(target.value)
            }}
            value={$value}
        />
    )
}
