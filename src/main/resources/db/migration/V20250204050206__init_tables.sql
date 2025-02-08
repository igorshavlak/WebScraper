create table image_entity
(
    id                     serial primary key,
    original_url           text unique not null,
    path                   text        not null,
    original_size          bigint      not null,
    size_after_compression bigint      not null
);