import axios from 'axios'

export type SignedT = {
    pathPrefix: string
    signedUrls: Record<string, string>
}

export async function sign(files: string[], pathPrefix?: string): Promise<SignedT> {
    const resp = await axios.put('/api/v1/filestorage/signedurl', {
        files,
        pathPrefix,
    })
    return resp.data
}

export async function deleteFiles(files: string[], pathPrefix?: string): Promise<any> {
    const resp = await axios.delete('/api/v1/filestorage/file', {
        data: {
            // @ts-ignore
            files,
            pathPrefix,
        },
    })
    return resp.data
}

export async function fetchSignPathPrefix(): Promise<string> {
    const resp = await axios.get('/api/v1/filestorage/path/apply?flag=ds-build')
    return resp.data
}
