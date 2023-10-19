import useTranslation from '@/hooks/useTranslation'
import Button, { ExtendButton } from '@starwhale/ui/Button'
import IconFont from '@starwhale/ui/IconFont'
import React, { Suspense } from 'react'
import { Modal, ModalBody, ModalHeader, ModalFooter } from 'baseui/modal'
import { BusyPlaceholder } from '@starwhale/ui/BusyLoaderWrapper'
import { expandPadding } from '@starwhale/ui/utils'
import useLocaleConst from '@/hooks/useLocaleConst'

const Markdown = React.lazy(() => import('@starwhale/ui/Markdown/Markdown'))

const QuickStartNewModel = () => {
    const [t] = useTranslation()
    const [isOpen, setIsOpen] = React.useState(false)
    const v = useLocaleConst('model')
    const quickStartNewModel = useLocaleConst('quickStartNewModel')

    return (
        <>
            <ExtendButton as='transparent' onClick={() => setIsOpen(true)}>
                <p className='cursor-pointer color-[rgba(2,16,43,0.60)] hover:color-[#5181E0]'>
                    <IconFont type='info' /> {t('quickstart.model.how')}
                </p>
            </ExtendButton>

            <Modal
                isOpen={isOpen}
                onClose={() => setIsOpen(false)}
                closeable
                animate
                autoFocus
                overrides={{
                    Dialog: {
                        style: {
                            width: '800px',
                        },
                    },
                }}
            >
                <ModalHeader>{t('quickstart.model.how')}</ModalHeader>
                <ModalBody
                    $style={{
                        ...expandPadding('20px', '0px', '20px', '0'),
                        fontSize: '14px',
                        color: 'rgba(2,16,43,0.60);',
                    }}
                >
                    <Suspense fallback={<BusyPlaceholder />}>
                        <Markdown>{quickStartNewModel}</Markdown>
                    </Suspense>
                </ModalBody>
                <ModalFooter
                    $style={{
                        ...expandPadding('0px', '0px', '20px', '0'),
                        justifyContent: 'flex-end',
                    }}
                >
                    <a target='_blank' href={v} rel='noreferrer'>
                        <Button size='default' kind='primary' onClick={() => {}}>
                            {t('quickstart.model.button')}{' '}
                        </Button>
                    </a>
                </ModalFooter>
            </Modal>
        </>
    )
}

export default QuickStartNewModel
