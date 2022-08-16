alter table swmp_version
    add eval_jobs text not null;
alter table step
    add concurrency int not null,
    add task_num int not null;
