import {
    mergeAttributes,
    NodeViewWrapper,
    NodeViewContent,
    NodeViewProps,
    Node,
    ReactNodeViewRenderer,
} from '@tiptap/react'
import { cn } from '../lib/utils'
import EvalSelectEditor from '@/components/Editor/EvalSelectEditor'
import { useEventCallback } from '@starwhale/core'
import React from 'react'

const Component = (props: NodeViewProps) => {
    const { node, selected } = props

    const onStateChange = useEventCallback((state: any) => {
        // console.log('onStateChange', state)
        props.updateAttributes({
            state,
        })
    })

    const memoe = React.useMemo(() => {
        return <EvalSelectEditor initialState={node.attrs.state} onStateChange={onStateChange as any} />
    }, [node.attrs.state, onStateChange])

    // console.log('node.attrs.state', node.attrs.state?.widgets)

    return (
        <NodeViewWrapper className={cn('project-summary-panel ', selected && 'shadow-sm border')}>
            {node.type.spec.draggable ? (
                <div draggable='true' data-drag-handle=''>
                    {memoe}
                </div>
            ) : (
                memoe
            )}
            <NodeViewContent />
        </NodeViewWrapper>
    )
}

declare module '@tiptap/core' {
    interface Commands<ReturnType> {
        projectSummaryPanel: {
            /**
             * Insert a panel
             */
            setPanel: (options?: any) => ReturnType
        }
    }
}

export default Node.create({
    name: 'eval-select-panel',
    group: 'block',
    draggable: false,
    atom: true,

    addOptions() {
        return {}
    },

    addStorage() {
        return {}
    },

    addAttributes() {
        return {
            state: {
                default: null,
                // renderHTML: (attributes) => {
                //     return {
                //         'data-state': JSON.stringify(attributes.state),
                //     }
                // },
                // parseHTML: (element) => element.getAttribute('data-state'),
            },
        }
    },

    parseHTML() {
        return [
            {
                tag: `div[data-type="${this.name}"]`,
            },
        ]
    },

    renderHTML({ HTMLAttributes }) {
        return ['div', mergeAttributes(HTMLAttributes, { 'data-type': this.name })]
    },

    addCommands() {
        return {
            setPanel:
                (options) =>
                ({ commands }) => {
                    return commands.insertContent([
                        {
                            type: this.name,
                            attrs: options,
                        },
                        {
                            type: 'paragraph',
                        },
                    ])
                },
        }
    },

    addNodeView() {
        return ReactNodeViewRenderer(Component)
    },
})
