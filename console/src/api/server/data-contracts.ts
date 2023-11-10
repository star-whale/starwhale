/* eslint-disable */
/* tslint:disable */
/*
 * ---------------------------------------------------------------
 * ## THIS FILE WAS GENERATED VIA SWAGGER-TYPESCRIPT-API        ##
 * ##                                                           ##
 * ## AUTHOR: acacode                                           ##
 * ## SOURCE: https://github.com/acacode/swagger-typescript-api ##
 * ---------------------------------------------------------------
 */

export interface IUserUpdateStateRequest {
    isEnabled: boolean
}

export interface IResponseMessageString {
    code: string
    message: string
    data: string
}

export interface IUserUpdatePasswordRequest {
    currentUserPwd: string
    newPwd: string
}

export interface IUserRoleUpdateRequest {
    currentUserPwd: string
    roleId: string
}

export interface IUpdateProjectRequest {
    /** @pattern ^[a-zA-Z][a-zA-Z\d_-]{2,80}$ */
    projectName?: string
    privacy?: string
    description?: string
    readme?: string
}

export interface IResponseMessageObject {
    code: string
    message: string
    data: object
}

export interface IRuntimeTagRequest {
    force?: boolean
    tag: string
}

export interface IUpdateReportRequest {
    /**
     * @minLength 0
     * @maxLength 255
     */
    title?: string
    /**
     * @minLength 0
     * @maxLength 255
     */
    description?: string
    content?: string
    shared?: boolean
}

export interface IModelUpdateRequest {
    tag?: string
    built_in_runtime?: string
}

export interface IJobModifyRequest {
    comment: string
}

export interface ISftSpaceCreateRequest {
    name: string
    description: string
}

export interface IApplySignedUrlRequest {
    flag?: string
    pathPrefix: string
    /** @uniqueItems true */
    files: string[]
}

export interface IResponseMessageSignedUrlResponse {
    code: string
    message: string
    data: ISignedUrlResponse
}

export interface ISignedUrlResponse {
    pathPrefix?: string
    signedUrls?: Record<string, string>
}

export interface IUserRequest {
    /** @pattern ^[a-zA-Z][a-zA-Z\d_-]{3,32}$ */
    userName: string
    userPwd: string
    salt?: string
}

export interface IUserCheckPasswordRequest {
    currentUserPwd: string
}

export interface IResource {
    name?: string
    /** @format float */
    max?: number
    /** @format float */
    min?: number
    /** @format float */
    defaults?: number
}

export interface IResourcePool {
    name?: string
    nodeSelector?: Record<string, string>
    resources?: IResource[]
    tolerations?: IToleration[]
    metadata?: Record<string, string>
    isPrivate?: boolean
    visibleUserIds?: number[]
}

export interface IToleration {
    key?: string
    operator?: string
    value?: string
    effect?: string
    /** @format int64 */
    tolerationSeconds?: number
}

export interface IUserRoleAddRequest {
    currentUserPwd: string
    userId: string
    roleId: string
}

export interface ICreateProjectRequest {
    /** @pattern ^[a-zA-Z][a-zA-Z\d_-]{2,80}$ */
    projectName: string
    privacy: string
    description: string
}

export interface ICreateModelVersionRequest {
    metaBlobId: string
    builtInRuntime?: string
    force?: boolean
}

export interface ICreateJobTemplateRequest {
    name: string
    jobUrl: string
}

export interface IModelServingRequest {
    modelVersionUrl: string
    runtimeVersionUrl: string
    resourcePool?: string
    /**
     * @deprecated
     * @format int64
     */
    ttlInSeconds?: number
    spec?: string
}

/**
 * Model Serving
 * Model Serving object
 */
export interface IModelServingVo {
    id: string
    baseUri?: string
}

export interface IResponseMessageModelServingVo {
    code: string
    message: string
    /** Model Serving object */
    data: IModelServingVo
}

/** user defined running configurations such environment variables */
export interface IRunEnvs {
    envVars?: Record<string, string>
}

/**
 * Runtime
 * Build runtime image result
 */
export interface IBuildImageResult {
    success?: boolean
    message?: string
}

export interface IResponseMessageBuildImageResult {
    code: string
    message: string
    /** Build runtime image result */
    data: IBuildImageResult
}

export interface IRuntimeRevertRequest {
    versionUrl: string
}

export interface IClientRuntimeRequest {
    runtime?: string
    project?: string
    force?: string
    manifest?: string
}

export interface ICreateReportRequest {
    /**
     * @minLength 1
     * @maxLength 255
     */
    title: string
    /**
     * @minLength 0
     * @maxLength 255
     */
    description?: string
    /**
     * @minLength 1
     * @maxLength 2147483647
     */
    content: string
}

export interface ITransferReportRequest {
    targetProjectUrl: string
}

export interface IModelTagRequest {
    force?: boolean
    tag: string
}

export interface IRevertModelVersionRequest {
    versionUrl: string
}

export interface IJobRequest {
    /** @format int64 */
    modelVersionId?: number
    datasetVersionIds?: number[]
    /** @format int64 */
    runtimeVersionId?: number
    /** @format int64 */
    timeToLiveInSec?: number
    /** @deprecated */
    modelVersionUrl?: string
    /** @deprecated */
    datasetVersionUrls?: string
    /** @deprecated */
    runtimeVersionUrl?: string
    comment?: string
    resourcePool: string
    handler?: string
    stepSpecOverWrites?: string
    type?: 'EVALUATION' | 'TRAIN' | 'FINE_TUNE' | 'SERVING' | 'BUILT_IN'
    devMode?: boolean
    devPassword?: string
    devWay?: 'VS_CODE'
}

export interface IExecRequest {
    command: string[]
}

export interface IExecResponse {
    stdout?: string
    stderr?: string
}

export interface IResponseMessageExecResponse {
    code: string
    message: string
    data: IExecResponse
}

export interface IJobModifyPinRequest {
    pinned: boolean
}

export interface IEventRequest {
    eventType: 'INFO' | 'WARNING' | 'ERROR'
    source: 'CLIENT' | 'SERVER' | 'NODE'
    relatedResource: IRelatedResource
    message: string
    data?: string
    /** @format int64 */
    timestamp?: number
}

export interface IRelatedResource {
    eventResourceType: 'JOB' | 'TASK' | 'RUN'
    /** @format int64 */
    id: number
}

export interface IResponseMessageMapStringString {
    code: string
    message: string
    data: Record<string, string>
}

export interface IConfigRequest {
    name: string
    content: string
}

export interface IDatasetTagRequest {
    force?: boolean
    tag: string
}

export interface IDataConsumptionRequest {
    sessionId?: string
    consumerId?: string
    /** @format int32 */
    batchSize?: number
    start?: string
    startType?: string
    startInclusive?: boolean
    end?: string
    endType?: string
    endInclusive?: boolean
    processedData?: IDataIndexDesc[]
    /** @deprecated */
    serial?: boolean
}

export interface IDataIndexDesc {
    start?: string
    startType?: string
    startInclusive?: boolean
    end?: string
    endType?: string
    endInclusive?: boolean
}

export interface INullableResponseMessageDataIndexDesc {
    code: string
    message: string
    data?: IDataIndexDesc
}

export interface IRevertDatasetRequest {
    versionUrl: string
}

export interface IDatasetUploadRequest {
    /** @format int64 */
    uploadId: number
    partName?: string
    signature?: string
    uri?: string
    desc?: 'MANIFEST' | 'SRC_TAR' | 'SRC' | 'MODEL' | 'DATA' | 'UNKNOWN'
    phase: 'MANIFEST' | 'BLOB' | 'END' | 'CANCEL'
    force?: string
    project: string
    swds: string
}

export interface IResponseMessageUploadResult {
    code: string
    message: string
    data: IUploadResult
}

export interface IUploadResult {
    /** @format int64 */
    uploadId?: number
}

export interface IDatasetBuildRequest {
    type: 'IMAGE' | 'VIDEO' | 'AUDIO'
    shared?: boolean
    storagePath: string
}

export interface IResponseMessageMapObjectObject {
    code: string
    message: string
    data: Record<string, object>
}

export interface ISftCreateRequest {
    /** @format int64 */
    modelVersionId?: number
    datasetVersionIds?: number[]
    /** @format int64 */
    runtimeVersionId?: number
    /** @format int64 */
    timeToLiveInSec?: number
    /** @deprecated */
    modelVersionUrl?: string
    /** @deprecated */
    datasetVersionUrls?: string
    /** @deprecated */
    runtimeVersionUrl?: string
    comment?: string
    resourcePool: string
    handler?: string
    stepSpecOverWrites?: string
    type?: 'EVALUATION' | 'TRAIN' | 'FINE_TUNE' | 'SERVING' | 'BUILT_IN'
    devMode?: boolean
    devPassword?: string
    devWay?: 'VS_CODE'
    evalDatasetVersionIds?: number[]
}

export interface IColumnSchemaDesc {
    name?: string
    /** @format int32 */
    index?: number
    type?: string
    pythonType?: string
    elementType?: IColumnSchemaDesc
    keyType?: IColumnSchemaDesc
    valueType?: IColumnSchemaDesc
    attributes?: IColumnSchemaDesc[]
    sparseKeyValuePairSchema?: Record<string, IKeyValuePairSchema>
}

export interface IKeyValuePairSchema {
    keyType?: IColumnSchemaDesc
    valueType?: IColumnSchemaDesc
}

export interface IRecordDesc {
    values: IRecordValueDesc[]
}

export interface IRecordValueDesc {
    key: string
    value?: object
}

export interface ITableSchemaDesc {
    keyColumn?: string
    columnSchemaList?: IColumnSchemaDesc[]
}

export interface IUpdateTableRequest {
    tableName?: string
    tableSchemaDesc?: ITableSchemaDesc
    records?: IRecordDesc[]
}

export interface IColumnDesc {
    columnName?: string
    alias?: string
}

export interface IScanTableRequest {
    tables?: ITableDesc[]
    start?: string
    startType?: string
    startInclusive?: boolean
    end?: string
    endType?: string
    endInclusive?: boolean
    /** @format int32 */
    limit?: number
    keepNone?: boolean
    rawResult?: boolean
    encodeWithType?: boolean
    ignoreNonExistingTable?: boolean
}

export interface ITableDesc {
    tableName?: string
    columnPrefix?: string
    columns?: IColumnDesc[]
    keepNone?: boolean
    revision?: string
}

export interface IColumnHintsDesc {
    typeHints?: string[]
    columnValueHints?: string[]
    elementHints?: IColumnHintsDesc
    keyHints?: IColumnHintsDesc
    valueHints?: IColumnHintsDesc
}

export interface IRecordListVo {
    columnTypes?: IColumnSchemaDesc[]
    columnHints?: Record<string, IColumnHintsDesc>
    records?: Record<string, object>[]
    lastKey?: string
}

export interface IResponseMessageRecordListVo {
    code: string
    message: string
    data: IRecordListVo
}

export interface IOrderByDesc {
    columnName?: string
    descending?: boolean
}

export interface IQueryTableRequest {
    tableName?: string
    columns?: IColumnDesc[]
    orderBy?: IOrderByDesc[]
    descending?: boolean
    filter?: ITableQueryFilterDesc
    /** @format int32 */
    start?: number
    /** @format int32 */
    limit?: number
    keepNone?: boolean
    rawResult?: boolean
    encodeWithType?: boolean
    ignoreNonExistingTable?: boolean
    revision?: string
}

export interface ITableQueryFilterDesc {
    operator: string
    operands?: ITableQueryOperandDesc[]
}

export interface ITableQueryOperandDesc {
    filter?: ITableQueryFilterDesc
    columnName?: string
    boolValue?: boolean
    /** @format int64 */
    intValue?: number
    /** @format double */
    floatValue?: number
    stringValue?: string
    bytesValue?: string
}

export interface IListTablesRequest {
    prefix?: string
    /** @uniqueItems true */
    prefixes?: string[]
}

export interface IResponseMessageTableNameListVo {
    code: string
    message: string
    data: ITableNameListVo
}

export interface ITableNameListVo {
    tables?: string[]
}

export type IFlushRequest = object

export interface IInitUploadBlobRequest {
    contentMd5: string
    /** @format int64 */
    contentLength: number
}

export interface IInitUploadBlobResult {
    status?: 'OK' | 'EXISTED'
    blobId?: string
    signedUrl?: string
}

export interface IResponseMessageInitUploadBlobResult {
    code: string
    message: string
    data: IInitUploadBlobResult
}

export interface ICompleteUploadBlobResult {
    blobId?: string
}

export interface IResponseMessageCompleteUploadBlobResult {
    code: string
    message: string
    data: ICompleteUploadBlobResult
}

export interface IPageInfoUserVo {
    /** @format int64 */
    total?: number
    list?: IUserVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoUserVo {
    code: string
    message: string
    data: IPageInfoUserVo
}

/**
 * User
 * User object
 */
export interface IUserVo {
    id: string
    name: string
    /** @format int64 */
    createdTime: number
    isEnabled: boolean
    systemRole?: string
    projectRoles?: Record<string, string>
}

export interface IResponseMessageUserVo {
    code: string
    message: string
    /** User object */
    data: IUserVo
}

/**
 * Role
 * Project Role object
 */
export interface IProjectMemberVo {
    id: string
    /** User object */
    user: IUserVo
    /** Project object */
    project: IProjectVo
    /** Role object */
    role: IRoleVo
}

/**
 * Project
 * Project object
 */
export interface IProjectVo {
    id: string
    name: string
    description?: string
    privacy: string
    /** @format int64 */
    createdTime: number
    /** User object */
    owner: IUserVo
    statistics?: IStatisticsVo
}

export interface IResponseMessageListProjectMemberVo {
    code: string
    message: string
    data: IProjectMemberVo[]
}

/**
 * Role
 * Role object
 */
export interface IRoleVo {
    id: string
    name: string
    code: string
    description?: string
}

export interface IStatisticsVo {
    /** @format int32 */
    modelCounts: number
    /** @format int32 */
    datasetCounts: number
    /** @format int32 */
    runtimeCounts: number
    /** @format int32 */
    memberCounts: number
    /** @format int32 */
    evaluationCounts: number
}

export interface IResponseMessageSystemVersionVo {
    code: string
    message: string
    /** System version */
    data: ISystemVersionVo
}

/**
 * Version
 * System version
 */
export interface ISystemVersionVo {
    id?: string
    version?: string
}

export interface IResponseMessageListResourcePool {
    code: string
    message: string
    data: IResourcePool[]
}

/**
 * Features
 * System features
 */
export interface IFeaturesVo {
    disabled: string[]
}

export interface IResponseMessageFeaturesVo {
    code: string
    message: string
    /** System features */
    data: IFeaturesVo
}

/**
 * Device
 * Device information
 */
export interface IDeviceVo {
    name: string
}

export interface IResponseMessageListDeviceVo {
    code: string
    message: string
    data: IDeviceVo[]
}

export interface IResponseMessageListRoleVo {
    code: string
    message: string
    data: IRoleVo[]
}

/**
 * Report
 * Report object
 */
export interface IReportVo {
    /** @format int64 */
    id: number
    uuid: string
    title: string
    content?: string
    description?: string
    shared?: boolean
    /** User object */
    owner: IUserVo
    /** @format int64 */
    createdTime: number
    /** @format int64 */
    modifiedTime: number
}

export interface IResponseMessageReportVo {
    code: string
    message: string
    /** Report object */
    data: IReportVo
}

export interface IPageInfoProjectVo {
    /** @format int64 */
    total?: number
    list?: IProjectVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoProjectVo {
    code: string
    message: string
    data: IPageInfoProjectVo
}

export interface IFileNode {
    name?: string
    signature?: string
    flag?: 'added' | 'updated' | 'deleted' | 'unchanged'
    mime?: string
    type?: 'directory' | 'file'
    desc?: string
    size?: string
}

export interface IListFilesResult {
    files?: IFileNode[]
}

export interface IResponseMessageListFilesResult {
    code: string
    message: string
    data: IListFilesResult
}

export interface IResponseMessageProjectVo {
    code: string
    message: string
    /** Project object */
    data: IProjectVo
}

export interface IPageInfoTrashVo {
    /** @format int64 */
    total?: number
    list?: ITrashVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoTrashVo {
    code: string
    message: string
    data: IPageInfoTrashVo
}

export interface ITrashVo {
    id?: string
    name?: string
    type?: string
    /** @format int64 */
    trashedTime?: number
    /** @format int64 */
    size?: number
    trashedBy?: string
    /** @format int64 */
    lastUpdatedTime?: number
    /** @format int64 */
    retentionTime?: number
}

export interface IJobTemplateVo {
    /** @format int64 */
    id?: number
    name?: string
    /** @format int64 */
    jobId?: number
}

export interface IResponseMessageListJobTemplateVo {
    code: string
    message: string
    data: IJobTemplateVo[]
}

export interface IResponseMessageJobTemplateVo {
    code: string
    message: string
    data: IJobTemplateVo
}

export interface IPageInfoRuntimeVo {
    /** @format int64 */
    total?: number
    list?: IRuntimeVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoRuntimeVo {
    code: string
    message: string
    data: IPageInfoRuntimeVo
}

/**
 * RuntimeVersion
 * Runtime version object
 */
export interface IRuntimeVersionVo {
    tags?: string[]
    latest: boolean
    id: string
    runtimeId: string
    name: string
    alias: string
    meta?: string
    image: string
    builtImage?: string
    /** @format int64 */
    createdTime: number
    /** User object */
    owner?: IUserVo
    shared: boolean
}

/**
 * Runtime
 * Runtime object
 */
export interface IRuntimeVo {
    id: string
    name: string
    /** @format int64 */
    createdTime: number
    /** User object */
    owner: IUserVo
    /** Runtime version object */
    version: IRuntimeVersionVo
}

/**
 * StorageFile
 * Storage file object
 */
export interface IFlattenFileVo {
    name?: string
    size?: string
}

export interface IResponseMessageRuntimeInfoVo {
    code: string
    message: string
    /** Runtime information object */
    data: IRuntimeInfoVo
}

/**
 * RuntimeInfo
 * Runtime information object
 */
export interface IRuntimeInfoVo {
    /** Runtime version object */
    versionInfo: IRuntimeVersionVo
    id: string
    name: string
    versionId: string
    versionName: string
    versionAlias: string
    versionTag?: string
    versionMeta?: string
    manifest: string
    /** @format int32 */
    shared: number
    /** @format int64 */
    createdTime: number
    files?: IFlattenFileVo[]
}

export interface IPageInfoRuntimeVersionVo {
    /** @format int64 */
    total?: number
    list?: IRuntimeVersionVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoRuntimeVersionVo {
    code: string
    message: string
    data: IPageInfoRuntimeVersionVo
}

export interface IResponseMessageListString {
    code: string
    message: string
    data: string[]
}

export interface IResponseMessageLong {
    code: string
    message: string
    /** @format int64 */
    data: number
}

export interface IResponseMessageListRuntimeViewVo {
    code: string
    message: string
    data: IRuntimeViewVo[]
}

/**
 * Runtime
 * Runtime Version View object
 */
export interface IRuntimeVersionViewVo {
    id: string
    versionName: string
    alias: string
    latest: boolean
    /** @format int32 */
    shared: number
    /** @format int64 */
    createdTime: number
}

/**
 * Runtime
 * Runtime View object
 */
export interface IRuntimeViewVo {
    ownerName: string
    projectName: string
    runtimeId: string
    runtimeName: string
    shared: boolean
    versions: IRuntimeVersionViewVo[]
}

export interface IPageInfoReportVo {
    /** @format int64 */
    total?: number
    list?: IReportVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoReportVo {
    code: string
    message: string
    data: IPageInfoReportVo
}

export interface IContainerSpec {
    image?: string
    cmds?: string[]
    entrypoint?: string[]
}

export interface IEnv {
    name?: string
    value?: string
}

/**
 * Model
 * Model Version View Object
 */
export interface IModelVersionViewVo {
    id: string
    versionName: string
    alias: string
    latest: boolean
    tags?: string[]
    /** @format int32 */
    shared: number
    stepSpecs: IStepSpec[]
    builtInRuntime?: string
    /** @format int64 */
    createdTime: number
}

/**
 * Model View
 * Model View Object
 */
export interface IModelViewVo {
    ownerName: string
    projectName: string
    modelId: string
    modelName: string
    shared: boolean
    versions: IModelVersionViewVo[]
}

export interface IParameterSignature {
    name: string
    required?: boolean
    multiple?: boolean
}

export interface IResponseMessageListModelViewVo {
    code: string
    message: string
    data: IModelViewVo[]
}

export interface IRuntimeResource {
    type?: string
    /** @format float */
    request?: number
    /** @format float */
    limit?: number
}

export interface IStepSpec {
    name: string
    /** @format int32 */
    concurrency?: number
    /** @format int32 */
    replicas: number
    /** @format int32 */
    backoffLimit?: number
    needs?: string[]
    resources?: IRuntimeResource[]
    env?: IEnv[]
    /** @format int32 */
    expose?: number
    virtual?: boolean
    job_name?: string
    show_name: string
    require_dataset?: boolean
    container_spec?: IContainerSpec
    ext_cmd_args?: string
    parameters_sig?: IParameterSignature[]
}

/**
 * Dataset
 * Dataset Version View object
 */
export interface IDatasetVersionViewVo {
    id: string
    versionName: string
    alias?: string
    latest: boolean
    /** @format int32 */
    shared: number
    /** @format int64 */
    createdTime: number
}

/**
 * Dataset
 * Dataset View object
 */
export interface IDatasetViewVo {
    ownerName: string
    projectName: string
    datasetId: string
    datasetName: string
    shared: boolean
    versions: IDatasetVersionViewVo[]
}

export interface IResponseMessageListDatasetViewVo {
    code: string
    message: string
    data: IDatasetViewVo[]
}

/**
 * ModelVersion
 * Model version object
 */
export interface IModelVersionVo {
    latest: boolean
    tags?: string[]
    stepSpecs: IStepSpec[]
    id: string
    name: string
    alias: string
    /** @format int64 */
    size?: number
    /** @format int64 */
    createdTime: number
    /** User object */
    owner?: IUserVo
    shared: boolean
    builtInRuntime?: string
}

/**
 * Model
 * Model object
 */
export interface IModelVo {
    id: string
    name: string
    /** @format int64 */
    createdTime: number
    /** User object */
    owner: IUserVo
    /** Model version object */
    version: IModelVersionVo
}

export interface IPageInfoModelVo {
    /** @format int64 */
    total?: number
    list?: IModelVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoModelVo {
    code: string
    message: string
    data: IPageInfoModelVo
}

/**
 * ModelInfo
 * Model information object
 */
export interface IModelInfoVo {
    /** Model version object */
    versionInfo: IModelVersionVo
    id: string
    name: string
    versionAlias: string
    versionId: string
    versionName: string
    versionTag?: string
    /** @format int64 */
    createdTime: number
    /** @format int32 */
    shared: number
}

export interface IResponseMessageModelInfoVo {
    code: string
    message: string
    /** Model information object */
    data: IModelInfoVo
}

export interface IPageInfoModelVersionVo {
    /** @format int64 */
    total?: number
    list?: IModelVersionVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoModelVersionVo {
    code: string
    message: string
    data: IPageInfoModelVersionVo
}

export interface IResponseMessageMapStringListFileNode {
    code: string
    message: string
    data: Record<string, IFileNode[]>
}

/**
 * DatasetVersion
 * Dataset version object
 */
export interface IDatasetVersionVo {
    tags?: string[]
    latest: boolean
    indexTable?: string
    id: string
    name: string
    alias?: string
    meta?: string
    /** @format int64 */
    createdTime: number
    /** User object */
    owner?: IUserVo
    shared: boolean
}

/**
 * Dataset
 * Dataset object
 */
export interface IDatasetVo {
    id: string
    name: string
    /** @format int64 */
    createdTime: number
    /** User object */
    owner?: IUserVo
    /** Dataset version object */
    version: IDatasetVersionVo
}

export interface IExposedLinkVo {
    type: 'DEV_MODE' | 'WEB_HANDLER'
    name: string
    link: string
}

/**
 * Job
 * Job object
 */
export interface IJobVo {
    exposedLinks: IExposedLinkVo[]
    id: string
    uuid: string
    modelName: string
    modelVersion: string
    /** Model object */
    model: IModelVo
    jobName?: string
    datasets?: string[]
    datasetList?: IDatasetVo[]
    /** Runtime object */
    runtime: IRuntimeVo
    isBuiltinRuntime?: boolean
    device?: string
    /** @format int32 */
    deviceAmount?: number
    /** User object */
    owner: IUserVo
    /** @format int64 */
    createdTime: number
    /** @format int64 */
    stopTime?: number
    jobStatus: 'CREATED' | 'READY' | 'PAUSED' | 'RUNNING' | 'CANCELLING' | 'CANCELED' | 'SUCCESS' | 'FAIL' | 'UNKNOWN'
    comment?: string
    stepSpec?: string
    resourcePool: string
    /** @format int64 */
    duration?: number
    /** @format int64 */
    pinnedTime?: number
}

export interface IPageInfoJobVo {
    /** @format int64 */
    total?: number
    list?: IJobVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoJobVo {
    code: string
    message: string
    data: IPageInfoJobVo
}

export interface IResponseMessageJobVo {
    code: string
    message: string
    /** Job object */
    data: IJobVo
}

export interface IPageInfoTaskVo {
    /** @format int64 */
    total?: number
    list?: ITaskVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoTaskVo {
    code: string
    message: string
    data: IPageInfoTaskVo
}

export interface IRunVo {
    /** @format int64 */
    id?: number
    /** @format int64 */
    taskId?: number
    status?: 'PENDING' | 'RUNNING' | 'FINISHED' | 'FAILED'
    ip?: string
    /** @format int64 */
    startTime?: number
    /** @format int64 */
    finishTime?: number
    failedReason?: string
}

/**
 * Task
 * Task object
 */
export interface ITaskVo {
    id: string
    uuid: string
    /** @format int64 */
    startedTime?: number
    /** @format int64 */
    finishedTime?: number
    taskStatus:
        | 'CREATED'
        | 'READY'
        | 'ASSIGNING'
        | 'PAUSED'
        | 'PREPARING'
        | 'RUNNING'
        | 'RETRYING'
        | 'SUCCESS'
        | 'CANCELLING'
        | 'CANCELED'
        | 'FAIL'
        | 'UNKNOWN'
    /** @format int32 */
    retryNum?: number
    resourcePool: string
    stepName: string
    exposedLinks?: IExposedLinkVo[]
    failedReason?: string
    runs?: IRunVo[]
}

export interface IResponseMessageTaskVo {
    code: string
    message: string
    /** Task object */
    data: ITaskVo
}

export interface IResponseMessageListRunVo {
    code: string
    message: string
    data: IRunVo[]
}

export interface IEventVo {
    eventType: 'INFO' | 'WARNING' | 'ERROR'
    source: 'CLIENT' | 'SERVER' | 'NODE'
    relatedResource: IRelatedResource
    message: string
    data?: string
    /** @format int64 */
    timestamp?: number
    /** @format int64 */
    id?: number
}

export interface IResponseMessageListEventVo {
    code: string
    message: string
    data: IEventVo[]
}

export interface IGraph {
    /** @format int64 */
    id?: number
    groupingNodes?: Record<string, IGraphNode[]>
    edges?: IGraphEdge[]
}

export interface IGraphEdge {
    /** @format int64 */
    from?: number
    /** @format int64 */
    to?: number
    content?: string
}

export interface IGraphNode {
    /** @format int64 */
    id?: number
    type?: string
    content?: object
    group?: string
    /** @format int64 */
    entityId?: number
}

export interface IResponseMessageGraph {
    code: string
    message: string
    data: IGraph
}

export interface IAttributeValueVo {
    name?: string
    type?: string
    value?: string
}

export interface IPageInfoSummaryVo {
    /** @format int64 */
    total?: number
    list?: ISummaryVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoSummaryVo {
    code: string
    message: string
    data: IPageInfoSummaryVo
}

/**
 * Evaluation
 * Evaluation Summary object
 */
export interface ISummaryVo {
    id: string
    uuid: string
    projectId: string
    projectName: string
    modelName: string
    modelVersion: string
    datasets?: string
    runtime: string
    device?: string
    /** @format int32 */
    deviceAmount?: number
    /** @format int64 */
    createdTime: number
    /** @format int64 */
    stopTime?: number
    owner: string
    /** @format int64 */
    duration?: number
    jobStatus: 'CREATED' | 'READY' | 'PAUSED' | 'RUNNING' | 'CANCELLING' | 'CANCELED' | 'SUCCESS' | 'FAIL' | 'UNKNOWN'
    attributes?: IAttributeValueVo[]
}

/**
 * Evaluation
 * Evaluation View Config object
 */
export interface IConfigVo {
    name?: string
    content?: string
    /** @format int64 */
    createTime?: number
}

export interface IResponseMessageConfigVo {
    code: string
    message: string
    /** Evaluation View Config object */
    data: IConfigVo
}

/**
 * Evaluation
 * Evaluation Attribute object
 */
export interface IAttributeVo {
    name?: string
    type?: string
}

export interface IResponseMessageListAttributeVo {
    code: string
    message: string
    data: IAttributeVo[]
}

export interface IPageInfoDatasetVo {
    /** @format int64 */
    total?: number
    list?: IDatasetVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoDatasetVo {
    code: string
    message: string
    data: IPageInfoDatasetVo
}

/**
 * DatasetInfo
 * SWDataset information object
 */
export interface IDatasetInfoVo {
    indexTable?: string
    /** Dataset version object */
    versionInfo?: IDatasetVersionVo
    id: string
    name: string
    versionId: string
    versionName: string
    versionAlias?: string
    versionTag?: string
    /** @format int32 */
    shared: number
    /** @format int64 */
    createdTime: number
    files?: IFlattenFileVo[]
    versionMeta: string
}

export interface IResponseMessageDatasetInfoVo {
    code: string
    message: string
    /** SWDataset information object */
    data: IDatasetInfoVo
}

export interface IPageInfoDatasetVersionVo {
    /** @format int64 */
    total?: number
    list?: IDatasetVersionVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoDatasetVersionVo {
    code: string
    message: string
    data: IPageInfoDatasetVersionVo
}

export interface IBuildRecordVo {
    id: string
    projectId: string
    taskId: string
    datasetName: string
    status:
        | 'CREATED'
        | 'READY'
        | 'ASSIGNING'
        | 'PAUSED'
        | 'PREPARING'
        | 'RUNNING'
        | 'RETRYING'
        | 'SUCCESS'
        | 'CANCELLING'
        | 'CANCELED'
        | 'FAIL'
        | 'UNKNOWN'
    type: 'IMAGE' | 'VIDEO' | 'AUDIO'
    /** @format int64 */
    createTime: number
}

export interface IPageInfoBuildRecordVo {
    /** @format int64 */
    total?: number
    list?: IBuildRecordVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoBuildRecordVo {
    code: string
    message: string
    data: IPageInfoBuildRecordVo
}

export interface IPageInfoSftSpaceVo {
    /** @format int64 */
    total?: number
    list?: ISftSpaceVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoSftSpaceVo {
    code: string
    message: string
    data: IPageInfoSftSpaceVo
}

export interface ISftSpaceVo {
    /** @format int64 */
    id?: number
    name?: string
    description?: string
    /** @format int64 */
    createdTime: number
    /** User object */
    owner: IUserVo
}

export type IDsInfo = object

export type IModelInfo = object

export interface IPageInfoSftVo {
    /** @format int64 */
    total?: number
    list?: ISftVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IResponseMessagePageInfoSftVo {
    code: string
    message: string
    data: IPageInfoSftVo
}

export interface ISftVo {
    /** @format int64 */
    id?: number
    /** @format int64 */
    jobId?: number
    status?: 'CREATED' | 'READY' | 'PAUSED' | 'RUNNING' | 'CANCELLING' | 'CANCELED' | 'SUCCESS' | 'FAIL' | 'UNKNOWN'
    /** @format int64 */
    startTime?: number
    /** @format int64 */
    endTime?: number
    trainDatasets?: IDsInfo[]
    evalDatasets?: IDsInfo[]
    baseModel?: IModelInfo
    targetModel?: IModelInfo
}

/**
 * Model Serving Status
 * Model Serving Status object
 */
export interface IModelServingStatusVo {
    /** @format int32 */
    progress?: number
    events?: string
}

export interface IResponseMessageModelServingStatusVo {
    code: string
    message: string
    /** Model Serving Status object */
    data: IModelServingStatusVo
}

export interface IPageInfoPanelPluginVo {
    /** @format int64 */
    total?: number
    list?: IPanelPluginVo[]
    /** @format int32 */
    pageNum?: number
    /** @format int32 */
    pageSize?: number
    /** @format int32 */
    size?: number
    /** @format int64 */
    startRow?: number
    /** @format int64 */
    endRow?: number
    /** @format int32 */
    pages?: number
    /** @format int32 */
    prePage?: number
    /** @format int32 */
    nextPage?: number
    isFirstPage?: boolean
    isLastPage?: boolean
    hasPreviousPage?: boolean
    hasNextPage?: boolean
    /** @format int32 */
    navigatePages?: number
    navigatepageNums?: number[]
    /** @format int32 */
    navigateFirstPage?: number
    /** @format int32 */
    navigateLastPage?: number
}

export interface IPanelPluginVo {
    id: string
    name: string
    version: string
}

export interface IResponseMessagePageInfoPanelPluginVo {
    code: string
    message: string
    data: IPageInfoPanelPluginVo
}

export interface IResponseMessageRuntimeSuggestionVo {
    code: string
    message: string
    /** Model Serving object */
    data: IRuntimeSuggestionVo
}

/**
 * Model Serving
 * Model Serving object
 */
export interface IRuntimeSuggestionVo {
    runtimes?: IRuntimeVersionVo[]
}

export interface IUserRoleDeleteRequest {
    currentUserPwd: string
}

export interface IFileDeleteRequest {
    pathPrefix: string
    /** @uniqueItems true */
    files: string[]
}

export type IUpdateUserStateData = IResponseMessageString['data']

export type IUpdateUserPwdData = IResponseMessageString['data']

export type IUpdateCurrentUserPasswordData = IResponseMessageString['data']

export type ICheckCurrentUserPasswordData = IResponseMessageString['data']

export type IUpdateUserSystemRoleData = IResponseMessageString['data']

export type IDeleteUserSystemRoleData = IResponseMessageString['data']

export type IGetProjectByUrlData = IResponseMessageProjectVo['data']

export type IUpdateProjectData = IResponseMessageString['data']

export type IDeleteProjectByUrlData = IResponseMessageString['data']

export type IRecoverTrashData = IResponseMessageString['data']

export type IDeleteTrashData = IResponseMessageString['data']

export type IUpdateRuntimeData = IResponseMessageObject['data']

export type IModifyRuntimeData = IResponseMessageString['data']

export type IShareRuntimeVersionData = IResponseMessageString['data']

export type IRecoverRuntimeData = IResponseMessageString['data']

export type IModifyProjectRoleData = IResponseMessageString['data']

export type IDeleteProjectRoleData = IResponseMessageString['data']

export type IGetReportData = IResponseMessageReportVo['data']

export type IModifyReportData = IResponseMessageString['data']

export type IDeleteReportData = IResponseMessageString['data']

export type ISharedReportData = IResponseMessageString['data']

export type IModifyModelData = IResponseMessageString['data']

export type IHeadModelData = object

export type IShareModelVersionData = IResponseMessageString['data']

export type IRecoverModelData = IResponseMessageString['data']

export type IFindJobData = IResponseMessageJobVo['data']

export type IModifyJobCommentData = IResponseMessageString['data']

export type IRemoveJobData = IResponseMessageString['data']

export type IShareDatasetVersionData = IResponseMessageString['data']

export type IRecoverDatasetData = IResponseMessageString['data']

export type IUpdateSftSpaceData = IResponseMessageString['data']

export type IRecoverProjectData = IResponseMessageString['data']

export type IApplySignedGetUrlsData = IResponseMessageSignedUrlResponse['data']

export type IApplySignedPutUrlsData = IResponseMessageSignedUrlResponse['data']

export type IListUserData = IResponseMessagePageInfoUserVo['data']

export type ICreateUserData = IResponseMessageString['data']

export type IInstanceStatusData = IResponseMessageString['data']

export type IQuerySettingData = IResponseMessageString['data']

export type IUpdateSettingData = IResponseMessageString['data']

export type IListResourcePoolsData = IResponseMessageListResourcePool['data']

export type IUpdateResourcePoolsData = IResponseMessageString['data']

export type IListSystemRolesData = IResponseMessageListProjectMemberVo['data']

export type IAddUserSystemRoleData = IResponseMessageString['data']

export type IListProjectData = IResponseMessagePageInfoProjectVo['data']

export type ICreateProjectData = IResponseMessageString['data']

export type ICreateModelVersionData = any

export type ISelectAllInProjectData = IResponseMessageListJobTemplateVo['data']

export type IAddTemplateData = IResponseMessageString['data']

export type ICreateModelServingData = IResponseMessageModelServingVo['data']

export type IListRuntimeVersionTagsData = IResponseMessageListString['data']

export type IAddRuntimeVersionTagData = IResponseMessageString['data']

export type IBuildRuntimeImageData = IResponseMessageBuildImageResult['data']

export type IRevertRuntimeVersionData = IResponseMessageString['data']

export type IUploadData = IResponseMessageString['data']

export type IListProjectRoleData = IResponseMessageListProjectMemberVo['data']

export type IAddProjectRoleData = IResponseMessageString['data']

export type IListReportsData = IResponseMessagePageInfoReportVo['data']

export type ICreateReportData = IResponseMessageString['data']

export type ITransferData = IResponseMessageString['data']

export type IListModelVersionTagsData = IResponseMessageListString['data']

export type IAddModelVersionTagData = IResponseMessageString['data']

export type IRevertModelVersionData = IResponseMessageString['data']

export type IListJobsData = IResponseMessagePageInfoJobVo['data']

export type ICreateJobData = IResponseMessageString['data']

export type IActionData = IResponseMessageString['data']

export type IExecData = IResponseMessageExecResponse['data']

export type IRecoverJobData = IResponseMessageString['data']

export type IModifyJobPinStatusData = IResponseMessageString['data']

export type IGetEventsData = IResponseMessageListEventVo['data']

export type IAddEventData = IResponseMessageString['data']

export type ISignLinksData = IResponseMessageMapStringString['data']

export type IGetHashedBlobData = any

export type IUploadHashedBlobData = IResponseMessageString['data']

export type IHeadHashedBlobData = object

export type IGetViewConfigData = IResponseMessageConfigVo['data']

export type ICreateViewConfigData = IResponseMessageString['data']

export type IListDatasetVersionTagsData = IResponseMessageListString['data']

export type IAddDatasetVersionTagData = IResponseMessageString['data']

export type IConsumeNextDataData = INullableResponseMessageDataIndexDesc

export type IRevertDatasetVersionData = IResponseMessageString['data']

export type IUploadDsData = IResponseMessageUploadResult['data']

export type IBuildDatasetData = IResponseMessageString['data']

export type ISignLinks1Data = IResponseMessageMapObjectObject['data']

export type IGetHashedBlob1Data = any

export type IUploadHashedBlob1Data = IResponseMessageString['data']

export type IHeadHashedBlob1Data = object

export type IListSftSpaceData = IResponseMessagePageInfoSftSpaceVo['data']

export type ICreateSftSpaceData = IResponseMessageString['data']

export type ICreateSftData = IResponseMessageString['data']

export type IGetPanelSettingData = IResponseMessageString['data']

export type ISetPanelSettingData = IResponseMessageString['data']

export type IPluginListData = IResponseMessagePageInfoPanelPluginVo['data']

export type IInstallPluginData = IResponseMessageString['data']

export type ISignLinks2Data = IResponseMessageMapStringString['data']

export type IUpdateTableData = IResponseMessageString['data']

export type IScanTableData = IResponseMessageRecordListVo['data']

export type IScanAndExportData = any

export type IQueryTableData = IResponseMessageRecordListVo['data']

export type IQueryAndExportData = any

export type IListTablesData = IResponseMessageTableNameListVo['data']

export type IFlushData = IResponseMessageString['data']

export type IInitUploadBlobData = IResponseMessageInitUploadBlobResult['data']

export type ICompleteUploadBlobData = IResponseMessageCompleteUploadBlobResult['data']

export type IHeadRuntimeData = object

export type IHeadDatasetData = object

export type IGetUserByIdData = IResponseMessageUserVo['data']

export type IUserTokenData = IResponseMessageString['data']

export type IGetCurrentUserData = IResponseMessageUserVo['data']

export type IGetCurrentUserRolesData = IResponseMessageListProjectMemberVo['data']

export type IGetCurrentVersionData = IResponseMessageSystemVersionVo['data']

export type IQueryFeaturesData = IResponseMessageFeaturesVo['data']

export type IListDeviceData = IResponseMessageListDeviceVo['data']

export type IListRolesData = IResponseMessageListRoleVo['data']

export type IPreviewData = IResponseMessageReportVo['data']

export type IGetModelMetaBlobData = IResponseMessageString['data']

export type IListFilesData = IResponseMessageListFilesResult['data']

/** @format binary */
export type IGetFileDataData = File

export type IListTrashData = IResponseMessagePageInfoTrashVo['data']

export type IGetTemplateData = IResponseMessageJobTemplateVo['data']

export type IDeleteTemplateData = IResponseMessageString['data']

export type IListRuntimeData = IResponseMessagePageInfoRuntimeVo['data']

export type IGetRuntimeInfoData = IResponseMessageRuntimeInfoVo['data']

export type IDeleteRuntimeData = IResponseMessageString['data']

export type IListRuntimeVersionData = IResponseMessagePageInfoRuntimeVersionVo['data']

export type IPullData = any

export type IGetRuntimeVersionTagData = IResponseMessageLong['data']

export type IListRuntimeTreeData = IResponseMessageListRuntimeViewVo['data']

export type ISelectRecentlyInProjectData = IResponseMessageListJobTemplateVo['data']

export type IRecentRuntimeTreeData = IResponseMessageListRuntimeViewVo['data']

export type IRecentModelTreeData = IResponseMessageListModelViewVo['data']

export type IRecentDatasetTreeData = IResponseMessageListDatasetViewVo['data']

export type IGetProjectReadmeByUrlData = IResponseMessageString['data']

export type IListModelData = IResponseMessagePageInfoModelVo['data']

export type IGetModelInfoData = IResponseMessageModelInfoVo['data']

export type IDeleteModelData = IResponseMessageString['data']

export type IListModelVersionData = IResponseMessagePageInfoModelVersionVo['data']

export type IGetModelVersionTagData = IResponseMessageLong['data']

export type IGetModelDiffData = IResponseMessageMapStringListFileNode['data']

export type IListModelTreeData = IResponseMessageListModelViewVo['data']

export type IListTasksData = IResponseMessagePageInfoTaskVo['data']

export type IGetTaskData = IResponseMessageTaskVo['data']

export type IGetRunsData = IResponseMessageListRunVo['data']

export type IGetJobDagData = IResponseMessageGraph['data']

export type IListEvaluationSummaryData = IResponseMessagePageInfoSummaryVo['data']

export type IListAttributesData = IResponseMessageListAttributeVo['data']

export type IListDatasetData = IResponseMessagePageInfoDatasetVo['data']

export type IGetDatasetInfoData = IResponseMessageDatasetInfoVo['data']

export type IDeleteDatasetData = IResponseMessageString['data']

export type IListDatasetVersionData = IResponseMessagePageInfoDatasetVersionVo['data']

export type IPullDsData = any

export type IGetDatasetVersionTagData = IResponseMessageLong['data']

export type IListBuildRecordsData = IResponseMessagePageInfoBuildRecordVo['data']

export type IListDatasetTreeData = IResponseMessageListDatasetViewVo['data']

export type IPullUriContentData = any

export type IListSftData = IResponseMessagePageInfoSftVo['data']

export type IGetModelServingStatusData = IResponseMessageModelServingStatusVo['data']

export type IOfflineLogsData = IResponseMessageListString['data']

export type ILogContentData = string

export type IGetRuntimeSuggestionData = IResponseMessageRuntimeSuggestionVo['data']

export type IApplyPathPrefixData = IResponseMessageString['data']

export type IPullUriContent1Data = any

export type IDeletePathData = IResponseMessageString['data']

export type IDeleteRuntimeVersionTagData = IResponseMessageString['data']

export type IDeleteModelVersionTagData = IResponseMessageString['data']

export type IDeleteDatasetVersionTagData = IResponseMessageString['data']

export type IUninstallPluginData = IResponseMessageString['data']
