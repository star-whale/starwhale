export type UploadFile = File & {
    path: string
    originFileObj?: any
    status: StatusT
    percent?: number
}

type StatusT = 'uploading' | 'error' | 'done' | 'errorMax' | 'errorExist' | 'errorUnknown'
