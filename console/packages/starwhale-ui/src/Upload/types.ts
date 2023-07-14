export type UploadFile = File & {
    path: string
    originFileObj?: any
    status: StatusT
}

type StatusT = 'uploading' | 'error' | 'done' | 'error_max' | 'erroexist'
