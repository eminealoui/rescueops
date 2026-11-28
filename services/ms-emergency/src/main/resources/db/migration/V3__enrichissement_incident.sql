alter table incident
    add column titre varchar(150),
    add column type varchar(30),
    add column adresse varchar(255),
    add column latitude double,
    add column longitude double,
    add column nombre_victimes int;