create sequence t_artists_SEQ start with 1 increment by 50;
create sequence t_customers_SEQ start with 1 increment by 50;
create sequence t_items_SEQ start with 1 increment by 50;
create sequence t_publishers_SEQ start with 1 increment by 50;
create table t_artists
(
    created_date timestamp(6) with time zone not null,
    id           bigint                      not null,
    name         varchar(100)                not null,
    primary key (id)
);
create table t_customers
(
    created_date timestamp(6) with time zone not null,
    id           bigint                      not null,
    full_name    varchar(100)                not null,
    email        varchar(255)                not null,
    primary key (id)
);
create table t_items
(
    nb_of_pages      integer,
    price            numeric(38, 2)              not null,
    publication_date date,
    created_date     timestamp(6) with time zone not null,
    id               bigint                      not null,
    isbn             varchar(15),
    language         varchar(20) check ((language in ('ENGLISH','FRENCH','SPANISH','PORTUGUESE'))),
    DTYPE            varchar(31)                 not null check ((DTYPE in ('Item', 'CD', 'Book'))),
    genre            varchar(100),
    title            varchar(100)                not null,
    description      varchar(3000),
    music_company    varchar(255),
    primary key (id)
);
create table t_publishers
(
    created_date timestamp(6) with time zone not null,
    id           bigint                      not null,
    name         varchar(50)                 not null,
    primary key (id)
);
