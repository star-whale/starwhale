import { useEffect, useState } from 'react'
import { useEditor, EditorContent } from '@tiptap/react'
import { TiptapEditorProps } from './props'
import { TiptapExtensions } from './extensions'
import useLocalStorage from './hooks/use-local-storage'
import DEFAULT_EDITOR_CONTENT from './default-content'
import { EditorBubbleMenu } from './components'
import { useDebounceFn } from 'ahooks'
import './styles/globals.css'
import './styles/prosemirror.css'
import useTranslation from '@/hooks/useTranslation'

export enum SaveStatus {
    SAVED = 'Saved',
    SAVING = 'Saving',
    UNSAVED = 'Unsaved',
}

export default function TiptapEditor({ id = '', initialContent, editable, onSaveStatusChange, onContentChange }: any) {
    const [content, setContent] = useLocalStorage('content', DEFAULT_EDITOR_CONTENT)
    const [contentId, setContentId] = useLocalStorage('content-id', '')
    const [, setSaveStatus] = useState('Saved')
    const [hydrated, setHydrated] = useState(false)
    const [serverHydrated, setServerHydrated] = useState(false)
    const [t] = useTranslation()

    const { run: debouncedUpdates } = useDebounceFn(
        async ({ editor }) => {
            if (!editable) return
            const json = editor.getJSON()
            setSaveStatus(t('report.save.saving'))
            onSaveStatusChange?.(SaveStatus.SAVING)
            setContent(json)
            setContentId(id)
            onContentChange?.(json)
            setTimeout(() => {
                setSaveStatus(t('report.save.saved'))
                onSaveStatusChange?.(SaveStatus.SAVED)
            }, 100)
        },
        {
            wait: 750,
        }
    )

    const editor = useEditor({
        extensions: TiptapExtensions,
        editorProps: TiptapEditorProps,
        onUpdate: (e) => {
            if (!editable) return
            setSaveStatus(t('report.save.unsaved'))
            onSaveStatusChange?.(SaveStatus.UNSAVED)
            debouncedUpdates(e)
        },
        autofocus: 'end',
    })

    // Hydrate the editor with the content from localStorage.
    useEffect(() => {
        // if diff report no load from local
        if (!editor || !content || hydrated || serverHydrated) return
        if (id !== contentId) return
        editor.commands.setContent(content)
        // TODO: should open this to limit only once editing content
        setHydrated(true)
        setContentId(id)
    }, [editor, content, hydrated, serverHydrated, id, contentId, setContentId])

    // Hydrate the editor with the content from SERVER.
    useEffect(() => {
        if (!editor || serverHydrated || !initialContent) return
        try {
            const tmp = typeof initialContent === 'string' ? JSON.parse(initialContent) : initialContent
            editor.commands.setContent(tmp)
            // should open this to limit only once editing content
            setServerHydrated(true)
            setContentId(id)
        } catch (e) {
            // eslint-disable-next-line no-console
            console.log('wrong report content:', { e })
        }
    }, [editor, initialContent, id, setContentId, content, serverHydrated, contentId, hydrated])

    useEffect(() => {
        if (!editor) return
        editor.setEditable(editable)
    }, [editor, editable])

    return (
        <div
            // notice 1: role=button will cause selector component interactive disabled
            // notice 2: onclick focus will cause page jitter
            // onClick={() => {
            //     editor?.chain().focus().run()
            // }}
            // role='button'
            // tabIndex={0}
            className='relative self-center min-h-[500px] w-full h-full bg-white sm:mb-[calc(10px)] sm:rounded-lg'
        >
            {editor && <EditorBubbleMenu editor={editor} />}
            <EditorContent editor={editor} />
        </div>
    )
}
