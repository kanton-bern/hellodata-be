select 	(cp.year::int*10000 + cp.month::int * 100 + 1) as time_id,
		cp."import"::numeric ,
		cp.birth::numeric ,
		cp.import_swiss_ear_tag::numeric ,
		cp.slaugthering::numeric ,
		cp.death::numeric ,
		cp.yard_slaughter::numeric ,
		cp.export::numeric 
from 	{{ source('lzn', 'cattle_popvariations') }} cp