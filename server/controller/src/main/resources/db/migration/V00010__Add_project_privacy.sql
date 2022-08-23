alter table project_info
    add privacy int not null default '0' after owner_id;
