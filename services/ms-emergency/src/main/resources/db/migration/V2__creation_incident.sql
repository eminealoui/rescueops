create table incident (
    id binary(16) not null primary key,
    description varchar(500) not null,
    statut varchar(30) not null,
    cree_par varchar(36) not null,
    cree_le datetime(6) not null,
    version bigint not null
);