import { Popover } from 'baseui/popover'
import React, { useEffect, useState } from 'react'

function SelectorPopover({
    isOpen: $isOpen,
    children,
    content,
}: {
    isOpen: boolean
    children?: React.ReactNode
    content: React.ReactNode | (() => React.ReactNode)
}) {
    const [isOpen, setIsOpen] = useState(false)
    const ref = React.useRef<HTMLElement>(null)

    useEffect(() => {
        setIsOpen($isOpen)
    }, [$isOpen])

    const handleClose = () => ref.current && setIsOpen(false)

    return (
        <Popover
            ignoreBoundary
            placement='right'
            showArrow
            // autoFocus
            isOpen={isOpen}
            onEsc={handleClose}
            popperOptions={{
                modifiers: {
                    preventOverflow: { enabled: false },
                },
            }}
            overrides={{
                Body: {
                    style: {
                        marginTop: '300px',
                        padding: '8px 12px',
                        backgroundColor: '#fff',
                        top: '-10px',
                    },
                },
                Arrow: {
                    style: {
                        backgroundColor: '#fff',
                        marginTop: '20px',
                    },
                },
                Inner: {
                    style: {
                        backgroundColor: '#FFF',
                        minHeight: '200px',
                        width: '410px',
                        height: '500px',
                        overflow: 'hidden',
                        display: 'flex',
                        flexDirection: 'column',
                    },
                },
            }}
            content={() => (
                <div className='popover flex flex-column overflow-hidden flex-1'>
                    {typeof content === 'function' ? content() : content}
                </div>
            )}
        >
            <p className='popover-handler absolute right-0 top-1/2'>{children}</p>
        </Popover>
    )
}
export default SelectorPopover
