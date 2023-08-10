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

const Component = (editor: NodeViewProps) => {
    const { node, selected } = editor

    const onStateChange = useEventCallback((state: any) => {
        editor.updateAttributes({
            state,
        })
    })

    // console.log('node.attrs.state', node.attrs.state?.widgets)

    return (
        <NodeViewWrapper className={cn('project-summary-panel ', selected && 'shadow-sm border')}>
            {node.type.spec.draggable ? (
                <div draggable='true' data-drag-handle=''>
                    <EvalSelectEditor initialState={node.attrs.state} onStateChange={onStateChange} />
                </div>
            ) : null}
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
    draggable: true,
    atom: true,

    addAttributes() {
        return {
            state: {},
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
                    return commands.insertContent({
                        type: this.name,
                        attrs: options,
                    })
                },
        }
    },

    addNodeView() {
        return ReactNodeViewRenderer(Component)
    },
})
