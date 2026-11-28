alter table incident
    add column traite_par varchar(36),
    add column traite_le datetime(6),
    add column motif_rejet varchar(500),
    add column cloture_par varchar(36),
    add column cloture_le datetime(6);