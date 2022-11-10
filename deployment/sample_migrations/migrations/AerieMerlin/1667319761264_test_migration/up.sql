update plan
set parent_id = 2
where id = 1;

select duplicate_plan(1, 'New plan');
