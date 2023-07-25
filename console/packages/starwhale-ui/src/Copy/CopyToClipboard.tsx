import React from 'react'
import Copy from 'react-copy-to-clipboard'
import IconFont from '../IconFont'
import Button from '../Button'

export interface ICopyToClipboardProps {
    content: string
    children?: React.ReactNode | React.FC
}

function CopyToClipboard({
    content,
    children,
}: ICopyToClipboardProps): React.FunctionComponentElement<ICopyToClipboardProps> {
    const [copied, setCopied] = React.useState(false)

    return (
        <Copy
            text={content}
            onCopy={() => {
                setCopied(true)
            }}
        >
            <Button as='link' kind='tertiary'>
                {/* @ts-ignore */}
                {typeof children === 'function' ? children(copied) : children}
            </Button>
        </Copy>
    )
}

function IconCopy(props: ICopyToClipboardProps) {
    return (
        <CopyToClipboard {...props}>
            {(copied: boolean) => <IconFont type={copied ? 'check' : 'copy'} style={{ marginLeft: '5px' }} />}
        </CopyToClipboard>
    )
}

export { IconCopy }

export default React.memo<ICopyToClipboardProps>(CopyToClipboard)
