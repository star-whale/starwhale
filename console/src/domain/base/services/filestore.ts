import axios from 'axios'
import qs from 'qs'

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

export async function fetchSignPathPrefix(): Promise<string> {
    const resp = await axios.get('/api/v1/filestorage/path/apply')
    return resp.data
}
