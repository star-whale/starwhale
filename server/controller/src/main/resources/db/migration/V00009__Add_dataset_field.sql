alter table dataset_version
    add size bigint not null;
alter table dataset_version
    add index_table varchar(255) not null;
