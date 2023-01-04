import React, { useEffect } from 'react'
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

// import('http://127.0.0.1:8080/index.js')
// @ts-ignore
import('http://localhost:3000/src/main.ts')
import { toaster } from 'baseui/toast'

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

    useEffect(() => {
        // @ts-ignore
        window.wait = async () => {
            if (!formRef.current) return
            console.log('wait', formRef.current.getFieldsValue())

            const values = formRef.current.getFieldsValue()

            if (
                values.modelVersionUrl === undefined ||
                values.runtimeVersionUrl === undefined ||
                values.resourcePool === undefined
            ) {
                toaster.negative('Please fill in all fields', {})
                return
            }

            const resp = await axios.post(`/api/v1/project/${projectId}/serving`, {
                modelVersionUrl: values.modelVersionUrl,
                runtimeVersionUrl: values.runtimeVersionUrl,
                resourcePool: values.resourcePool,
            })

            if (!resp.data?.baseUri) return

            async function checkStatus(baseUri: string) {
                baseUri = '/gateway/model-serving/23/'
                return await axios.get(baseUri)
            }

            const rtn = await new Promise((resolve) => {
                const id = setInterval(() => {
                    console.log('2, interval')
                    checkStatus(resp.data?.baseUri).then(() => {
                        // @ts-ignore
                        window.gradio_config.root = `http://${location.host}${resp.data?.baseUri}`
                        console.log('3', window.gradio_config)
                        clearInterval(id)
                        resolve(null)
                    })
                }, 2000)
            })

            console.log('1', rtn)
        }
        return () => {
            // @ts-ignores
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
                    setConfig(data)
                    // @ts-ignore
                    window.gradio_config = data
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
                <gradio-app>
                    <div id='online-eval' />
                </gradio-app>
            )}
            {/* <iframe src='http://localhost:3000/' title='gradio' height='600px' frameBorder='0' /> */}
        </>
    )
}
