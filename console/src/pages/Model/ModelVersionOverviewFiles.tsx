import React, { useEffect, useMemo, useCallback } from 'react'
import { createUseStyles } from 'react-jss'
import GridResizer from '@/components/AutoResizer/GridResizer'
import Input from '@starwhale/ui/Input'
import IconFont from '@starwhale/ui/IconFont'
import { useModelVersion } from '@/domain/model/hooks/useModelVersion'
import { TreeView, toggleIsExpanded, TreeLabelInteractable } from 'baseui/tree-view'
import { FileNode } from '@/domain/base/schemas/file'
import { getToken } from '@/api'
import { useProject } from '@/domain/project/hooks/useProject'
import { useModel } from '@/domain/model/hooks/useModel'
import Editor, { EditorProps } from '@monaco-editor/react'
import BusyPlaceholder from '@/components/BusyLoaderWrapper/BusyPlaceholder'
import AutoResizer from '@/components/AutoResizer/AutoResizer'
import Select from '@starwhale/ui/Select'
// @ts-ignore
import * as monaco from 'monaco-editor/esm/vs/editor/editor.api'
import { useLocalStorage } from 'react-use'

const useStyles = createUseStyles({
    wrapper: {
        'flex': 1,
        'position': 'relative',
        'display': 'flex',
        'flexDirection': 'column',
        '& [role="treeitem"] > div': {
            color: '#02102B',
        },
        '& [role="treeitem"] > div:has(.item--selected)': {
            backgroundColor: '#2B65D9',
            color: '#fff !important',
        },
    },
    treeWrapper: {
        padding: '20px 20px 20px 0',
        flex: 1,
    },
    flex: {
        display: 'flex',
        alignItems: 'center',
    },
})

type FileNodeWithPathT = FileNode & {
    path: string[]
}

const isText = (file?: FileNodeWithPathT) => (file ? file.desc === 'SRC' || file.mime === 'text/plain' : false)

export default function ModelVersionFiles() {
    const styles = useStyles()
    const { modelVerson } = useModelVersion()
    const { project } = useProject()
    const { model } = useModel()
    const [search, setSearch] = React.useState('')
    const [content, setContent] = React.useState('')
    const [sourceFile, setSourceFile] = React.useState<FileNodeWithPathT | undefined>()

    const fileMap = useMemo(() => {
        const map = new Map<string, FileNodeWithPathT>()
        const walk = (files: FileNode[] = [], directory: string[] = []) => {
            files.forEach((file) => {
                map.set([...directory, file.name].join('/'), {
                    ...file,
                    path: [...directory, file.name],
                })
                walk(file.files, [...directory, file.name])
            })
        }
        walk(modelVerson?.files)
        return map
    }, [modelVerson])

    const fileTree: TreeNodeT[] = useMemo(() => {
        const walkWithSearch = (files: FileNode[] = [], directory: string[] = [], searchtmp = ''): TreeNodeT[] => {
            return files
                .map((file) => {
                    if (file.type === 'file' && !file.name.includes(searchtmp)) return null as any
                    const id = [...directory, file.name].join('/')
                    const isSelected = sourceFile?.path.join('/') === id
                    const fileType = file.type === 'directory' ? 'file' : 'file2'

                    return {
                        id,
                        label: (
                            <TreeLabelInteractable>
                                <div
                                    role='button'
                                    tabIndex={-1}
                                    className={isSelected ? 'item--selected' : ''}
                                    style={{
                                        display: 'flex',
                                        justifyContent: 'space-between',
                                        alignItems: 'center',
                                    }}
                                    onClick={() => setSourceFile(fileMap.get(id))}
                                >
                                    <p>
                                        <IconFont
                                            type={fileType}
                                            style={{ color: 'rgba(2,16,43,0.20)', marginRight: '5px' }}
                                            size={14}
                                        />{' '}
                                        {file.name}
                                    </p>
                                    {isSelected && <IconFont type='check' style={{ color: '#00B0FF' }} />}
                                </div>
                            </TreeLabelInteractable>
                        ),
                        isExpanded: true,
                        children: walkWithSearch(file.files, [...directory, file.name], searchtmp),
                    }
                })
                .filter((file) => !!file)
        }
        return walkWithSearch(modelVerson?.files, [], search)
    }, [modelVerson, search, fileMap, sourceFile, setSourceFile])

    // testing
    // useEffect(() => {
    //     setSourceFile(fileMap.get('src/model.yaml'))
    // }, [fileMap])

    useEffect(() => {
        if (sourceFile) {
            fetch(`/api/v1/project/${project?.name}/model/${model?.name}/version/${modelVerson?.versionName}/file`, {
                // @ts-ignore
                headers: {
                    'Authorization': getToken(),
                    'X-SW-DOWNLOAD-OBJECT-PATH': sourceFile.path.join('/'),
                    'X-SW-DOWNLOAD-OBJECT-NAME': sourceFile.name,
                    'X-SW-DOWNLOAD-OBJECT-HASH': sourceFile.signature,
                },
            }).then(async (res) => {
                if (isText(sourceFile)) {
                    const text = await res.text()
                    setContent(text)
                }
            })
        }
    }, [sourceFile, project?.name, model?.name, modelVerson?.versionName])

    return (
        <div className={styles.wrapper}>
            <GridResizer
                left={() => {
                    // eslint-disable-next-line @typescript-eslint/no-use-before-define
                    return <FileTree data={fileTree} search={search} onSearch={setSearch} />
                }}
                right={() => {
                    if (isText(sourceFile)) {
                        // eslint-disable-next-line @typescript-eslint/no-use-before-define
                        return <CodeViewer value={content} file={sourceFile} />
                    }

                    // eslint-disable-next-line @typescript-eslint/no-use-before-define
                    return <UnablePreviewer file={sourceFile} />
                }}
                gridLayout={[
                    // RIGHT:
                    '0px 40px 1fr',
                    // MIDDLE:
                    '320px 40px 1fr',
                    // LEFT:
                    '1fr 40px 0px',
                ]}
            />
        </div>
    )
}

type TreeNodeT = {
    id: string
    label: string
    isExpanded: boolean
    children?: TreeNodeT[]
}
type FileTreePropsT = {
    data: TreeNodeT[]
    search?: string
    onSearch?: (search: string) => void
}
function FileTree({ data: rawData = [], search = '', onSearch = () => {} }: FileTreePropsT) {
    const styles = useStyles()
    const [data, setData] = React.useState(rawData)

    useEffect(() => {
        setData(rawData)
    }, [rawData])

    return (
        <div className={styles.treeWrapper}>
            <Input
                value={search}
                onChange={(e) => {
                    onSearch?.(e.target.value)
                }}
                startEnhancer={() => <IconFont type='search' style={{ color: 'rgba(2,16,43,0.40)' }} />}
                overrides={{
                    Root: {
                        style: {
                            marginBottom: '20px',
                            maxWidth: '320px',
                        },
                    },
                }}
            />
            <TreeView
                data={data}
                onToggle={(node) => {
                    // @ts-ignore
                    setData((prevData) => toggleIsExpanded(prevData, node))
                }}
            />
        </div>
    )
}

const THEMES = [
    {
        id: 'light',
        label: 'Light',
    },
    {
        id: 'vs-dark',
        label: 'Dark',
    },
]

function CodeViewer({ file, value }: EditorProps & { file?: FileNodeWithPathT | null }) {
    const styles = useStyles()
    const [theme, setTheme] = useLocalStorage<string>(THEMES[0].id)
    const [language, setLanguage] = React.useState('yaml')
    const [languages, setLanguages] = React.useState<monaco.languages.ILanguageExtensionPoint[]>([])
    const editorRef = React.useRef<monaco.editor.IStandaloneCodeEditor | null>(null)
    const monacoRef = React.useRef<typeof monaco | null>(null)

    const onMount = useCallback(
        (e: monaco.editor.IStandaloneCodeEditor, m: typeof monaco) => {
            editorRef.current = e
            monacoRef.current = m
            if (m) {
                const lgs = m.languages.getLanguages()
                setLanguages(lgs)
            }
        },
        [setLanguages]
    )

    return (
        <div>
            <div
                style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    padding: '20px 0',
                    alignItems: 'center',
                }}
            >
                <div>{file?.name ?? ''}</div>
                <div className={styles.flex} style={{ gap: '20px' }}>
                    <div className={styles.flex} style={{ gap: '12px' }}>
                        Language{' '}
                        <Select
                            overrides={{
                                ControlContainer: {
                                    style: {
                                        width: '200px',
                                    },
                                },
                            }}
                            clearable={false}
                            options={languages.map((lg) => ({ id: lg.id, label: lg.id }))}
                            onChange={(params) => {
                                if (!params.option) return
                                setLanguage?.(params.option.id as string)
                            }}
                            value={language ? [{ id: language }] : []}
                        />
                    </div>
                    <div className={styles.flex} style={{ gap: '12px' }}>
                        Theme
                        <Select
                            overrides={{
                                ControlContainer: {
                                    style: {
                                        width: '200px',
                                    },
                                },
                            }}
                            clearable={false}
                            options={THEMES}
                            onChange={(params) => {
                                if (!params.option) return
                                setTheme?.(params.option.id as string)
                            }}
                            value={theme ? [{ id: theme }] : []}
                        />
                    </div>
                </div>
            </div>
            <AutoResizer>
                {/* eslint-disable-next-line react/no-unused-prop-types */}
                {({ width, height }: { width: number; height: number }) => {
                    return (
                        <div
                            style={{
                                display: value ? 'block' : 'none',
                                border: '1px solid #CFD7E6',
                                borderRadius: '4px',
                                padding: '2px',
                                width: 'fit-content',
                            }}
                        >
                            <Editor
                                options={{
                                    readOnly: true,
                                }}
                                theme={theme as string}
                                height={height - 58}
                                width={width - 6}
                                defaultLanguage='yaml'
                                language={language}
                                value={value}
                                onMount={onMount}
                            />
                        </div>
                    )
                }}
            </AutoResizer>
        </div>
    )
}

function UnablePreviewer({ file }: { file?: FileNodeWithPathT | null }) {
    return (
        <div>
            <div
                style={{
                    display: 'flex',
                    justifyContent: 'space-between',
                    padding: '20px 0',
                    alignItems: 'center',
                }}
            >
                <div>{file?.name ?? ''}</div>
            </div>
            <AutoResizer>
                {/* eslint-disable-next-line react/no-unused-prop-types */}
                {({ width, height }: { width: number; height: number }) => {
                    return (
                        <div
                            style={{
                                display: 'block',
                                border: '1px solid #CFD7E6',
                                borderRadius: '4px',
                                padding: '2px',
                                width,
                                height,
                            }}
                        >
                            <BusyPlaceholder type='center'>
                                <IconFont type='invalidFile' size={64} />
                                <p style={{ color: 'rgba(2,16,43,0.40)' }}>Unable to show preview</p>
                            </BusyPlaceholder>
                        </div>
                    )
                }}
            </AutoResizer>
        </div>
    )
}
