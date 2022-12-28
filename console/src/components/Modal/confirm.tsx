import React, { useState, useContext, useRef } from 'react'
import { Modal, ModalHeader, ModalBody, ModalFooter } from 'baseui/modal'
import useTranslation from '@/hooks/useTranslation'
import { Button, IButtonProps } from '@starwhale/ui'

export interface IConfirmCtxProviderProps {
    children?: React.ReactNode
}

export interface IShowProps {
    title?: React.ReactNode | string
    content?: React.ReactNode | string
}

export interface IConfirmCtxProps {
    show: (props: IShowProps) => Promise<boolean>
}

const ConfirmCtx = React.createContext<IConfirmCtxProps>({
    show: (): Promise<boolean> => {
        return new Promise<boolean>((r) => r(false))
    },
})

const ConfirmCtxProvider = ({ children }: IConfirmCtxProviderProps) => {
    const [t] = useTranslation()
    const [showModal, setShowModal] = useState(false)
    const [showProps, setShowProps] = useState<IShowProps>({})
    const resolver: React.MutableRefObject<(ok: boolean) => void> = useRef(() => {})

    const show = (props: IShowProps) => {
        setShowProps(props)
        setShowModal(true)
        return new Promise<boolean>((resolve) => {
            resolver.current = resolve
        })
    }

    return (
        <ConfirmCtx.Provider value={{ show }}>
            {children}
            <Modal closeable={false} isOpen={showModal}>
                <ModalHeader>{showProps?.title}</ModalHeader>
                <ModalBody>{showProps?.content}</ModalBody>
                <ModalFooter>
                    <div style={{ display: 'flex' }}>
                        <div style={{ flexGrow: 1 }} />
                        <Button
                            size='compact'
                            kind='secondary'
                            onClick={() => {
                                resolver.current(false)
                                setShowModal(false)
                            }}
                        >
                            {t('Cancel')}
                        </Button>
                        &nbsp;&nbsp;
                        <Button
                            size='compact'
                            onClick={() => {
                                resolver.current(true)
                                setShowModal(false)
                            }}
                        >
                            {t('Continue')}
                        </Button>
                    </div>
                </ModalFooter>
            </Modal>
        </ConfirmCtx.Provider>
    )
}

const useConfirmCtx = () => useContext(ConfirmCtx)

export interface IConfirmButtonProps extends IButtonProps, IShowProps {}

const ConfirmButton = ({ children, onClick, title, content, ...props }: IConfirmButtonProps) => {
    const confirmCtx = useConfirmCtx()
    return (
        <Button
            /* eslint-disable react/jsx-props-no-spreading */
            {...props}
            onClick={async (e) => {
                const ok = await confirmCtx.show({ title, content })
                if (ok && onClick) {
                    onClick(e)
                }
            }}
        >
            {children}
        </Button>
    )
}

export { ConfirmCtxProvider, useConfirmCtx, ConfirmButton }
