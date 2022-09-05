alter table swmp_version
    add version_order bigint default 0 not null after id;
update swmp_version set version_order = id;
create index idx_version_order
    on swmp_version (version_order);

alter table dataset_version
    add version_order bigint default 0 not null after id;
update dataset_version set version_order = id;
create index idx_version_order
    on dataset_version (version_order);

alter table runtime_version
    add version_order bigint default 0 not null after id;
update runtime_version set version_order = id;
create index idx_version_order
    on runtime_version (version_order);



