import useTranslation from '@/hooks/useTranslation'
import React from 'react'
import { useFetchSystemVersion } from '@/domain/setting/hooks/useSettings'
import Card from '@/components/Card'
import BaseSubLayout from '../BaseSubLayout'
import SettingsSidebar from './SettingsSidebar'

export interface IJobLayoutProps {
    children: React.ReactNode
}

function SettingsOverviewLayout({ children }: IJobLayoutProps) {
    const versionInfo = useFetchSystemVersion()
    const [t] = useTranslation()

    const items = [
        {
            label: t('System Version'),
            value: versionInfo?.data?.version ?? '-',
        },
    ]

    const header = (
        <Card
            style={{
                fontSize: '16px',
                background: 'var(--color-brandBgSecondory4)',
                padding: '12px 20px',
                marginBottom: '10px',
            }}
            bodyStyle={{
                display: 'grid',
                gridTemplateColumns: 'repeat(auto-fit, minmax(400px, 1fr))',
                gap: '12px',
            }}
        >
            {items.map((v) => (
                <div key={v?.label} style={{ display: 'flex', gap: '12px' }}>
                    <div
                        style={{
                            background: 'var(--color-brandBgSecondory)',
                            lineHeight: '24px',
                            padding: '0 12px',
                            borderRadius: '4px',
                        }}
                    >
                        {v?.label}:
                    </div>
                    <div> {v?.value}</div>
                </div>
            ))}
        </Card>
    )
    return (
        <BaseSubLayout header={header} breadcrumbItems={[]} sidebar={SettingsSidebar}>
            {children}
        </BaseSubLayout>
    )
}

export default React.memo(SettingsOverviewLayout)
