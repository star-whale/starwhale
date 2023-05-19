import useTranslation from '@/hooks/useTranslation'
import IconFont from '@starwhale/ui/IconFont'
import React from 'react'

export function Shared({ shared = 0, isTextShow = false }: { shared?: number; isTextShow?: boolean }) {
    const [t] = useTranslation()

    if (shared === 0 && !isTextShow) return null

    return (
        <div style={{ display: 'inline-flex', gap: '4px', alignItems: 'center' }}>
            {shared === 1 && (
                <div
                    style={{
                        width: '16px',
                        height: '16px',
                        borderRadius: '2px',
                        backgroundColor: '#E6FFF4',
                        display: 'grid',
                        placeItems: 'center',
                    }}
                >
                    <IconFont
                        type='group'
                        size={12}
                        style={{
                            color: '#00B368',
                            margin: '0 auto',
                        }}
                    />
                </div>
            )}
            {isTextShow && (shared === 1 ? t('dataset.overview.shared.yes') : t('dataset.overview.shared.no'))}
        </div>
    )
}
export default Shared
