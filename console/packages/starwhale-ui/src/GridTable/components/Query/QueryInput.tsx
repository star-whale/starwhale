import { themedUseStyletron } from '@starwhale/ui/theme/styletron'
import React from 'react'
import { LocaleContext } from 'baseui/locale'
import Input from '@starwhale/ui/Input'

function QueryInput(props: any) {
    const [css, theme] = themedUseStyletron()
    const locale = React.useContext(LocaleContext)
    const [value, setValue] = React.useState('')
    const { onChange } = props

    React.useEffect(() => {
        const timeout = setTimeout(() => onChange(value), 250)
        return () => clearTimeout(timeout)
    }, [onChange, value])

    return (
        <Input
            aria-label={locale.datatable.searchAriaLabel}
            overrides={{
                Before: function Before() {
                    return (
                        <div
                            className={css({
                                alignItems: 'center',
                                display: 'flex',
                                paddingLeft: theme.sizing.scale500,
                            })}
                        />
                    )
                },
            }}
            onChange={(event) => setValue((event.target as HTMLInputElement).value)}
            value={value}
            clearable
        />
    )
}

export { QueryInput }

export default QueryInput
