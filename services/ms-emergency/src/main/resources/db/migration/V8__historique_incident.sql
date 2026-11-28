create table historique_incident (

    id binary(16) not null primary key,

    incident_id binary(16) not null,

    action varchar(40) not null,

    ancien_statut varchar(30),

    nouveau_statut varchar(30),

    auteur varchar(100) not null,

    details varchar(2000),

    date_action datetime(6) not null,

    constraint fk_historique_incident
        foreign key (incident_id)
        references incident(id)
);

create index idx_historique_incident_date
on historique_incident(incident_id, date_action);