create type merlin.merge_request_status as enum ('pending', 'in-progress','accepted', 'rejected', 'withdrawn');
create type merlin.activity_change_type as enum ('none', 'add', 'delete','modify');
create type merlin.conflict_resolution as enum ('none','supplying', 'receiving');
