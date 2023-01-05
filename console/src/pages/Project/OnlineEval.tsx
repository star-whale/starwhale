import React, { useEffect, useLayoutEffect } from 'react'
import CreateOnlineEvalForm from '@model/components/CreateOnlineEvalForm'
import useTranslation from '@/hooks/useTranslation'
import Card from '@/components/Card'
import { useProject } from '@/domain/project/hooks/useProject'
import { getToken } from '@/api'
import { useParams } from 'react-router'
import { fetchModel } from '@/domain/model/services/model'
import { useQuery } from 'react-query'
import { useFetchModelVersion } from '@/domain/model/hooks/useFetchModelVersion'
import axios from 'axios'
import { Modal } from 'baseui/modal'
import { toaster } from 'baseui/toast'
import { Spinner } from 'baseui/spinner'
import css from '@/assets/GradioWidget/es/style.css'
import '@/assets/GradioWidget/es/app.es.js'

declare global {
    interface Window {
        wait: Function | null
        gradio_config: any
    }
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
        if (window.wait) return
        window.wait = async () => {
            if (!formRef.current) return
            const values = formRef.current.getFieldsValue()

            if (
                values.modelVersionUrl === undefined ||
                values.runtimeVersionUrl === undefined ||
                values.resourcePool === undefined
            ) {
                toaster.negative('Please fill in all fields', {})
                return Promise.reject()
            }

            try {
                setIsLoading(true)

                const resp = await axios.post(`/api/v1/project/${projectId}/serving`, {
                    modelVersionUrl: values.modelVersionUrl,
                    runtimeVersionUrl: values.runtimeVersionUrl,
                    resourcePool: values.resourcePool,
                })

                if (!resp.data?.baseUri) return

                window.gradio_config.root = `http://${location.host}${resp.data?.baseUri}/run/`

                async function checkStatus(baseUri: string) {
                    return await axios.get(baseUri)
                }

                await new Promise((resolve) => {
                    const id = setInterval(() => {
                        checkStatus(resp.data?.baseUri).then(() => {
                            setIsLoading(false)
                            clearInterval(id)
                            resolve(null)
                        })
                    }, 2000)
                })
            } catch (e) {
                console.log(e)
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

            fetch(`/api/v1/project/${project?.name}/model/${modelName}/version/${versionName}/file`, {
                headers: {
                    'Authorization': getToken(),
                    'X-SW-DOWNLOAD-OBJECT-PATH': 'src/svc.json',
                    'X-SW-DOWNLOAD-OBJECT-NAME': 'svc.json',
                    'X-SW-DOWNLOAD-OBJECT-HASH': '',
                },
            })
                .then((res) => res.json())
                .then((data) => {
                    window.gradio_config = data
                    window.gradio_config.css = css
                    setConfig(data)
                })
        }
    }, [project?.name, projectId, modelId, modelVersionId, modelInfo.isSuccess, modelVersionInfo.isSuccess])

    return (
        <>
            <Card title={t('online eval')}>
                <CreateOnlineEvalForm
                    ref={formRef}
                    onSubmit={(data: any) => {
                        console.log(data)
                    }}
                />
            </Card>
            {config && (
                // @ts-ignore
                <gradio-app>
                    <div id='online-eval' />
                    {/* @ts-ignore */}
                </gradio-app>
            )}
            <Modal
                isOpen={isLoading}
                closeable={false}
                role={'dialog'}
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
