----- NOTE: Test file, should not be used in real database migration

--- Database Change

--- create parameter_dictionary_table
create table sequence_adaptation (
                                   id integer generated always as identity,

                                   adaptation text not null,

                                   created_at timestamptz not null default now(),
                                   updated_by text,

                                   constraint sequence_adaptation_synthetic_key
                                     primary key (id)
);

comment on table sequence_adaptation is e''
  'A custom adaptation used to overwrite variable and linting rules for the sequence editor';
comment on column sequence_adaptation.adaptation is e''
  'The adaptation code.';

--- Data migration logic

--- Database Check
select * from "public"."sequence_adaptation";
