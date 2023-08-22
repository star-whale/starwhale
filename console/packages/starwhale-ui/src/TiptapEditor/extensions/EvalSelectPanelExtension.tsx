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
import React, { useEffect } from 'react'

const Component = (props: NodeViewProps) => {
    const [, setEditing] = React.useState(true)
    const { node, selected } = props

    const onStateChange = useEventCallback((state: any) => {
        // console.log('onStateChange', state)
        props.updateAttributes({
            state,
        })
    })

    useEffect(() => {
        const handle = ({ editor }) => {
            setEditing(editor.isEditable)
        }
        props.editor.on('update', handle)
        return () => {
            props.editor.off('update', handle)
        }
    }, [props.editor])

    const memoe = React.useMemo(() => {
        return (
            <EvalSelectEditor
                editable={props.editor.isEditable}
                initialState={node.attrs.state}
                onStateChange={onStateChange as any}
                onEvalSectionDelete={props.deleteNode}
            />
        )
    }, [props.deleteNode, node.attrs.state, props.editor.isEditable, onStateChange])

    // console.log('node.attrs.state', node.attrs.state?.widgets)

    return (
        <NodeViewWrapper className={cn('project-summary-panel ', selected && 'shadow-sm')}>
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
        return ReactNodeViewRenderer(Component, {
            // https://discuss.prosemirror.net/t/make-only-part-of-a-nodeview-draggable/1145/7
            // stopEvent({ event: e }) {
            //     console.log(e.type, this)
            //     return true
            //     if (/dragstart|dragover|drangend|drop/.test(e.type)) return false
            //     return /mousedown|drag|drop/.test(e.type)
            // },
        })
    },
})
