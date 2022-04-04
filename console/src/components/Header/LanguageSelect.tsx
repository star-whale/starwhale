import React, { useCallback } from 'react'
import { Select } from 'baseui/select'
import i18n from '@/i18n'
import ReactCountryFlag from 'react-country-flag'

export default function LanguageSelect() {
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    const handleRenderLanguageOption = useCallback(({ option }: any) => {
        return (
            <div>
                {option.flag && <span style={{ marginRight: 8, verticalAlign: 'middle' }}>{option.flag}</span>}
                <span style={{ verticalAlign: 'middle' }}>{option.text}</span>
            </div>
        )
    }, [])

    return (
        <div
            style={{
                width: 140,
            }}
        >
            <Select
                overrides={{
                    ControlContainer: {
                        style: {
                            fontSize: 12,
                        },
                    },
                    InputContainer: {
                        style: {
                            fontSize: 12,
                        },
                    },
                }}
                clearable={false}
                searchable={false}
                size='mini'
                value={[{ id: i18n.language ? i18n.language.split('-')[0] : '' }]}
                onChange={(params) => {
                    if (!params.option?.id) {
                        return
                    }
                    i18n.changeLanguage(params.option?.id as string)
                }}
                getOptionLabel={handleRenderLanguageOption}
                getValueLabel={handleRenderLanguageOption}
                options={[
                    {
                        id: 'en',
                        text: 'English',
                        flag: <ReactCountryFlag countryCode='US' svg />,
                    },
                    {
                        id: 'zh',
                        text: '中文',
                        flag: <ReactCountryFlag countryCode='CN' svg />,
                    },
                    {
                        id: 'ja',
                        text: '日本語',
                        flag: <ReactCountryFlag countryCode='JP' svg />,
                    },
                    {
                        id: 'ko',
                        text: '한국어',
                        flag: <ReactCountryFlag countryCode='KR' svg />,
                    },
                    {
                        id: 'vi',
                        text: 'Tiếng Việt',
                        flag: <ReactCountryFlag countryCode='VN' svg />,
                    },
                ]}
            />
        </div>
    )
}
