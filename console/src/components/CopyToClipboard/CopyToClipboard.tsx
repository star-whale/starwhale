import React from 'react'
import { ExtendButton } from '@starwhale/ui/Button'
import useTranslation from '@/hooks/useTranslation'
import Copy from 'react-copy-to-clipboard'
import { toaster } from 'baseui/toast'

export interface ICopyToClipboardProps {
    content: string
    showSuccessDelay?: number
    successContent?: string
}

function CopyToClipboard({
    content,
    showSuccessDelay = 1500,
    successContent,
}: ICopyToClipboardProps): React.FunctionComponentElement<ICopyToClipboardProps> {
    const [t] = useTranslation()

    return (
        <Copy
            text={content}
            onCopy={() => {
                toaster.positive(successContent ?? t('Copied'), { autoHideDuration: showSuccessDelay })
            }}
        >
            <ExtendButton tooltip={t('Copy Link')} icon='a-copylink' as='link' />
        </Copy>
    )
}

CopyToClipboard.displayName = 'CopyToClipboard'

export default React.memo<ICopyToClipboardProps>(CopyToClipboard)
