import { IListQuerySchema } from '@/domain/base/schemas/list'
import { useQuery } from 'react-query'
import qs from 'qs'
import { listBaseImages, listDevices } from '../services/runtime'

export function useFetchDevices(query: IListQuerySchema) {
    const info = useQuery(`listDevices:${qs.stringify(query)}`, () => listDevices(query))
    return info
}

export function useFetchBaseImages(query: IListQuerySchema) {
    const info = useQuery(`listBaseImages:${qs.stringify(query)}`, () => listBaseImages(query))
    return info
}
