import React from 'react'

import { Button } from '@/components'

import { StatefulTooltip as Tooltip } from 'baseui/tooltip'

export interface ICopyToClipBoardProps {
    contentRef: React.RefObject<any>
    showSuccessDelay?: number
    className?: string
}

function CopyToClipboard({
    contentRef,
    showSuccessDelay = 1500,
    className = '',
}: ICopyToClipBoardProps): React.FunctionComponentElement<ICopyToClipBoardProps> {
    const [showCopiedIcon, setShowCopiedIcon] = React.useState<boolean>(false)

    React.useEffect(() => {
        if (showCopiedIcon) {
            setTimeout(() => {
                setShowCopiedIcon(false)
            }, showSuccessDelay)
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [showCopiedIcon])

    const onCopy = React.useCallback(() => {
        if (contentRef.current && !showCopiedIcon) {
            navigator.clipboard
                .writeText(contentRef.current.innerText.trim(''))
                .then(() => {
                    setShowCopiedIcon(true)
                })
                .catch()
        }
    }, [contentRef, showCopiedIcon])

    return (
        <Tooltip
        // title={showCopiedIcon ? 'Copied!' : 'Copy to clipboard'}
        >
            {/* eslint-disable-next-line jsx-a11y/no-static-element-interactions */}
            <span className={className} onClick={onCopy}>
                <Button>{/* {showCopiedIcon ? <Icon name='check' /> : <Icon name='copy' />} */}</Button>
            </span>
        </Tooltip>
    )
}

CopyToClipboard.displayName = 'CopyToClipBoard'

export default React.memo<ICopyToClipBoardProps>(CopyToClipboard)
