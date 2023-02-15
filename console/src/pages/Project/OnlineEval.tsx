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
// eslint-disable-next-line baseui/deprecated-component-api
import { Spinner } from 'baseui/spinner'
import yaml from 'js-yaml'
import css from '@/assets/GradioWidget/es/style.css'
// eslint-disable-next-line import/extensions
import '@/assets/GradioWidget/es/app.es.js'
import qs from 'qs'

declare global {
    interface Window {
        // eslint-disable-next-line @typescript-eslint/ban-types
        wait: Function | null
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
                window.gradio_config.root = `http://${location.host}${resp.data?.baseUri}/run/`

                await new Promise((resolve) => {
                    const check = () => {
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
        if (modelInfo.isSuccess || modelVersionInfo.isSuccess) {
            const versionName = modelVersionId ? modelVersionInfo?.data?.versionName : modelInfo?.data?.versionName
            const modelName = modelInfo?.data?.name
            if (!modelName || !versionName) {
                return
            }

            fetch(
                `/api/v1/project/${project?.name}/model/${modelName}/version/${versionName}/file?${qs.stringify({
                    Authorization: getToken(),
                    partName: 'svc.json',
                    signature: '',
                })}`
            )
                .then((res) => res.json())
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
        modelVersionInfo?.data?.versionName,
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
                        },
                    },
                }}
            >
                <Spinner />
            </Modal>
        </>
    )
}
