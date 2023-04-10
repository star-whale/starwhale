import React from 'react'
import Select from '@starwhale/ui/Select'
import IconFont from '@starwhale/ui/IconFont'
import { languages } from '@/consts'
import i18n from '@/i18n'
import { expandBorder } from '@starwhale/ui/utils'

export interface IRoleSelectorProps {
    value?: string
    onChange?: (newValue: string) => void
}

export default function LanguageSelector() {
    const languageValue = React.useMemo(() => {
        const lan = i18n.language
        const lanPrefix = lan.split('-')[0]
        return (
            languages.find((option) => option.id === lan) ||
            languages.find((option) => option.id === lanPrefix) ||
            languages.find((option) => option.id.split('-')[0] === lanPrefix) ||
            languages[0]
        )
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [i18n.language, languages])

    return (
        <Select
            overrides={{
                Root: {
                    style: {
                        width: 'fit-content',
                        backgroundColor: 'transparent',
                        ...expandBorder('0'),
                    },
                },
                ControlContainer: {
                    style: {
                        backgroundColor: 'transparent',
                        ...expandBorder('0'),
                    },
                },
                DropdownContainer: {
                    style: {
                        width: '100%',
                    },
                },
                SelectArrow: () => {
                    return null
                },
            }}
            clearable={false}
            searchable={false}
            value={[languageValue]}
            onChange={(params) => {
                if (!params.option?.id) {
                    return
                }
                i18n.changeLanguage(params.option?.id as string)
            }}
            options={languages}
            getValueLabel={() => <IconFont type='global' style={{ color: '#fff' }} size={18} />}
        />
    )
}
