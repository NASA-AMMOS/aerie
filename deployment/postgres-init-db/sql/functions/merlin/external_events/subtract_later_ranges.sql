-- Create a function to subtract lists of time ranges
create or replace function merlin.subtract_later_ranges(curr_date tstzmultirange, later_dates tstzmultirange[])
returns tstzmultirange
language plpgsql as $$
  declare
	  ret tstzmultirange := curr_date;
	  later_date tstzmultirange;
begin
	foreach later_date in array later_dates loop
		ret := ret - later_date;
	end loop;
	return ret;
end
$$;

alter function merlin.subtract_later_ranges(tstzmultirange, tstzmultirange[])
    owner to aerie;
grant execute on function merlin.subtract_later_ranges(tstzmultirange, tstzmultirange[]) to aerie;
