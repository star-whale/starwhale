import React from 'react'
import { JobStatusType } from '../schemas/job'
import useTranslation from '@/hooks/useTranslation'
import { themedUseStyletron } from '@starwhale/ui/theme/styletron'

function JobStatus({ status }: { status: Exclude<JobStatusType, 'TO_CANCEL' | 'CANCELING' | 'UNKNOWN'> }) {
    const [t] = useTranslation()
    const [css] = themedUseStyletron()

    const cls = css({
        'borderRadius': '12px',
        'padding': '3px 10px',
        'fontSize': '12px',
        'lineHeight': '12px',
        'width': 'max-content',
        ':before': {
            content: '"\u2022"',
            display: 'inline-block',
            marginRight: '4px',
        },
    })

    const JOB_STATUS = {
        [JobStatusType.CREATED]: (
            <p className={cls} style={{ color: '#2B65D9', backgroundColor: '#EBF1FF' }}>
                {t('job.status.created')}
            </p>
        ),
        [JobStatusType.PAUSED]: (
            <p className={cls} style={{ color: '#6C48B3', backgroundColor: '#F3EDFF' }}>
                {t('job.status.paused')}
            </p>
        ),
        [JobStatusType.RUNNING]: (
            <p className={cls} style={{ color: '#E67F17', backgroundColor: '#FFF3E8' }}>
                {t('job.status.running')}
            </p>
        ),
        [JobStatusType.CANCELED]: (
            <p className={cls} style={{ color: '#4D576A', backgroundColor: '#EBF1FF' }}>
                {t('job.status.cancelled')}
            </p>
        ),
        [JobStatusType.SUCCESS]: (
            <p className={cls} style={{ color: '#00B368', backgroundColor: '#E6FFF4' }}>
                {t('job.status.succeeded')}
            </p>
        ),
        [JobStatusType.FAIL]: (
            <p className={cls} style={{ color: '#CC3D3D', backgroundColor: '#FFEDED' }}>
                {t('job.status.fail')}
            </p>
        ),
        [JobStatusType.READY]: (
            <p className={cls} style={{ color: '#00B368', backgroundColor: '#FFEDED' }}>
                {t('job.status.ready')}
            </p>
        ),
    }

    if (!status) return null

    if (!JOB_STATUS[status])
        return (
            <p className={cls} style={{ color: '#4D576A', backgroundColor: '#EBF1FF' }}>
                {status}
            </p>
        )

    return JOB_STATUS[status]
}

export default JobStatus
