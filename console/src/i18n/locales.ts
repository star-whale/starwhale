export interface ILocaleItem {
    en: string
    zh: string
}

const basic = {
    'resource.unit.cpu': {
        en: 'Core',
        zh: '核',
    },
    'resource.price.unit.hour': {
        en: 'per hour',
        zh: '小时',
    },
    'resource.price.unit.minute': {
        en: 'per minute',
        zh: '分钟',
    },
    'resource.price.unit.second': {
        en: 'per second',
        zh: '秒',
    },
    'remove.yes': {
        en: 'Yes',
        zh: '是',
    },
    'remove.no': {
        en: 'No',
        zh: '否',
    },
    'add': {
        en: 'Add',
        zh: '添加',
    },
    'en': {
        en: 'English',
        zh: '英文',
    },
    'zh': {
        en: 'Chinese',
        zh: '中文',
    },
    'docs': {
        en: 'Docs',
        zh: '文档',
    },
    'form.upload.error.exist': {
        en: 'File already exists',
        zh: '文件已存在',
    },
    'form.rule.min': {
        en: '{{0}} must be at least 3 characters',
        zh: '{{0}} 至少需要3个字符',
    },
}

const dataset = {
    'dataset.remove.confirm': {
        en: 'Confirm Remove Dataset?',
        zh: '确认删除数据集？',
    },
    'dataset.remove.success': {
        en: 'Remove Dataset Success',
        zh: '删除数据集成功',
    },
    'dataset.remove.button': {
        en: 'Remove',
        zh: '删除',
    },
    'dataset.overview.shared': {
        en: 'Shared',
        zh: '共享',
    },
    'dataset.overview.shared.success': {
        en: 'Update success',
        zh: '更新成功',
    },
    'dataset.overview.shared.fail': {
        en: 'Update Failed',
        zh: '更新失败',
    },
    'dataset.overview.shared.yes': {
        en: 'True',
        zh: '是',
    },
    'dataset.overview.shared.no': {
        en: 'False',
        zh: '否',
    },
    'dataset.selector.view': {
        en: 'View',
        zh: '查看',
    },
    'dataset.create': {
        en: 'Create Dataset',
        zh: '创建数据集',
    },
    'dataset.update': {
        en: 'Update Dataset',
        zh: '更新数据集',
    },
    'dataset.create.title': {
        en: 'Dataset Build Task',
        zh: '数据集构建任务',
    },
    'dataset.create.owner': {
        en: 'Owner',
        zh: '拥有者',
    },
    'dataset.create.files': {
        en: 'Dataset Files',
        zh: '数据集文件',
    },
    'dataset.create.type': {
        en: 'Dataset Type',
        zh: '数据集类型',
    },
    'dataset.create.build.desc': {
        en: '{{0}} Datasets are building',
        zh: '{{0}} 个数据集正在构建',
    },
    'dataset.create.upload.desc': {
        en: 'Drag or click to upload files',
        zh: '拖拽或点击上传文件',
    },
    'dataset.create.upload.total.desc': {
        en: '{{0}} files, total size {{1}}',
        zh: '{{0}} 个文件，共 {{1}}',
    },
    'dataset.create.upload.error.type': {
        zh: '文件主类型异常, 建议格式 image/video/audio/csv/json/jsonl .',
        en: 'File main type is invalid, suggest format image/video/audio/csv/json/jsonl.',
    },
    'dataset.create.upload.error.desc': {
        en: 'the following files have an invalid status: ',
        zh: '以下文件上传状态异常: ',
    },
    'dataset.create.upload.error.exist': {
        en: 'File already exists',
        zh: '文件已存在',
    },
    'dataset.create.upload.error.unknown': {
        en: 'File already exists',
        zh: '文件异常',
    },
    'dataset.create.upload.error.max': {
        en: 'File size exceeds the maximum limit',
        zh: '文件大小超过最大限制',
    },
    'dataset.create.upload.drag.desc': {
        en: "Drag 'n' drop some files here, or click to select files",
        zh: '拖拽文件到此处，或点击选择文件',
    },
}

const runtime = {
    'runtime.remove.confirm': {
        en: 'Confirm Remove Runtime?',
        zh: '确认删除运行时？',
    },
    'runtime.remove.success': {
        en: 'Remove Runtime Success',
        zh: '删除运行时成功',
    },
    'runtime.remove.button': {
        en: 'Remove',
        zh: '删除',
    },
    'runtime.selector.view': {
        en: 'View',
        zh: '查看',
    },
    'runtime.image.build': {
        en: 'Build Image',
        zh: '构建镜像',
    },
    'runtime.image.built': {
        en: 'Image Built',
        zh: '镜像已构建',
    },
    'runtime.image.builtin': {
        en: 'Built-in',
        zh: '内置',
    },
    'runtime.image.other': {
        en: 'Other',
        zh: '其他',
    },
}

const model = {
    'model.remove.confirm': {
        en: 'Confirm Remove Model?',
        zh: '确认删除模型？',
    },
    'model.remove.success': {
        en: 'Remove Model Success',
        zh: '删除模型成功',
    },
    'model.remove.button': {
        en: 'Remove',
        zh: '删除',
    },
    'model.viewer.compare': {
        en: 'Compare',
        zh: '对比',
    },
    'model.viewer.source': {
        en: 'Source',
        zh: '源',
    },
    'model.viewer.target': {
        en: 'Target',
        zh: '目标',
    },
    'model.selector.view': {
        en: 'View',
        zh: '查看',
    },
    'model.handler': {
        en: 'Handler',
        zh: 'Handler',
    },
    'model.selector.placeholder': {
        en: 'Select Model',
        zh: '选择模型',
    },
    'model.selector.version.placeholder': {
        en: 'Select Model Version',
        zh: '请选择模型版本',
    },
    'online eval': {
        en: 'Online Evaluation',
        zh: '在线评测',
    },
    'online eval.advance': {
        en: 'Advance',
        zh: '高级',
    },
    'online eval.resource amount.tooltip': {
        en: 'Leave it blank will use the default configuration',
        zh: '留空则使用默认配置',
    },
    'online eval.memory.placeholder': {
        en: 'e.g. 1.5G',
        zh: '例如：1.5G',
    },
}
const trash = {
    'trash.title': {
        en: 'Trash',
        zh: '回收站',
    },
    'trash.name': {
        en: 'Name',
        zh: '名称',
    },
    'trash.type': {
        en: 'Type',
        zh: '类型',
    },
    'trash.size': {
        en: 'Size',
        zh: '大小',
    },
    'trash.trashedTime': {
        en: 'Trash Date',
        zh: '删除日期',
    },
    'trash.updatedTime': {
        en: 'Modified Date',
        zh: '修改日期',
    },
    'trash.retentionTime': {
        en: 'Retention Date',
        zh: '保留日期',
    },
    'trash.trashedBy': {
        en: 'Trashed By',
        zh: '删除者',
    },
    'trash.remove.confirm': {
        en: 'Confirm Remove Trash?',
        zh: '确认删除？',
    },
    'trash.remove.success': {
        en: 'Remove Trash Success',
        zh: '删除成功',
    },
    'trash.remove.button': {
        en: 'Delete',
        zh: '删除',
    },
    'trash.restore.confirm': {
        en: 'Confirm Restore Trash?',
        zh: '确认恢复？',
    },
    'trash.restore.success': {
        en: 'Restore Trash Success',
        zh: '恢复成功',
    },
    'trash.restore.button': {
        en: 'Restore',
        zh: '恢复',
    },
}

const project = {
    'project.remove.title': {
        en: 'Type the Project Name to delete',
        zh: '请输入要删除的项目名称',
    },
    'project.remove.confirm.start': {
        en: 'Please type',
        zh: '请输入',
    },
    'project.remove.confirm.end': {
        en: 'to confirm',
        zh: '以确认',
    },
    'project.visit.orderby': {
        en: 'Sort by',
        zh: '排序',
    },
    'project.visit.visited': {
        en: 'Visited',
        zh: '访问历史',
    },
    'project.visit.latest': {
        en: 'Latest',
        zh: '最新的',
    },
    'project.visit.oldest': {
        en: 'Oldest',
        zh: '最老的',
    },
}

const job = {
    'job.status.created': {
        en: 'Created',
        zh: '已创建',
    },
    'job.status.paused': {
        en: 'Paused',
        zh: '已暂停',
    },
    'job.status.succeeded': {
        en: 'Succeeded',
        zh: '已成功',
    },
    'job.status.running': {
        en: 'Running',
        zh: '运行中',
    },
    'job.status.cancelling': {
        en: 'Cancelling',
        zh: '取消中',
    },
    'job.status.cancelled': {
        en: 'Cancelled',
        zh: '已取消',
    },
    'job.status.fail': {
        en: 'Failed',
        zh: '失败',
    },
    'job.status.ready': {
        en: 'Ready',
        zh: '就绪',
    },
    'job.status.selector.placeholder': {
        en: 'Select Job Status',
        zh: '选择任务状态',
    },
    'job.advanced': {
        en: 'Advanced',
        zh: '高级配置',
    },
    'job.debug.mode': {
        en: 'Debug Mode',
        zh: '调试模式',
    },
    'job.debug.password': {
        en: 'Debug Password',
        zh: '调试密码',
    },
    'job.debug.generate': {
        en: 'Generate',
        zh: '生成密码',
    },
    'job.debug.notice': {
        en: 'Debug mode is on, please remember the debug password for later debugging. You can also change it to your own password.',
        zh: '调试模式已开启，请记住您的调试密码，以便后续调试。您也可以修改为自己的密码。',
    },
    'job.autorelease.notice': {
        en: 'Auto release is on, to prevent unnecessary cost, the service will be terminated automatically under the release time condition.',
        zh: '自动释放已开启，为了预防多余的费用支出，服务将在到达释放时间 后自动终止。',
    },
    'job.autorelease.toggle': {
        en: 'Auto Release',
        zh: '自动释放',
    },
    'job.autorelease.time': {
        en: 'Auto Release Time',
        zh: '自动释放时间',
    },
    'job.task.executor': {
        en: 'Execute Cmd In Task Container',
        zh: '在任务容器中执行命令',
    },
    'job.pin': {
        en: 'Pin the job',
        zh: '置顶',
    },
    'job.unpin': {
        en: 'Unpin the job',
        zh: '取消置顶',
    },
    'job.expose.title': {
        en: 'Open Expose Service',
        zh: '开启外部访问',
    },
}

const evaluation = {
    'evaluation.title': {
        en: 'Evaluation',
        zh: '评测',
    },
    'evaluation.save.success': {
        en: 'Save Evaluation Success',
        zh: '保存成功',
    },
    'compare.title': {
        en: 'Compare',
        zh: '评测对比',
    },
    'compare.column.metrics': {
        en: 'Metrics',
        zh: '指标',
    },
    'compare.config.show.changes': {
        en: 'Show cell changes',
        zh: '内容对比',
    },
    'compare.config.show.diff': {
        en: 'Rows with diff only',
        zh: '只显示不同内容',
    },
    'compare.show.details': {
        en: 'Show Details',
        zh: '显示详情',
    },
    'evaluation.detail.title': {
        en: 'Evaluation Detail',
        zh: '评测详情',
    },
    'evaluation.detail.compare': {
        en: 'Compare',
        zh: '评测对比',
    },
    'evalution.result.title': {
        en: 'Result Details',
        zh: '结果详情',
    },
}

const table = {
    'table.column.manage': {
        en: 'Manage Columns',
        zh: '列管理',
    },
    'table.column.invisible': {
        en: 'Invisible Columns',
        zh: '隐藏列',
    },
    'table.column.visible': {
        en: 'Visible Columns',
        zh: '显示列',
    },
    'table.view.add': {
        en: 'Add View',
        zh: '添加视图',
    },
    'table.view.manage': {
        en: 'Manage Views',
        zh: '管理视图',
    },
    'table.view.name': {
        en: 'View Name',
        zh: '视图名称',
    },
    'table.view.name.exsts': {
        en: 'View Name Exists',
        zh: '视图名称已存在',
    },
    'table.view.name.placeholder': {
        en: 'Input text',
        zh: '请输入',
    },
    'table.filter.add': {
        en: 'Add Filter',
        zh: '添加过滤器',
    },
    'table.sort.by': {
        en: 'Sort',
        zh: '排序',
    },
    'table.sort.placeholder': {
        en: 'Select',
        zh: '请选择',
    },
    'table.sort.asc': {
        en: 'Ascending',
        zh: '升序',
    },
    'table.sort.desc': {
        en: 'Descending',
        zh: '降序',
    },
    'table.search.placeholder': {
        en: 'Search and Filter',
        zh: '搜索和过滤',
    },
    'table.config.query.simple': {
        en: 'Simple Query',
        zh: '简单查询',
    },
    'table.config.query.advanced': {
        en: 'Advanced Query',
        zh: '高级查询',
    },
}

const ui = {
    'selector.placeholder': {
        en: 'Select...',
        zh: '请选择',
    },
    'log.resume': {
        en: 'Resume',
        zh: '继续',
    },
    'log.pause': {
        en: 'Pause',
        zh: '暂停',
    },
    'log.behind.lines': {
        en: 'and show {{0}} lines',
        zh: '并显示 {{0}} 行',
    },
}

const widget = {
    'panel.name': {
        en: 'Panel',
        zh: '面板',
    },
    'panel.save': {
        en: 'Save',
        zh: '保存',
    },
    'panel.add': {
        en: 'Add Panel',
        zh: '添加面板',
    },
    'panel.rename': {
        en: 'Rename',
        zh: '重命名',
    },
    'panel.delete': {
        en: 'Delete',
        zh: '删除',
    },
    'panel.add.placeholder': {
        en: 'Select a metric to visualize in this chart',
        zh: '选择图表展示',
    },
    'panel.add.above': {
        en: 'Add Panel Above',
        zh: '在上方添加新面板',
    },
    'panel.add.below': {
        en: 'Add Panel Below',
        zh: '在下方添加新面板',
    },
    'panel.save.success': {
        en: 'Panel setting saved',
        zh: '保存成功',
    },
    'panel.chart.add': {
        en: 'Add Chart',
        zh: '添加图表',
    },
    'panel.chart.title': {
        en: 'Chart Title',
        zh: '图表标题',
    },
    'panel.chart.delete': {
        en: 'Delete Chart',
        zh: '删除图表',
    },
    'panel.chart.edit': {
        en: 'Edit Chart',
        zh: '编辑图表',
    },
    'panel.chart.type': {
        en: 'Chart Type',
        zh: '图表类型',
    },
    'panel.chart.table.name': {
        en: 'Table Name',
        zh: '数据表',
    },
    'panel.list.placeholder': {
        en: 'Click "Add Chart" to add visualizations',
        zh: '点击 "添加图表" 添加可视化图表',
    },
    'panel.view.config.custom': {
        en: 'Custom',
        zh: '自定义',
    },
    'panel.view.config.model-buildin': {
        en: 'Model Build-in',
        zh: '模型内置配置',
    },
}

const locales0 = {
    'create sth': {
        en: 'Create {{0}}',
        zh: '创建{{0}}',
    },
    'edit sth': {
        en: 'Edit {{0}}',
        zh: '编辑{{0}}',
    },
    'delete sth': {
        en: 'Delete {{0}}',
        zh: '删除{{0}}',
    },
    'select sth': {
        en: 'Select {{0}}',
        zh: '选择{{0}}',
    },
    'sth name': {
        en: '{{0}} Name',
        zh: '{{0}}名称',
    },
    'sth required': {
        en: '{{0}} required',
        zh: '{{0}}必填',
    },
    'Model': {
        en: 'Model',
        zh: '模型',
    },
    'Model Version': {
        en: 'Model Version',
        zh: '模型版本',
    },
    'model versions': {
        en: 'Model Versions',
        zh: '模型版本',
    },
    'Models': {
        en: 'Models',
        zh: '模型',
    },
    'Model Information': {
        en: 'Model Information',
        zh: '模型信息',
    },
    'Model ID': {
        en: 'Model ID',
        zh: '模型ID',
    },
    'Dataset': {
        en: 'Dataset',
        zh: '数据集',
    },
    'Dataset Version': {
        en: 'Dataset Version',
        zh: '数据集版本',
    },
    'dataset versions': {
        en: 'Dataset Versions',
        zh: '数据集版本',
    },
    'Alias': {
        en: 'Alias',
        zh: '别名',
    },
    'Shared': {
        en: 'Shared',
        zh: '共享',
    },
    'datasets': {
        en: 'Datasets',
        zh: '数据集',
    },
    'Datasets': {
        en: 'Datasets',
        zh: '数据集',
    },
    'Job': {
        en: 'Job',
        zh: '作业',
    },
    'Job ID': {
        en: 'Job ID',
        zh: '作业ID',
    },
    'Run Model': {
        en: 'Run Model',
        zh: '运行模型',
    },
    'Task': {
        en: 'Task',
        zh: '任务',
    },
    'tasks': {
        en: 'tasks',
        zh: '任务',
    },
    'View Tasks': {
        en: 'View Tasks',
        zh: '查看任务',
    },
    'Jobs': {
        en: 'Jobs',
        zh: '作业',
    },
    'PROJECT': {
        en: 'PROJECT',
        zh: '项目',
    },
    'projects': {
        en: 'Projects',
        zh: '项目',
    },
    'Project': {
        en: 'Project',
        zh: '项目',
    },
    'Create Project': {
        en: 'Create Project',
        zh: '创建项目',
    },
    'Private': {
        en: 'Private',
        zh: '私有',
    },
    'Private Project Desc': {
        en: 'You can choose who can maintain and browse.',
        zh: '您可以选择谁可以维护和浏览。',
    },
    'Public': {
        en: 'Public',
        zh: '公共',
    },
    'Public Project Desc': {
        en: 'Anyone on the internet can see this project.',
        zh: '互联网上的任何人都可以看到这个项目。',
    },
    'Description': {
        en: 'Description',
        zh: '描述',
    },
    'Project List': {
        en: 'Project List',
        zh: '项目列表',
    },
    'Project Name': {
        en: 'Project Name',
        zh: '项目名称',
    },
    'project created': {
        en: 'project created',
        zh: '项目已创建',
    },
    'Manage Project Member': {
        en: 'Manage Member',
        zh: '管理成员',
    },
    'Project Role': {
        en: 'Project Role',
        zh: '项目角色',
    },
    'Onwer': {
        en: 'Onwer',
        zh: '所有者',
    },
    'Maintainer': {
        en: 'Maintainer',
        zh: '维护者',
    },
    'Guest': {
        en: 'Guest',
        zh: '访客',
    },
    'USER': {
        en: 'USER',
        zh: '用户',
    },
    'User': {
        en: 'User',
        zh: '用户',
    },
    'Admin Settings': {
        en: 'Admin Settings',
        zh: '管理员设置',
    },
    'Manage Users': {
        en: 'Manage Users',
        zh: '管理用户',
    },
    'Manage Project Members': {
        en: 'Manage Member',
        zh: '管理成员',
    },
    'Add Project Member': {
        en: 'Add Member',
        zh: '添加成员',
    },
    'Remove Project Member': {
        en: 'Remove',
        zh: '移除',
    },
    'Change project role success': {
        en: 'Change project role success',
        zh: '更改项目角色成功',
    },
    'Add project role success': {
        en: 'Add project role success',
        zh: '添加项目角色成功',
    },
    'Remove Project Role Confirm': {
        en: 'Are you sure to remove the user?',
        zh: '您确定要移除该用户吗？',
    },
    'Remove Project Role Success': {
        en: 'Remove project role success',
        zh: '移除项目角色成功',
    },
    'Add User': {
        en: 'Add User',
        zh: '添加用户',
    },
    'Add User Success': {
        en: 'Add User Success',
        zh: '添加用户成功',
    },
    'Update User Success': {
        en: 'Update User Success',
        zh: '更新用户成功',
    },
    'Random Password Tips For Add': {
        en: 'The user has been added successfully. You can copy the random password below and send it to the user',
        zh: '用户已成功添加。您可以复制下面的随机密码并发送给用户。',
    },
    'Random Password Tips For Update': {
        en: 'The user password has been updated successfully. You can copy the random password below and send it to the user',
        zh: '用户密码已成功更新。您可以复制下面的随机密码并发送给用户。',
    },
    'Copy To Clipboard': {
        en: 'copy',
        zh: '复制',
    },
    'Copied': {
        en: 'Copied !',
        zh: '已复制！',
    },
    'Copy': {
        en: 'Copy',
        zh: '复制',
    },
    'Copy Link': {
        en: 'Copy Link',
        zh: '复制链接',
    },
    'User List': {
        en: 'User List',
        zh: '用户列表',
    },
    'Disable User': {
        en: 'Disable',
        zh: '禁用',
    },
    'Enable User': {
        en: 'Enable',
        zh: '启用',
    },
    'Disable User Success': {
        en: 'Disable User Success',
        zh: '禁用用户成功',
    },
    'Disable User Confirm': {
        en: 'Are you sure to disable the user?',
        zh: '确定要禁用该用户吗？',
    },
    'Enable User Success': {
        en: 'Enable User Success',
        zh: '启用成功',
    },
    'Enable User Confirm': {
        en: 'Are you sure to enable the user?',
        zh: '确定要启用该用户吗？',
    },
    'Disabled User': {
        en: 'Disabled',
        zh: '已禁用',
    },
    'Enabled User': {
        en: 'Enabled',
        zh: '已启用',
    },
    'Username': {
        en: 'Username',
        zh: '用户名',
    },
    'Create Your Account': {
        en: 'Create your account',
        zh: '创建您的账户',
    },
    'Account Name': {
        en: 'Account Name',
        zh: '账户名称',
    },
    'Send Email': {
        en: 'Send Email',
        zh: '发送邮件',
    },
    'Send Reset Password Email Form title': {
        en: 'Please enter your email address',
        zh: '请输入您的电子邮件地址',
    },
    'Reset Your Password': {
        en: 'Reset Your Password',
        zh: '重置密码',
    },
    'LOGIN': {
        en: 'Log in',
        zh: '登录',
    },
    'Created': {
        en: 'Created',
        zh: '创建时间',
    },
    'Finished': {
        en: 'Finished',
        zh: '完成时间',
    },
    'Owner': {
        en: 'Owner',
        zh: '操作者',
    },
    'File': {
        en: 'File',
        zh: '文件',
    },
    'Size': {
        en: 'Size',
        zh: '大小',
    },
    'Import Path': {
        en: 'Import Path',
        zh: '导入路径',
    },
    'Import from server': {
        en: 'Import from server',
        zh: '从服务器导入',
    },
    'Upload': {
        en: 'Upload',
        zh: '上传',
    },
    'Version History': {
        en: 'Version History',
        zh: '版本历史',
    },
    'Version': {
        en: 'Version',
        zh: '版本',
    },
    'Action': {
        en: 'Action',
        zh: '操作',
    },
    'Tag': {
        en: 'Tag',
        zh: '标签',
    },
    'Revert': {
        en: 'Revert',
        zh: '还原',
    },
    'Environment': {
        en: 'Environment',
        zh: '环境变量',
    },
    'BaseImage': {
        en: 'Base Image',
        zh: '基础镜像',
    },
    'Resource': {
        en: 'Resource',
        zh: '资源',
    },
    'Add Resource': {
        en: 'Add Resource',
        zh: '添加资源',
    },
    'Selected Dataset': {
        en: 'Selected Dataset',
        zh: '已选择的数据集',
    },
    'Result Output Path': {
        en: 'Result Output Path',
        zh: '输出结果路径',
    },
    'Resource Amount': {
        en: 'Quantity',
        zh: '数量',
    },
    'Resource Pool': {
        en: 'Resource Pool',
        zh: '资源池',
    },
    'Runtime': {
        en: 'Runtime',
        zh: '运行时',
    },
    'Runtime Type': {
        en: 'Runtime Type',
        zh: '类型',
    },
    'End Time': {
        en: 'End Time',
        zh: '结束时间',
    },
    'Duration': {
        en: 'Duration',
        zh: '持续时间',
    },
    'Status': {
        en: 'Status',
        zh: '状态',
    },
    'Status Desc': {
        en: 'Description',
        zh: '状态信息',
    },
    'Continue': {
        en: 'Continue',
        zh: '继续',
    },
    'Cancel': {
        en: 'Cancel',
        zh: '取消',
    },
    'Cancel.Confirm': {
        en: 'Are you sure to cancel the job?',
        zh: '确定要取消该任务吗？',
    },
    'Confirm': {
        en: 'Confirm',
        zh: '确认',
    },
    'Suspend': {
        en: 'Suspend',
        zh: '暂停',
    },
    'View Results': {
        en: 'View Results',
        zh: '查看结果',
    },
    'job action done': {
        en: 'job action done',
        zh: '任务操作完成',
    },
    'Task ID': {
        en: 'Task ID',
        zh: '任务标识',
    },
    'IP': {
        en: 'IP',
        zh: 'IP地址',
    },
    'Started': {
        en: 'Started',
        zh: '已开始',
    },
    'Debug': {
        en: 'Debug',
        zh: '调试',
    },
    'To Debug': {
        en: 'To Debug',
        zh: '去调试',
    },
    'Password': {
        en: 'Password',
        zh: '密码',
    },
    'Password Changed': {
        en: 'Password Changed',
        zh: '密码已更改',
    },
    'New Password': {
        en: 'New Password',
        zh: '新密码',
    },
    'Password Too Short': {
        en: 'Password too short',
        zh: '密码太短',
    },
    'Current Password': {
        en: 'Current Password',
        zh: '当前密码',
    },
    'Your Password': {
        en: 'Your Password',
        zh: '您的密码',
    },
    'Validate Password': {
        en: 'validate',
        zh: '验证密码',
    },
    'Confirm New Password': {
        en: 'Confirm New Password',
        zh: '确认新密码',
    },
    'Enter your password': {
        en: 'Enter your password',
        zh: '请输入密码',
    },
    '(reset from) Enter your password For': {
        en: 'Enter a new password for',
        zh: '为 {{0}} 输入新密码',
    },
    'Use random password': {
        en: 'Use random password',
        zh: '使用随机密码',
    },
    'Logout': {
        en: 'Logout',
        zh: '登出',
    },
    'Change Password': {
        en: 'Change Password',
        zh: '修改密码',
    },
    'Get Token': {
        en: 'Get Token',
        zh: '获取令牌',
    },
    'Password Not Equal': {
        en: 'The passwords you entered do not match',
        zh: '两次输入的密码不一致',
    },
    'submit': {
        en: 'Submit',
        zh: '提交',
    },
    'create': {
        en: 'Create',
        zh: '创建',
    },
    'login': {
        en: 'Log in',
        zh: '登录',
    },
    'overview': {
        en: 'Overview',
        zh: '总览',
    },
    'Overview': {
        en: 'Overview',
        zh: '总览',
    },
    'TP': {
        en: 'tp',
        zh: '真正例',
    },
    'TN': {
        en: 'tn',
        zh: '真负例',
    },
    'FP': {
        en: 'fp',
        zh: '假正例',
    },
    'FN': {
        en: 'fn',
        zh: '假负例',
    },
    'Accuracy': {
        en: 'Accuracy',
        zh: '准确率',
    },
    'Precision': {
        en: 'Precision',
        zh: '精确率',
    },
    'Recall': {
        en: 'Recall',
        zh: '召回率',
    },
    'Label': {
        en: 'Label',
        zh: '标签',
    },
    'Meta': {
        en: 'Meta',
        zh: '元数据',
    },
    'show meta': {
        en: 'show meta',
        zh: '显示元数据',
    },
    'View Log': {
        en: 'View Logs',
        zh: '查看日志',
    },
    'Logs collected': {
        en: 'Logs collected',
        zh: '已收集日志',
    },
    'Execution id': {
        en: 'Execution id',
        zh: '执行标识',
    },
    'Support': {
        en: 'Support',
        zh: '支持',
    },
    'F1-score': {
        en: 'F1-score',
        zh: 'F1得分',
    },
    'Pause': {
        en: 'Pause',
        zh: '暂停',
    },
    'Pause.Confirm': {
        en: 'Are you sure to pause the job?',
        zh: '确定要暂停该任务吗？',
    },
    'Resume': {
        en: 'Resume',
        zh: '恢复',
    },
    'Tasks': {
        en: 'Tasks',
        zh: '任务',
    },
    'Results': {
        en: 'Results',
        zh: '结果',
    },
    'no logs found': {
        en: 'no logs found',
        zh: '没有找到日志',
    },
    'Summary': {
        en: 'Summary',
        zh: '摘要',
    },
    'Confusion Matrix': {
        en: 'Confusion Matrix',
        zh: '混淆矩阵',
    },
    'Roc Auc': {
        en: 'Roc Auc',
        zh: 'Roc曲线下面积',
    },
    'Labels': {
        en: 'Labels',
        zh: '标签',
    },
    'Version Meta': {
        en: 'Version Meta',
        zh: '版本元数据',
    },
    'Version Tag': {
        en: 'Version Tag',
        zh: '版本标签',
    },
    'Files': {
        en: 'Files',
        zh: '文件',
    },
    'Directory': {
        en: 'Directory',
        zh: '目录',
    },
    'Version ID': {
        en: 'Version ID',
        zh: '版本标识',
    },
    'Version Name': {
        en: 'Version Name',
        zh: '版本名称',
    },
    'model version reverted': {
        en: 'model version reverted',
        zh: '模型版本已还原',
    },
    'something wrong with the server': {
        en: 'something wrong with the server',
        zh: '服务器出错了',
    },
    'System Info': {
        en: 'System Info',
        zh: '系统信息',
    },
    'Base Images': {
        en: 'Base Images',
        zh: '基础镜像',
    },
    'Agent List': {
        en: 'Agent List',
        zh: '代理列表',
    },
    'SETTINGS': {
        en: 'SETTINGS',
        zh: '设置',
    },
    'Agent': {
        en: 'Agent',
        zh: '代理',
    },
    'System Version': {
        en: 'System Version',
        zh: '系统版本',
    },
    'Connected Time': {
        en: 'Connected Time',
        zh: '连接时间',
    },
    'UUID': {
        en: 'UUID',
        zh: '标识符',
    },
    'agent delete done': {
        en: 'agent delete done',
        zh: '代理已删除',
    },
    'base image delete done': {
        en: 'base image delete done',
        zh: '基础镜像已删除',
    },
    'Delete': {
        en: 'Delete',
        zh: '删除',
    },
    'Runtimes': {
        en: 'Runtimes',
        zh: '运行时',
    },
    'Runtime ID': {
        en: 'Runtime ID',
        zh: '运行时标识',
    },
    'Runtime Name': {
        en: 'Runtime Name',
        zh: '运行时名称',
    },
    'Runtime Version': {
        en: 'Runtime Version',
        zh: '运行时版本',
    },
    'runtime versions': {
        en: 'Runtime Versions',
        zh: '运行时版本',
    },
    'DAG': {
        en: 'Actions',
        zh: 'DAG',
    },
    'Evaluation': {
        en: 'Evaluation',
        zh: '评测',
    },
    'Evaluation ID': {
        en: 'Evaluation ID',
        zh: '评测标识',
    },
    'Evaluations': {
        en: 'Evaluations',
        zh: '评测',
    },
    'Compare Evaluations': {
        en: 'Compare Evaluations',
        zh: '比较',
    },
    // data table
    'Add a New View': {
        en: 'Add a New View',
        zh: '添加新视图',
    },
    'Edit View': {
        en: 'Edit View',
        zh: '编辑视图',
    },
    'Manage Views': {
        en: 'Manage Views',
        zh: '管理视图',
    },
    'Select a view': {
        en: 'Select a view',
        zh: '选择视图',
    },
    'All runs': {
        en: 'All runs',
        zh: '展示所有',
    },
    'Edit': {
        en: 'Edit',
        zh: '编辑',
    },
    'Other Login Methods': {
        en: 'Other Login Methods',
        zh: '其他方式登录',
    },
    'emailPlaceholder for login': {
        en: 'yours@example.com or your username',
        zh: '您的电子邮件或用户名',
    },
    'emailPlaceholder for sing up': {
        en: 'yours@example.com',
        zh: '您的电子邮件',
    },
    'passwordPlaceholder': {
        en: 'your password',
        zh: '您的密码',
    },
    'Do Not Remember Your Password': {
        en: "Don't remember your password?",
        zh: '忘记密码？',
    },
    'Reset Password Email Send Success': {
        en: 'You should receive an email shortly with further instructions.',
        zh: '您应该会很快收到一封电子邮件，其中包含进一步的说明。',
    },
    'Reset Password Success': {
        en: 'Reset password success!',
        zh: '密码重置成功！',
    },
    'logIn': {
        en: 'Log in',
        zh: '登录',
    },
    'signUp': {
        en: 'Sign up',
        zh: '注册',
    },
    'Sign Up With': {
        en: 'Sign up with {{0}}',
        zh: '{{0}}',
    },
    'Log In With': {
        en: 'Log in with {{0}}',
        zh: '{{0}} ',
    },
    'agreePolicy': {
        en: 'I agree to <1>Terms of Service</1> and <3>Privacy Policy</3>',
        zh: '我同意 <1>服务条款</1> 和 <3>隐私政策</3>',
    },
    'Should Check the ToS': {
        en: 'Please read and check the ToS',
        zh: '请阅读并勾选服务条款',
    },
    'User Not Registered': {
        en: 'User not registered',
        zh: '用户未注册',
    },
    'Check Your Email': {
        en: 'Check your email',
        zh: '请检查您的电子邮件',
    },
    'Please verify the email send from Starwhale': {
        en: 'Please verify the email send from Starwhale',
        zh: '请验证来自 Starwhale 的电子邮件',
    },
    'Resend Email': {
        en: 'Resend Email',
        zh: '重新发送电子邮件',
    },
    'Send Email Success': {
        en: 'Send email success',
        zh: '发送电子邮件成功',
    },
    'Already Verified': {
        en: 'Already Verified',
        zh: '已验证',
    },
    'alreadyHaveAccount': {
        en: 'Already have an account?',
        zh: '注册过了？',
    },
    'Manage Member': {
        en: 'Manage Member',
        zh: '管理成员',
    },
    'Privacy': {
        en: 'Privacy',
        zh: '隐私',
    },
    'Member': {
        en: 'Member',
        zh: '成员',
    },
    'Version and Files': {
        en: 'Version and Files',
        zh: '版本和文件',
    },
    'Metadata': {
        en: 'Metadata',
        zh: '元数据',
    },
    'Version Full Name': {
        en: 'Version Full Name',
        zh: '版本全称',
    },
    'Created At': {
        en: 'Created At',
        zh: '创建于',
    },
    'Aliases': {
        en: 'Aliases',
        zh: '别名',
    },
    'History': {
        en: 'History',
        zh: '历史记录',
    },
    'or': {
        en: 'or',
        zh: '或者',
    },
    'Members': {
        en: 'Members',
        zh: '成员',
    },
    'Signed in as': {
        en: 'Signed in as',
        zh: '登录为',
    },
    'runtime version reverted': {
        en: 'runtime version reverted',
        zh: '版本回退成功',
    },
    'dataset version reverted': {
        en: 'dataset version reverted',
        zh: '版本回退成功',
    },
    'Exit Fullscreen': {
        en: 'Exit Fullscreen',
        zh: '退出全屏模式',
    },
    'Remove Project Success': {
        en: 'Remove Project Success',
        zh: '移除项目成功',
    },
    'All the evaluations, datasets, models, and runtimes belong to the project will be removed.': {
        en: 'All the evaluations, datasets, models, and runtimes belong to the project will be removed.',
        zh: '该项目下的所有评测、数据集、模型和运行时都将被删除。',
    },
    'Image': {
        en: 'Image',
        zh: '镜像',
    },
    'Elapsed Time': {
        en: 'Elapsed Time',
        zh: '耗时',
    },
    'Step': {
        en: 'Step',
        zh: 'Step',
    },
    'Task Amount': {
        en: 'Replica',
        zh: '副本',
    },
    'Raw Type': {
        en: 'Edit',
        zh: '修改',
    },
    'eval debug mode': {
        en: 'Debug Mode',
        zh: '调试模式',
    },
    'wrong yaml syntax': {
        en: 'wrong yaml syntax',
        zh: 'YAML 语法错误',
    },
    'System Settings': {
        en: 'System Settings',
        zh: '系统设置',
    },
    'Update': {
        en: 'Update',
        zh: '更新',
    },
    'Reset': {
        en: 'Reset',
        zh: '重置',
    },
    'Update Setting Success': {
        en: 'Update Setting Success',
        zh: '更新设置成功',
    },
    'Pull resource to local with cli mate': {
        en: 'Push to local',
        zh: '拉取到本地',
    },
    'Github': {
        en: 'Github',
        zh: 'Github',
    },
    'Google': {
        en: 'Google',
        zh: 'Google',
    },
    ...basic,
    ...dataset,
    ...model,
    ...runtime,
    ...trash,
    ...project,
    ...job,
    ...table,
    ...evaluation,
    ...widget,
    ...ui,
}

// eslint-disable-next-line import/no-mutable-exports
export let locales: { [key in keyof typeof locales0]: ILocaleItem } = locales0

export function registerLocales(tmp: Record<string, ILocaleItem>) {
    locales = { ...tmp, ...locales0 }
}
