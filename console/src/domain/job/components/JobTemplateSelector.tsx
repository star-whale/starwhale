import Select from '@starwhale/ui/Select'
import _ from 'lodash'
import React, { useEffect, useState } from 'react'
import { useQuery } from 'react-query'
import { listJobRecentTemplate, listJobTemplate } from '../services/job'
import useTranslation from '@/hooks/useTranslation'
import { StyledList, StyledEmptyState, OptionListProps } from 'baseui/menu'
import { StyledDropdownListItem } from 'baseui/select'
import classNames from 'classnames'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import { ExtendButton } from '@starwhale/ui'

export interface IRuntimeVersionSelectorProps {
    projectId: string
    value?: string
    onChange?: (newValue: string) => void
    disabled?: boolean
}

export default function JobTemplateSelector({ projectId, value, onChange, disabled }: IRuntimeVersionSelectorProps) {
    const [options, setOptions] = useState<{ id: string; label: React.ReactNode }[]>([])
    const [isRecent, setIsRecent] = useState(true)
    const info = useQuery(
        `listJobTemplate:${projectId}:${isRecent}`,
        () => (isRecent ? listJobRecentTemplate(projectId) : listJobTemplate(projectId)),
        {}
    )

    const handleInputChange = _.debounce((term: string) => {
        if (!term) {
            setOptions([])
        }
    })

    useEffect(() => {
        if (info.isSuccess) {
            const ops =
                info.data?.map((item) => ({
                    id: item.id,
                    label: item.name,
                })) ?? []
            setOptions(ops as any)
        } else {
            setOptions([])
        }
    }, [info.data, info.isSuccess])

    return (
        <Select
            disabled={disabled}
            isLoading={info.isFetching}
            options={options}
            clearable={false}
            overrides={{
                DropdownContainer: {
                    style: {
                        maxHeight: '40vh',
                    },
                },
                Dropdown: {
                    props: {
                        setIsRecent: () => setIsRecent(true),
                        setIsAll: () => setIsRecent(false),
                        isRecent,
                    },
                    // eslint-disable-next-line @typescript-eslint/no-use-before-define
                    component: Dropdown,
                },
            }}
            onChange={(params) => {
                if (!params.option) {
                    return
                }
                onChange?.(params.option.id as string)
            }}
            onInputChange={(e) => {
                const target = e.target as HTMLInputElement
                handleInputChange(target.value)
            }}
            value={
                value
                    ? [
                          {
                              id: value,
                          },
                      ]
                    : []
            }
        />
    )
}

const ListItem = ({
    data,
    index,
    style,
}: {
    data: { props: OptionListProps }[]
    index: number
    style: React.CSSProperties
}) => {
    const [css] = themedUseStyletron()
    // eslint-disable-next-line
    const { item, overrides, ...restChildProps } = data[index].props

    return (
        <StyledDropdownListItem
            className={classNames(
                'text-ellipsis',
                css({
                    'boxSizing': 'border-box',
                    ':hover': {
                        backgroundColor: '#EBF1FF',
                    },
                })
            )}
            style={style}
            title={item.label}
            // eslint-disable-next-line
            {..._.omit(restChildProps, ['resetMenu', 'renderAll', 'renderHrefAsAnchor', 'getItemLabel'])}
            key={item.id}
        >
            {item.label}
        </StyledDropdownListItem>
    )
}

const Dropdown = React.forwardRef((props: any, ref) => {
    const overrides = {
        BaseButton: {
            style: {
                'paddingTop': '8px',
                'paddingBottom': '8px',
                'paddingLeft': '16px',
                'paddingRight': '16px',
                'backgroundColor': '#fff',
                ':hover': {
                    backgroundColor: '#EBF1FF',
                },
                ':focus': {
                    backgroundColor: '#EBF1FF',
                },
            },
        },
    }
    const highlight = {
        BaseButton: {
            style: {
                'paddingTop': '8px',
                'paddingBottom': '8px',
                'paddingLeft': '16px',
                'paddingRight': '16px',
                'backgroundColor': '#EBF1FF',
                ':hover': {
                    backgroundColor: '#EBF1FF',
                },
                ':focus': {
                    backgroundColor: '#EBF1FF',
                },
            },
        },
    }
    const { isRecent } = props
    const [t] = useTranslation()

    const children = React.Children.toArray(props.children)
    let items = null
    // @ts-ignore
    if (!children[0] || !children[0].props.item) {
        // @ts-ignore
        items = (
            <StyledEmptyState
                $style={{
                    boxShadow: 'none',
                }}
            />
        )
    } else {
        items = props.children.map((args: any, index: number) => (
            <ListItem key={index} index={index} data={props.children} style={args.style} />
        ))
    }

    return (
        <StyledList ref={ref as any}>
            <div className='flex gap-10px pb-8px px-12px justify-start b-b-1 border-["#EEF1F6"] '>
                <ExtendButton
                    as='link'
                    // @ts-ignore
                    overrides={isRecent ? highlight : overrides}
                    onClick={props.setIsRecent}
                >
                    {t('job.form.template.recently')}
                </ExtendButton>
                <ExtendButton
                    as='link'
                    // @ts-ignore
                    overrides={!isRecent ? highlight : overrides}
                    onClick={props.setIsAll}
                >
                    {t('job.form.template.all')}
                </ExtendButton>
            </div>
            {items}
        </StyledList>
    )
})
