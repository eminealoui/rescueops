create table evenement_traite (

    event_id binary(16) not null primary key,

    event_type varchar(100) not null,

    date_traitement datetime(6) not null
);