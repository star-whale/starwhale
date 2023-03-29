import useTranslation from '@/hooks/useTranslation'
import IconFont from '@starwhale/ui/IconFont'
import React from 'react'

export function Shared({ shared = 0, isTextShow = false }: { shared?: number; isTextShow?: boolean }) {
    const [t] = useTranslation()

    return (
        <div style={{ display: 'inline-flex', gap: '4px', alignItems: 'center' }}>
            {shared === 1 && (
                <IconFont type='group' style={{ borderRadius: '2px', backgroundColor: '#E6FFF4', color: '#00B368' }} />
            )}
            {isTextShow && (shared === 1 ? t('dataset.overview.shared.yes') : t('dataset.overview.shared.no'))}
        </div>
    )
}
export default Shared
