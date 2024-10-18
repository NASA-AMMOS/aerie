-- up.sql creates table and sequence, delete them
drop view ui.derivation_group_comp;
drop view merlin.derived_events;
drop function merlin.subtract_later_ranges cascade;
drop trigger check_external_event_boundaries on merlin.external_event;
drop function merlin.check_external_event_boundaries cascade;
drop trigger external_source_pdg_association_delete on merlin.external_source;
drop function merlin.external_source_pdg_association_delete cascade;
drop table merlin.plan_derivation_group cascade;
drop trigger check_external_event_duration_is_nonnegative_trigger on merlin.external_event;
drop table merlin.external_event cascade;
drop table merlin.external_source cascade;
drop table merlin.derivation_group cascade;
drop table merlin.external_event_type cascade;
drop table merlin.external_source_type cascade;

call migrations.mark_migration_rolled_back('11');
