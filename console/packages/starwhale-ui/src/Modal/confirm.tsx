import React, { useState, useContext, useRef } from 'react'
import { Modal, ModalHeader, ModalBody, ModalFooter } from 'baseui/modal'
import { Button, IButtonProps } from '../Button'
import IconFont from '../IconFont'
import { expandMargin, expandPadding } from '../utils'

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
                <ModalHeader
                    $style={{
                        ...expandMargin('30px', '30px', '10px', '30px'),
                        fontSize: '16px',
                    }}
                >
                    <IconFont type='info' style={{ color: ' #E67F17', marginRight: '8px' }} size={16} />
                    {showProps?.title}
                </ModalHeader>
                <ModalBody
                    $style={{
                        ...expandMargin('10px', '30px', '10px', '30px'),
                        fontSize: '16px',
                    }}
                >
                    {showProps?.content}
                </ModalBody>
                <ModalFooter
                    $style={{
                        ...expandMargin('30px', '30px', '30px', '30px'),
                        ...expandPadding('0', '0', '0', '0'),
                        fontSize: '16px',
                    }}
                >
                    <div style={{ display: 'grid', gap: '20px', gridTemplateColumns: '1fr 79px 79px' }}>
                        <div style={{ flexGrow: 1 }} />
                        <Button
                            size='default'
                            isFull
                            kind='secondary'
                            onClick={() => {
                                resolver.current(false)
                                setShowModal(false)
                            }}
                        >
                            No
                        </Button>
                        <Button
                            size='default'
                            isFull
                            onClick={() => {
                                resolver.current(true)
                                setShowModal(false)
                            }}
                        >
                            Yes
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
