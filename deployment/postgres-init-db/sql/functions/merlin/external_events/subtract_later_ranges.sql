-- Create a function to subtract lists of time ranges
CREATE OR REPLACE FUNCTION merlin.subtract_later_ranges(curr_date tstzmultirange, later_dates tstzmultirange[])
RETURNS tstzmultirange AS $$
DECLARE
	ret tstzmultirange := curr_date;
	later_date tstzmultirange;
BEGIN
	FOREACH later_date IN ARRAY later_dates LOOP
		ret := ret - later_date;
	END LOOP;
	RETURN ret;
END;
$$ LANGUAGE plpgsql;

ALTER FUNCTION merlin.subtract_later_ranges(tstzmultirange, tstzmultirange[])
    OWNER TO aerie;
GRANT EXECUTE ON FUNCTION merlin.subtract_later_ranges(tstzmultirange, tstzmultirange[]) TO aerie;
