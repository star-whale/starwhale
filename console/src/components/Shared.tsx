import useTranslation from '@/hooks/useTranslation'
import IconFont from '@starwhale/ui/IconFont'
import React from 'react'

export function Shared({ shared = false, isTextShow = false }: { shared?: boolean; isTextShow?: boolean }) {
    const [t] = useTranslation()

    if (!shared && !isTextShow) return null

    return (
        <div className='shared' style={{ display: 'inline-flex', gap: '4px', alignItems: 'center' }}>
            {shared && (
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
            {isTextShow && (shared ? t('dataset.overview.shared.yes') : t('dataset.overview.shared.no'))}
        </div>
    )
}
export default Shared
