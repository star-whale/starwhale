import useLocaleConst from '@/hooks/useLocaleConst'
import useTranslation from '@/hooks/useTranslation'
import { Button, ButtonLink } from '@starwhale/ui'
import React from 'react'
import { useLocalStorage } from 'react-use'

const QuickStart = () => {
    const [t] = useTranslation()
    const [isNoticeClosed, setIsNoticeClosed] = useLocalStorage('isQuickstartNoticeClosed', false)
    const v = useLocaleConst('quickstart')

    const handleClose = () => {
        setIsNoticeClosed(true)
    }

    if (isNoticeClosed) {
        return null
    }

    return (
        <div className='notice-card relative bg-white card-shadow rounded-6px w-full p-20px mb-12px mt-2px pb-40px'>
            <div className='notice-header mb-12px font-bold text-m'>
                <h3>{t('quickstart.welcome.title')}</h3>
            </div>
            <div className='absolute right-10px top-10px '>
                <Button as='transparent' kind='secondary' icon='close' onClick={handleClose} />
            </div>
            <p>{t('quickstart.welcome.desc')}</p>
            <p>{t('quickstart.welcome.desc1')}</p>
            <div className='flex justify-end absolute bottom-25px right-20px'>
                <ButtonLink target='_blank' href={v}>
                    {t('quickstart.button')}
                </ButtonLink>
            </div>
        </div>
    )
}

export default QuickStart
