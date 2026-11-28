create index idx_incident_statut
on incident(statut);

create index idx_incident_type
on incident(type);

create index idx_incident_cree_par
on incident(cree_par);

create index idx_incident_cree_le
on incident(cree_le);