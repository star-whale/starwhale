import Select, { ISelectProps } from '@starwhale/ui/Select'
import _ from 'lodash'
import React, { useState } from 'react'
import useTranslation from '@/hooks/useTranslation'
import { JobStatusType } from '../schemas/job'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'

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
    // const JOB_STATUS_COLOR = {
    //     [JobStatusType.CREATED]: '#EBF1FF',
    //     [JobStatusType.PAUSED]: '#F3EDFF',
    //     [JobStatusType.RUNNING]: '#FFF3E8',
    //     [JobStatusType.CANCELED]: '#EBF1FF',
    //     [JobStatusType.FAIL]: '#FFEDED',
    //     [JobStatusType.SUCCESS]: '#E6FFF4',
    // }

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
    }

    const defaultOverrides = {}

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
                .map((item: any) => ({ id: item }))

        return (
            value?.map((item) => ({
                id: item,
            })) ?? []
        )
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
