import * as monaco from 'monaco-editor'
import Editor, { loader, EditorProps, DiffEditor as BaseDiffEditor, DiffEditorProps } from '@monaco-editor/react'

loader.config({ monaco })

function MonacoEditor(props: EditorProps) {
    return <Editor theme='vs-dark' {...props} />
}

function MonacoDiffEditor(props: DiffEditorProps) {
    return <BaseDiffEditor theme='vs-dark' {...props} />
}
export type { EditorProps, DiffEditorProps }
export { MonacoEditor, MonacoDiffEditor }
export default MonacoEditor
