import { themedUseStyletron } from '../theme/styletron'
import React from 'react'
import { Input } from './Input'
import IconFont from '../IconFont'

export function QueryInput(props: any) {
    const [css, theme] = themedUseStyletron()
    const [value, setValue] = React.useState('')
    const { onChange } = props

    React.useEffect(() => {
        const timeout = setTimeout(() => onChange(value), 250)
        return () => clearTimeout(timeout)
    }, [onChange, value])

    return (
        <Input
            overrides={{
                Before: function Before() {
                    return (
                        <div
                            className={css({
                                alignItems: 'center',
                                display: 'flex',
                                paddingLeft: theme.sizing.scale500,
                            })}
                        >
                            <IconFont type='search' kind='gray' />
                        </div>
                    )
                },
            }}
            onChange={(event) => setValue((event.target as HTMLInputElement).value)}
            value={value}
            clearable
            placeholder={props.placeholder}
        />
    )
}
