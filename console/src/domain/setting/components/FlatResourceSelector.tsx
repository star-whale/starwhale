import Input from '@starwhale/ui/Input'
import useTranslation from '@/hooks/useTranslation'
import React, { FormEvent, useCallback, useMemo, useState } from 'react'
import { floor } from 'lodash'
import { StatefulTooltip } from 'baseui/tooltip'

interface IResourceType {
    name: string
    max?: number
    min?: number
    defaults?: number
}

export interface Dict<T> {
    [Key: string]: T
}

export interface IDeviceSelectorProps {
    resourceTypes: Array<IResourceType>
    className?: string
    value?: Dict<string>
    onChange?: (value: Dict<string>) => void
}

interface IComponentDesc {
    name: string
    unit: string
    defaults?: string
}

interface IMemoryProps {
    name: string
    value?: string
    placeholder?: string
    onChange?: (value: string) => void
}

const memoryValueRegex = /^(([0-9]*[.])?[0-9]+)([kKmMgG])$/
const Memory = ({ name, value, placeholder, onChange }: IMemoryProps) => {
    const [t] = useTranslation()
    const [error, setError] = useState(false)
    const [$value, setValue] = useState(value)
    const _onChange = useCallback(
        (e: React.ChangeEvent<HTMLInputElement>) => {
            setError(false)
            const val = e.target.value
            setValue(val)

            // empty or positive integer is ok
            if (val === '' || (Number.isInteger(Number(val)) && Number(val) > 0 && !val.includes('.'))) {
                onChange?.(val)
                return
            }
            const match = memoryValueRegex.exec(val)
            if (!match) {
                // pass the original value to the parent
                // and the parent can check if it is numeric
                onChange?.(val)
                setError(true)
                return
            }
            // convert value with unit to integer (Bytes)
            const num = match[1]
            const unit = match[3].toUpperCase()
            const unitMapping: Dict<number> = { K: 1024, M: 1024 * 1024, G: 1024 * 1024 * 1024 }
            const count = floor(Number(num) * unitMapping[unit])
            onChange?.(count.toString())
        },
        [onChange]
    )
    return (
        <Input
            startEnhancer={() => <span>{name.toUpperCase()}</span>}
            value={$value}
            error={error}
            onChange={_onChange}
            placeholder={placeholder ?? t('online eval.memory.placeholder')}
        />
    )
}

export default function FlatResourceSelector({ resourceTypes, className, value, onChange }: IDeviceSelectorProps) {
    const [t] = useTranslation()

    const types: Array<IComponentDesc> = resourceTypes.map((item) => {
        let unit = ''
        if (item.name === 'cpu') {
            unit = t('resource.unit.cpu')
        }
        return { name: item.name, unit, defaults: item.defaults?.toString() }
    })

    const data = useMemo(() => value || {}, [value])
    const _onChange = useCallback(
        (name: string) => {
            return (e: FormEvent<HTMLInputElement> | string) => {
                if (typeof e === 'string') {
                    data[name] = e
                } else {
                    const target = e.target as HTMLInputElement
                    data[name] = target.value
                }
                onChange?.(data)
            }
        },
        [data, onChange]
    )

    return (
        <div className={className}>
            {types.map((i) => (
                <StatefulTooltip key={i.name} content={t('online eval.resource amount.tooltip')} showArrow>
                    <div>
                        {i.name === 'memory' ? (
                            <Memory
                                name={i.name}
                                value={value?.[i.name]}
                                placeholder={i.defaults}
                                onChange={_onChange(i.name)}
                            />
                        ) : (
                            <Input
                                type='number'
                                value={value?.[i.name]}
                                placeholder={i.defaults}
                                startEnhancer={() => <span>{i.name.toUpperCase()}</span>}
                                endEnhancer={() => <span>{i.unit}</span>}
                                onChange={_onChange(i.name) as any}
                            />
                        )}
                    </div>
                </StatefulTooltip>
            ))}
        </div>
    )
}
