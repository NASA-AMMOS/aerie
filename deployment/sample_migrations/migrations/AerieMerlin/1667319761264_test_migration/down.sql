update plan
set parent_id = null
where id = 1;

delete from plan
where name = 'New plan';
