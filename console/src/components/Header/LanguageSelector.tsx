import React from 'react'
import Select from '@starwhale/ui/Select'
import IconFont from '@starwhale/ui/IconFont'
import { languages } from '@/consts'
import i18n from '@/i18n'
import { expandBorder } from '@starwhale/ui/utils'
import useTranslation from '@/hooks/useTranslation'

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
    const [t] = useTranslation()

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
            getValueLabel={() => {
                return (
                    <p style={{ gap: '4px', display: 'flex', color: '#fff', alignItems: 'center', fontSize: '14px' }}>
                        <IconFont type='global2' style={{ color: '#fff' }} size={14} />
                        {languageValue.id === 'en' ? t('en') : t('zh')}
                        <IconFont type='arrow_down' />
                    </p>
                )
            }}
        />
    )
}
