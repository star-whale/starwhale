import React from 'react'
import ReactMarkdown, { Options, uriTransformer } from 'react-markdown'
import './markdown.css'
// import remarkGfm from 'remark-gfm'
// import { Prism as SyntaxHighlighter } from 'react-syntax-highlighter'

export interface IMarkdownProps extends Options {
    children: string
    className?: string
}

export default function Markdown({ children, ...props }: IMarkdownProps) {
    const transformLinkUri = (href: string) => {
        const url = uriTransformer(href)
        if (url.startsWith('http')) {
            return url
        }
        return false as any
    }
    const transformImageUri = (src: string) => {
        const url = uriTransformer(src)
        if (url.startsWith('http')) {
            return url
        }
        //  eslint-disable-next-line
        return 'javascript:void(0)'
    }
    const linkTarget = (href: string) => {
        const url = uriTransformer(href)
        if (url.startsWith('http')) {
            return '_blank'
        }
        return ''
    }

    return (
        <div className='markdown-body'>
            <ReactMarkdown
                transformLinkUri={transformLinkUri}
                transformImageUri={transformImageUri}
                linkTarget={linkTarget}
                {...props}
            >
                {children}
            </ReactMarkdown>
        </div>
    )
}
