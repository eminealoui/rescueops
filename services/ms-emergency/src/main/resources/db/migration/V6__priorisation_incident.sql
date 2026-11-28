alter table incident
    add column gravite varchar(30),
    add column danger_immediat boolean,
    add column personnes_vulnerables boolean,
    add column score_priorite int,
    add column priorite varchar(30),
    add column priorite_suggeree varchar(30),
    add column explication_priorite varchar(1000),
    add column motif_ajustement_priorite varchar(500);

create index idx_incident_priorite
on incident(priorite);