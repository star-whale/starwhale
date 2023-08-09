import React, { useEffect } from 'react'
import CreateOnlineEvalForm from '@model/components/CreateOnlineEvalForm'
import useTranslation from '@/hooks/useTranslation'
import Card from '@/components/Card'
import { useProject } from '@/domain/project/hooks/useProject'
import { getToken } from '@/api'
import { useParams } from 'react-router-dom'
import { fetchModel } from '@/domain/model/services/model'
import { useQuery } from 'react-query'
import { useFetchModelVersion } from '@/domain/model/hooks/useFetchModelVersion'
import axios from 'axios'
import { Modal } from 'baseui/modal'
import { toaster } from 'baseui/toast'
import yaml from 'js-yaml'
import css from '@/assets/GradioWidget/es/style.css?inline'
// eslint-disable-next-line import/extensions
import '@/assets/GradioWidget/es/app.es.js'
import qs from 'qs'
import { IComponent, IDependency, IGradioConfig } from '@/domain/project/schemas/gradio'
import { getOnlineEvalStatus } from '@project/services/OnlineEval'
import { IOnlineEvalStatusSchema } from '@project/schemas/OnlineEval'
import OnlineEvalLoading from '@project/components/OnlineEvalLoading'

declare global {
    interface Window {
        // eslint-disable-next-line @typescript-eslint/ban-types
        wait: Function | null
        // function for fetching the example resources used by gradio dataset component
        fetchExample: ((url: string) => Promise<string>) | null
        gradio_config: any
    }
}

interface ISystemResource {
    type: string
    request: number
    limit: number
}

interface ISpec {
    resources: Array<ISystemResource>
}

// production mode
// @ts-ignore
// import('http://127.0.0.1:8080/app.es.js')

// debug mode
// @ts-ignore
// import('http://localhost:3000/src/main.ts')

export default function OnlineEval() {
    const { project } = useProject()
    const [t] = useTranslation()
    const [config, setConfig] = React.useState<any>(null)
    const [gradioId, setGradioId] = React.useState(1)
    const { projectId, modelId, modelVersionId } = useParams<{
        projectId: string
        modelId: string
        modelVersionId: string
    }>()
    const modelInfo = useQuery(`fetchModel:${projectId}:${modelId}`, () => fetchModel(projectId, modelId))
    const modelVersionInfo = useFetchModelVersion(projectId, modelId, modelVersionId)
    const formRef = React.useRef<any>()
    const [isLoading, setIsLoading] = React.useState(false)
    const [status, setStatus] = React.useState<IOnlineEvalStatusSchema>()

    useEffect(() => {
        if (window.wait) return undefined
        window.wait = async (): Promise<void> => {
            if (!formRef.current) return
            const values = formRef.current.getFieldsValue()

            if (values.modelVersionUrl === undefined || values.runtimeVersionUrl === undefined) {
                toaster.negative('Please fill in all fields', {})
                await Promise.reject(new Error('no runtime version'))
                return
            }

            try {
                setIsLoading(true)

                // spec of `spec` see https://github.com/star-whale/starwhale/pull/1709
                const spec: ISpec = { resources: [] }
                // eslint-disable-next-line guard-for-in,no-restricted-syntax
                for (const k in values.resourceAmount) {
                    spec.resources.push({
                        type: k,
                        request: Number(values.resourceAmount[k]),
                        limit: Number(values.resourceAmount[k]),
                    })
                }

                const resp = await axios.post(`/api/v1/project/${projectId}/serving`, {
                    modelVersionUrl: values.modelVersionUrl,
                    runtimeVersionUrl: values.runtimeVersionUrl,
                    resourcePool: values.resourcePool ?? null,
                    spec: spec.resources.length > 0 ? yaml.dump(spec) : '',
                })

                if (!resp.data?.baseUri) return

                // eslint-disable-next-line no-restricted-globals
                window.gradio_config.root = `${location.protocol}//${location.host}${resp.data?.baseUri}/run/`

                await new Promise((resolve) => {
                    const check = () => {
                        getOnlineEvalStatus(projectId, resp.data?.id).then(setStatus)
                        axios
                            .get(resp.data?.baseUri)
                            .then(() => {
                                setIsLoading(false)
                                resolve(null)
                            })
                            .catch(() => {
                                setTimeout(check, 1000)
                            })
                    }
                    check()
                })
            } catch (e) {
                setIsLoading(false)
            }
        }
        return () => {
            window.wait = null
        }
    }, [formRef, projectId])

    useEffect(() => {
        if (window.fetchExample) return undefined
        window.fetchExample = async (url: string): Promise<string> => {
            const { data } = await axios.get(url, { responseType: 'arraybuffer' })
            const base64 = btoa(new Uint8Array(data).reduce((i, byte) => i + String.fromCharCode(byte), ''))
            return `data:;base64,${base64}`
        }

        return () => {
            window.fetchExample = null
        }
    }, [formRef, projectId])

    useEffect(() => {
        if (modelInfo.isSuccess || modelVersionInfo.isSuccess) {
            const versionName = modelVersionId ? modelVersionInfo?.data?.versionInfo.name : modelInfo?.data?.versionName
            const modelName = modelInfo?.data?.name
            if (!modelName || !versionName) {
                return
            }

            const api = `/api/v1/project/${project?.name}/model/${modelName}/getFileData`
            fetch(
                `${api}?${qs.stringify({
                    Authorization: getToken(),
                    path: 'svc.json',
                    version: versionName,
                    silent: true,
                })}`
            )
                .then((res) => res.json())
                .then((conf) => {
                    // patch examples params
                    const gradioConfig = conf as IGradioConfig
                    const datasets: IComponent[] = []
                    gradioConfig.components.forEach((cp) => {
                        const { props } = cp

                        if (!props?.components) {
                            return
                        }
                        // if the component is a dataset and the related input type is image, video or file
                        // add the request params (file path and auth token) for the src
                        // details of the api: https://github.com/star-whale/starwhale/blob/28a4741e6d6c4aa487b298567772458b1291c049/server/controller/src/main/java/ai/starwhale/mlops/api/ModelApi.java#L307-L325
                        if (cp.type === 'dataset') {
                            datasets.push(cp)
                            let append = false
                            for (let i = 0; i < props?.components?.length ?? 0; i++) {
                                const tp = cp.props?.components?.[i]
                                if (tp && ['image', 'video', 'file'].includes(tp)) {
                                    append = true
                                    break
                                }
                            }
                            if (append && props && props.samples) {
                                props.samples = props.samples?.map((parts: string[]) =>
                                    parts.map(
                                        (item) =>
                                            `${api}?${qs.stringify({
                                                Authorization: getToken(),
                                                version: versionName,
                                                path: item,
                                            })}`
                                    )
                                )
                            }
                        }
                    })
                    // gradio always generate the click dependencies with the `backend_fn` to true and `js` to null.
                    // see: https://github.com/star-whale/gradio/blob/474b88af52367c76b4dc207e0628871234f0c8ef/gradio/examples.py#L242-L256
                    // this makes a http request to the model serving backend for fetching the example resource and
                    // call the backend directly without calling the `wait` function.
                    // so we need hijack the generated configs for gradio
                    datasets.forEach((ds) => {
                        gradioConfig.dependencies.forEach((dep: IDependency, idx: number) => {
                            if (!dep.targets.includes(ds.id)) {
                                return
                            }
                            // do not request the builtin backend, the model service is not ready
                            gradioConfig.dependencies[idx].backend_fn = false
                            // call our fetch fn to get the resources
                            gradioConfig.dependencies[idx].js = `async function (...x) {
                                return window.fetchExample('${ds.props?.samples?.[0]?.[0]}')
                            }`
                        })
                    })

                    return conf
                })
                .then((data) => {
                    window.gradio_config = data
                    window.gradio_config.css = css
                    setConfig(data)
                    // update gradio id to trigger gradio reloading
                    setGradioId((i) => i + 1)
                })
        }
    }, [
        project?.name,
        projectId,
        modelId,
        modelVersionId,
        modelInfo.isSuccess,
        modelVersionInfo.isSuccess,
        modelInfo?.data?.name,
        modelInfo?.data?.versionName,
        modelVersionInfo?.data?.versionInfo.name,
    ])

    return (
        <>
            <Card title={t('online eval')}>
                <CreateOnlineEvalForm ref={formRef} onSubmit={() => {}} />
            </Card>
            {config && (
                // @ts-ignore
                <gradio-app key={gradioId.toString()}>
                    <div id='online-eval' />
                    {/* @ts-ignore */}
                </gradio-app>
            )}
            <Modal
                isOpen={isLoading}
                closeable={false}
                role='dialog'
                overrides={{
                    Dialog: {
                        style: {
                            width: 'auto',
                            padding: '30px',
                        },
                    },
                }}
            >
                <OnlineEvalLoading progress={status?.progress} events={status?.events} />
            </Modal>
        </>
    )
}
