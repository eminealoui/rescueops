alter table incident
    add column incident_principal_id binary(16),
    add column fusionne_par varchar(36),
    add column fusionne_le datetime(6),
    add column motif_fusion varchar(500);

alter table incident
    add constraint fk_incident_principal
    foreign key (incident_principal_id)
    references incident(id);

create index idx_incident_principal
on incident(incident_principal_id);