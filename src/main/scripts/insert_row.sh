#!/bin/sh
# Script to create a record for piction_interface_cinefiles
#
# Script takes one argument: name of file containing tab-delimited records for inserting.
#
# File must have a header with the first column header = 'fpath' for the filepath of the imagefile,
#	followed by the column names in piction_interface_cinefiles.
# e.g.
#
#fpath	piction_id	filename	mimetype	img_size	img_height	img_width	object_csid	action	relationship	dt_addedtopiction	dt_uploaded	dt_processed	sha1_hash	website_display_level	object_number
#/home/elayem/cf_piction_test1.jpg	1234	cf_piction_test1.jpg	image/jpeg	1160322	1830	2193		UPDATE	ALTERNATE	2017-02-22 19:30:49	2017-02-22 21:01:58.0078	2017-02-24 12:12:53.507653	48a317353e1ce8c3bf0eb8f1d3eb363dc55232d9	Display thumbnail only	object_number-6
#/home/elayem/bampfa_1966-8_001_5.jpg	23456	bampfa_1966-8_001_5.jpg	image/jpeg	2170463	2340	3132		UPDATE	ALTERNATE	2016-08-22 12:32:24	2016-09-02 11:31:38.0052	2016-09-22 11:32:31.608243	29b374623e7ce833bd0eb7f1d3ebe63d255972d2	Display thumbnail only	object_number-3
#
# example:
#   ./insert_row.sh testfile.txt
#
# Set the postgres password env var:
#	export PGPASSWORD=<password>
#
HOST="localhost"
PORT="5432"
DBNAME="piction_transit"
DBUSER="piction"
DATAFILE=$1
PICFLOG=piction_int_cf.log
COLNAMES=`head -1 $DATAFILE | sed $'s/\t/,/g'`

date >> $PICFLOG

psql -h $HOST -p $PORT -d $DBNAME -U $DBUSER << CF1_END >> $PICFLOG
	drop table if exists pi_temp;
	create table pi_temp (like piction_interface_cinefiles);
	alter table pi_temp drop id;
	alter table pi_temp add fpath varchar;
	\copy pi_temp ($COLNAMES) from '$DATAFILE' delimiter E'\t' null as '' csv header
CF1_END

for FPATH  in `sed '1d' $DATAFILE | cut -f1`
do
psql -h $HOST -p $PORT -d $DBNAME -U $DBUSER << CF2_END >> $PICFLOG

	\lo_import $FPATH
	\set cf_lastoid :LASTOID
	update pi_temp set bimage = lo_get(:cf_lastoid) where fpath = '$FPATH';
	\lo_unlink :cf_lastoid
CF2_END
done

psql -h $HOST -p $PORT -d $DBNAME -U $DBUSER << CF3_END >> $PICFLOG

	insert into piction_interface_cinefiles (
		piction_id,
		filename,
		mimetype,
		img_size,
		img_height,
		img_width,
		object_csid,
		object_number,
		action,
		relationship,
		dt_addedtopiction,
		dt_uploaded,
		bimage,
		dt_processed,
		sha1_hash,
		website_display_level)
	select
		piction_id,
		filename,
		mimetype,
		img_size,
		img_height,
		img_width,
		object_csid,
		object_number,
		action,
		relationship,
		dt_addedtopiction,
		dt_uploaded,
		bimage,
		dt_processed,
		sha1_hash,
		website_display_level
	from pi_temp;

	drop table pi_temp;

CF3_END

