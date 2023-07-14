export type UploadFile = File & {
    path: string
    originFileObj?: any
    status: 'uploading' | 'error' | 'done' | 'error_max' | 'error_exist'
}
