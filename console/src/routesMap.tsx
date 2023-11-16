import React from 'react'
import AdminLayout from '@/pages/Admin/AdminLayout'
import UserManagement from '@/pages/Admin/UserManagement'
import { Redirect } from 'react-router-dom'
import ProjectLayout from '@/pages/Project/ProjectLayout'
import Login from '@/pages/Home/Login'
import ProjectMembers from '@/pages/Project/ProjectMembers'
import ProjectListCard from '@/pages/Project/ProjectListCard'
import ProjectModels from '@/pages/Project/Models'
import ProjectDatasets from '@/pages/Project/Datasets'
import ProjectJobs from '@/pages/Project/Jobs'
import ModelVersionListCard from '@/pages/Model/ModelVersionListCard'
import DatasetVersionListCard from '@/pages/Dataset/DatasetVersionListCard'
import DatasetOverviewLayout from '@/pages/Dataset/DatasetOverviewLayout'
import JobNewCard from '@/pages/Project/JobNewCard'
import DatasetNewCard from '@/pages/Project/DatasetNewCard'
import JobTasks from '@/pages/Job/JobTasks'
import JobOverviewLayout from '@/pages/Job/JobOverviewLayout'
import SettingsOverviewLayout from '@/pages/Settings/SettingsOverviewLayout'
import SettingAgentListCard from '@/pages/Settings/SettingAgentListCard'
import RuntimeVersionListCard from '@/pages/Runtime/RuntimeVersionListCard'
import RuntimeVersionOverviewFiles from '@/pages/Runtime/RuntimeVersionOverviewFiles'
import ProjectRuntimes from '@/pages/Project/Runtimes'
import ProjectEvaluations from '@/pages/Project/Evaluations'
import EvaluationOverviewLayout from '@/pages/Evaluation/EvaluationOverviewLayout'
import LoginLayout from '@/pages/Home/LoginLayout'
import ResetPassword from '@/pages/Home/ResetPassword'
import CenterLayout from '@/pages/CenterLayout'
import ProjectOverview from '@/pages/Project/Overview'
import DatasetVersionOverview from '@/pages/Dataset/DatasetVersionOverview'
import DatasetVersionOverviewMeta from '@/pages/Dataset/DatasetVersionOverviewMeta'
import DatasetVersionOverviewFiles from '@/pages/Dataset/DatasetVersionOverviewFiles'
import RuntimeVersionOverviewMeta from '@/pages/Runtime/RuntimeVersionOverviewMeta'
import RuntimeVersionOverview from '@/pages/Runtime/RuntimeVersionOverview'
import RuntimeOverviewLayout from '@/pages/Runtime/RuntimeOverviewLayout'
import SystemSettings from '@/pages/Admin/SystemSettings'
import EvaluationWidgetResults from '@/pages/Evaluation/EvaluationWidgetResults'
import ModelVersionOverviewFiles from '@/pages/Model/ModelVersionOverviewFiles'
import ModelVersionOverview from '@/pages/Model/ModelVersionOverview'
import ModelOverviewLayout from '@/pages/Model/ModelOverviewLayout'
import ProjectTrashes from '@/pages/Project/Trashes'
import TrashLayout from '@/pages/Trash/TrashLayout'
import TrashListCard from '@/pages/Trash/TrashListCard'
import OnlineEval from '@/pages/Project/OnlineEval'
import EvaluationListResult from './pages/Evaluation/EvaluationListResult'
import DatasetBuildListCard from './pages/Dataset/DatasetBuildListCard'
import ModelReadmeOverview from './pages/Model/ModelReadmeOverview'
import ReportOverviewLayout from '@/pages/Report/ReportOverviewLayout'
import ReportListCard from '@/pages/Report/ReportListCard'
import ReportEdit from '@/pages/Report/ReportEdit'
import JobOverview from './pages/Job/JobOverview'
import EvaluationOverview from './pages/Evaluation/EvaluationOverview'
import ClientLogin from '@/pages/Auth/ClientLogin'
import JobServings from '@/pages/Job/JobServings'
import ModelVersionServings from '@/pages/Model/ModelVersionServings'
import FineTuneSpaceListCard from './pages/Space/FineTuneSpaceListCard'
import FineTuneListCard from './pages/Space/FineTuneListCard'
import FineTuneOverviewLayout from './pages/Space/FineTuneOverviewLayout'
import FineTuneNewCard from './pages/Project/FineTuneNewCard'
import FineTuneOverview from './pages/Space/FineTuneOverview'
import FineTuneRunOverviewLayout from './pages/Space/FineTuneRunOverviewLayout'
import JobTaskListCard from './pages/Job/JobTaskListCard'
import FineTuneRunsTaskListCard from './pages/Space/FineTuneRunsTaskListCard'

const unauthed = {
    component: LoginLayout,
    routes: [
        {
            path: '/reset',
            component: ResetPassword,
        },
        {
            path: '/login',
            component: Login,
        },
        {
            from: '/',
            to: '/login',
            component: Redirect,
        },
    ],
}

const authed = {
    routes: [
        {
            path: '/settings/:path?',
            component: SettingsOverviewLayout,
            routes: [
                {
                    path: '/settings/agents',
                    component: SettingAgentListCard,
                },
                {
                    from: '/settings/:path?',
                    to: '/settings/agents',
                    component: Redirect,
                },
            ],
        },
        {
            path: '/auth/:path?',
            component: CenterLayout,
            routes: [
                {
                    path: '/auth/client',
                    component: ClientLogin,
                },
            ],
        },
        {
            path: '/projects/:projectId/members',
            component: CenterLayout,
            routes: [
                {
                    path: '/projects/:projectId/members',
                    component: ProjectMembers,
                },
            ],
        },
        {
            path: '/projects/:projectId/evaluations/:jobId/:path?',
            component: EvaluationOverviewLayout,
            routes: [
                {
                    path: '/projects/:projectId/evaluations/:jobId/overview',
                    component: EvaluationOverview,
                },
                {
                    path: '/projects/:projectId/evaluations/:jobId/results',
                    component: EvaluationWidgetResults,
                },
                {
                    path: '/projects/:projectId/evaluations/:jobId/tasks',
                    component: JobTaskListCard,
                },
                {
                    from: '/projects/:projectId/evaluations/:jobId',
                    to: '/projects/:projectId/evaluations/:jobId/results',
                    component: Redirect,
                },
            ],
        },
        {
            path: '/projects/:projectId/jobs/:jobId/:path?',
            component: JobOverviewLayout,
            routes: [
                {
                    path: '/projects/:projectId/jobs/:jobId/overview',
                    component: JobOverview,
                },
                {
                    path: '/projects/:projectId/jobs/:jobId/tasks',
                    component: JobTaskListCard,
                },
                {
                    path: '/projects/:projectId/jobs/:jobId/results',
                    component: EvaluationWidgetResults,
                },
                {
                    path: '/projects/:projectId/jobs/:jobId/servings',
                    component: JobServings,
                },
                {
                    from: '/projects/:projectId/jobs/:jobId',
                    to: '/projects/:projectId/jobs/:jobId/tasks',
                    component: Redirect,
                },
            ],
        },
        {
            path: '/projects/:projectId/datasets/builds',
            component: ProjectLayout,
            routes: [
                {
                    path: '/projects/:projectId/datasets/builds',
                    component: DatasetBuildListCard,
                },
            ],
        },
        {
            path: '/projects/:projectId/datasets/:datasetId/:path?/:datasetVersionId?/:path?/:fileId?',
            component: DatasetOverviewLayout,
            routes: [
                {
                    path: '/projects/:projectId/datasets/:datasetId',
                    component: DatasetVersionListCard,
                },
                {
                    path: '/projects/:projectId/datasets/:datasetId/versions/:datasetVersionId/overview',
                    component: DatasetVersionOverview,
                },
                {
                    path: '/projects/:projectId/datasets/:datasetId/versions/:datasetVersionId/meta',
                    component: DatasetVersionOverviewMeta,
                },
                {
                    path: '/projects/:projectId/datasets/:datasetId/versions/:datasetVersionId/files',
                    component: DatasetVersionOverviewFiles,
                },
                {
                    path: '/projects/:projectId/datasets/:datasetId/versions/:datasetVersionId/files/:fileId?/:path?',
                    component: DatasetVersionOverviewFiles,
                },
                {
                    from: '/projects/:projectId/datasets/:datasetId/:path*',
                    to: '/projects/:projectId/datasets/:datasetId',
                    component: Redirect,
                },
            ],
        },
        {
            path: '/projects/:projectId/runtimes/:runtimeId/:path?/:runtimeVersionId?/:path?/:fileId?',
            component: RuntimeOverviewLayout,
            routes: [
                {
                    path: '/projects/:projectId/runtimes/:runtimeId',
                    component: RuntimeVersionListCard,
                },
                {
                    path: '/projects/:projectId/runtimes/:runtimeId/versions/:runtimeVersionId/overview',
                    component: RuntimeVersionOverview,
                },
                {
                    path: '/projects/:projectId/runtimes/:runtimeId/versions/:runtimeVersionId/meta',
                    component: RuntimeVersionOverviewMeta,
                },
                {
                    path: '/projects/:projectId/runtimes/:runtimeId/versions/:runtimeVersionId/files',
                    component: RuntimeVersionOverviewFiles,
                },
                {
                    from: '/projects/:projectId/runtimes/:runtimeId/:path*',
                    to: '/projects/:projectId/runtimes/:runtimeId',
                    component: Redirect,
                },
            ],
        },
        {
            path: '/projects/:projectId/models/:modelId/:path?/:modelVersionId?/:path?/:fileId?',
            component: ModelOverviewLayout,
            routes: [
                {
                    path: '/projects/:projectId/models/:modelId',
                    component: ModelVersionListCard,
                },
                {
                    path: '/projects/:projectId/models/:modelId/versions/:modelVersionId/overview',
                    component: ModelVersionOverview,
                },
                {
                    path: '/projects/:projectId/models/:modelId/versions/:modelVersionId/readme',
                    component: ModelReadmeOverview,
                },
                {
                    path: '/projects/:projectId/models/:modelId/versions/:modelVersionId/files',
                    component: ModelVersionOverviewFiles,
                },
                {
                    path: '/projects/:projectId/models/:modelId/versions/:modelVersionId/servings',
                    component: ModelVersionServings,
                },
                {
                    from: '/projects/:projectId/models/:modelId/:path*',
                    to: '/projects/:projectId/models/:modelId',
                    component: Redirect,
                },
            ],
        },
        {
            path: '/projects/:projectId/reports/:reportId',
            component: ReportOverviewLayout,
            routes: [
                {
                    path: '/projects/:projectId/reports/:reportId',
                    component: ReportEdit,
                },
                {
                    from: '/projects/:projectId/reports/:reportId',
                    to: '/projects/:projectId/reports',
                    component: Redirect,
                },
            ],
        },
        {
            path: '/projects/:projectId/spaces/:spaceId/:path/:fineTuneId/:path',
            component: FineTuneRunOverviewLayout,
            routes: [
                {
                    path: '/projects/:projectId/spaces/:spaceId/fine-tunes/:fineTuneId?/overview',
                    component: FineTuneOverview,
                },
                {
                    path: '/projects/:projectId/spaces/:spaceId/fine-tunes/:fineTuneId?/tasks',
                    component: FineTuneRunsTaskListCard,
                },
                {
                    to: '/projects/:projectId/spaces/:spaceId/fine-tunes/:fineTuneId/overview',
                    component: Redirect,
                },
            ],
        },

        {
            path: '/projects/:projectId/spaces/:spaceId/:path?',
            component: FineTuneOverviewLayout,
            routes: [
                {
                    path: '/projects/:projectId/spaces/:spaceId/fine-tunes/:fineTuneId?',
                    component: FineTuneListCard,
                },
                {
                    from: '/projects/:projectId/spaces/:spaceId/:path*',
                    to: '/projects/:projectId/spaces/:spaceId/fine-tunes',
                    component: Redirect,
                },
            ],
        },
        {
            path: '/projects/:projectId/trashes',
            component: TrashLayout,
            routes: [
                {
                    path: '/projects/:projectId/trashes',
                    component: TrashListCard,
                },
                {
                    to: '/projects/:projectId/trashes',
                    component: Redirect,
                },
            ],
        },
        {
            path: '/admin/:path?',
            component: AdminLayout,
            routes: [
                {
                    path: '/admin/users',
                    component: UserManagement,
                },
                {
                    path: '/admin/settings',
                    component: SystemSettings,
                },
                {
                    from: '/admin/:path*',
                    to: '/admin/users',
                    component: Redirect,
                },
            ],
        },
        {
            path: '/projects/:projectId/:path*',
            component: ProjectLayout,
            routes: [
                {
                    path: '/projects/:projectId/reports',
                    component: ReportListCard,
                },
                {
                    path: '/projects/:projectId/new_report',
                    component: ReportEdit,
                },
                {
                    path: '/projects/:projectId/models',
                    component: ProjectModels,
                },
                {
                    path: '/projects/:projectId/trashes',
                    component: ProjectTrashes,
                },
                {
                    path: '/projects/:projectId/datasets',
                    component: ProjectDatasets,
                },
                {
                    path: '/projects/:projectId/jobs',
                    component: ProjectJobs,
                },
                {
                    path: '/projects/:projectId/evaluations',
                    component: ProjectEvaluations,
                },
                {
                    path: '/projects/:projectId/results',
                    component: EvaluationListResult,
                },
                {
                    path: '/projects/:projectId/runtimes',
                    component: ProjectRuntimes,
                },
                {
                    path: '/projects/:projectId/new_job',
                    component: JobNewCard,
                },
                {
                    path: '/projects/:projectId/new_fine_tune/:spaceId',
                    component: FineTuneNewCard,
                },
                {
                    path: '/projects/:projectId/new_dataset/:datasetId?',
                    component: DatasetNewCard,
                },
                {
                    path: '/projects/:projectId/online_eval/:modelId/:modelVersionId?',
                    component: OnlineEval,
                },
                {
                    path: '/projects/:projectId/members',
                    component: ProjectMembers,
                },
                {
                    path: '/projects/:projectId/overview',
                    component: ProjectOverview,
                },
                {
                    path: '/projects/:projectId/spaces',
                    component: FineTuneSpaceListCard,
                },
                {
                    from: '/projects/:projectId/:path*',
                    to: '/projects/:projectId/overview',
                    component: Redirect,
                },
            ],
        },
        {
            path: '/projects/:projectId',
            component: ProjectLayout,
            routes: [
                {
                    path: '/projects/:projectId',
                    component: ProjectOverview,
                },
                {
                    from: '/projects/:projectId',
                    to: '/projects/:projectId/overview',
                },
            ],
        },
        {
            component: CenterLayout,
            routes: [
                {
                    path: '/projects',
                    component: ProjectListCard,
                },
                {
                    from: '/',
                    to: '/projects',
                },
            ],
        },
    ],
}

export { unauthed, authed }
