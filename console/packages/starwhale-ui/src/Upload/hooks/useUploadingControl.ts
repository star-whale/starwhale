import { useEffect, useRef } from 'react'
import { Subject, Subscription, mergeMap } from 'rxjs'

function useUploadingControl<FileUpload>({
    concurrent = 4,
    onUpload,
    onDone,
}: {
    concurrent?: number
    onUpload?: (args: FileUpload) => Promise<any>
    onDone?: (args: FileUpload & { resp: any }) => void
    onError?: (args: FileUpload & { resp: any }) => void
}) {
    const uploadQRef = useRef<Subject<FileUpload>>(new Subject<FileUpload>())

    useEffect(() => {
        const subscription = new Subscription()
        const uploadQ = uploadQRef.current
        const uploadQSubscription = uploadQ
            .asObservable()
            .pipe(
                mergeMap(async (args: FileUpload) => {
                    const resp = await onUpload?.(args)
                    return {
                        ...args,
                        resp,
                    }
                    // return new Promise((resolve, reject) => {
                    //     setTimeout(() => {
                    //         resolve(fu)
                    //     }, 2000)
                    // })
                }, concurrent)
            )
            .subscribe((res: any) => {
                // process result
                // console.log(res, 'done')
                onDone?.(res)
            })

        subscription.add(uploadQSubscription)

        // new Array(concurrent).fill(100).map(() => uploadQ.next({ test: new Date() }))

        return () => {
            subscription.unsubscribe()
        }
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, [])

    return {
        uploadQueue: uploadQRef.current,
    }
}

export { useUploadingControl }
export default useUploadingControl
