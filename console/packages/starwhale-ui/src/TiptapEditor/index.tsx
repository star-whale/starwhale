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

export default function TiptapEditor({ editable, onSaveStatusChange }) {
    const [content, setContent] = useLocalStorage('content', DEFAULT_EDITOR_CONTENT)
    const [saveStatus, setSaveStatus] = useState('Saved')
    const [hydrated, setHydrated] = useState(false)
    const [t] = useTranslation()

    const { run: debouncedUpdates } = useDebounceFn(
        async ({ editor }) => {
            const json = editor.getJSON()
            setSaveStatus(t('report.save.saving'))
            onSaveStatusChange?.(SaveStatus.SAVING)
            setContent(json)
            setTimeout(() => {
                setSaveStatus(t('report.save.saved'))
                onSaveStatusChange?.(SaveStatus.SAVED)
            }, 500)
        },
        {
            wait: 750,
        }
    )

    const editor = useEditor({
        extensions: TiptapExtensions,
        editorProps: TiptapEditorProps,
        onUpdate: (e) => {
            console.log(e.editor.getJSON())
            setSaveStatus(t('report.save.unsaved'))
            onSaveStatusChange?.(SaveStatus.UNSAVED)
            debouncedUpdates(e)
        },
        autofocus: 'end',
    })

    // Hydrate the editor with the content from localStorage.
    useEffect(() => {
        if (editor && content && !hydrated) {
            editor.commands.setContent(content)
            // TODO: should open this to limit only once editing content
            setHydrated(true)
        }
    }, [editor, content, hydrated])

    useEffect(() => {
        if (!editor) {
            return
        }

        editor.setEditable(editable)
    }, [editor, editable])

    return (
        <div
            onClick={() => {
                // editor?.chain().focus().run()
            }}
            role='button'
            tabIndex={0}
            className='relative self-center min-h-[500px] w-full h-full bg-white sm:mb-[calc(10px)] sm:rounded-lg'
        >
            {/* <div className='absolute right-5 top-5 mb-5 rounded-lg bg-stone-100 px-2 py-1 text-sm text-stone-400'>
                {saveStatus}
            </div> */}
            {editor && <EditorBubbleMenu editor={editor} />}
            <EditorContent editor={editor} />
        </div>
    )
}
