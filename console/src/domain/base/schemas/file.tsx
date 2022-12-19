export interface IFileSchema {
    files?: FileNode[]
}

export interface FileNode {
    name: string
    signature?: string
    flag?: 'added' | 'updated' | 'deleted' | 'unchanged'
    mime?: string
    type?: 'directory' | 'file'
    desc?: string
    size?: string
    files?: FileNode[]
}
