select 	(ceb.year::int*10000 + ceb.month::int * 100 + 1) as time_id,
		ceb.h::numeric ,
		ceb.t::numeric  + ceb."t.1"::numeric  + ceb."t.2"::numeric  as t,
		ceb.a::numeric ,
		ceb."1_x"::numeric  + ceb."2_x"::numeric  + ceb."3_x"::numeric  as x,
		ceb.c::numeric,
		cef."1"::numeric,
		cef."2"::numeric,
		cef."3"::numeric,
		cef."4"::numeric,
		cef."5"::numeric 
from 	{{ source('lzn', 'cattle_evolbeefiness') }} ceb
join 	{{ source('lzn', 'cattle_evolfattissue') }} cef
on		ceb.year = cef.year
and 	ceb.month = cef.month