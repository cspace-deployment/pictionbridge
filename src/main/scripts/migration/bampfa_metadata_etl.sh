#!/bin/bash
#
# Name: bampfa_metadata_etl.sh
# 
# Purpose:
#   1. In bampfa_domain_bampfa database, extract data into CSV file.
#   2. In piction_transit database:
#      a. Copy data from piction.bampfa_metadata_mv into archive table piction.bampfa_metadata_mv_arc.
#      b. Load data from bampfa_domain_bampfa CSV file into piction.bampfa_metadata_mv.
#
# Requirement: set up extract directory
#   1. mkdir -p ${HOME}/pictiontransit/extracts
# 
# Usage: ./bampfa_metadata_etl.sh
# 
# Verification:
#   1. Log in to piction_transit database and run count query in BM_EXT.
#   2. Log in to bampfa_domain_bampfa database and run queries:
#      a. select count(*) from bampfa_metadata_mv;
#      b. select count(*) from bampfa_metadata_mv_arc;
#
# Note: Credentials set for connecting to databases through blacklight-qa.ets.berkeley.edu.
#       May need to adjust for Prod connections.
#

# START OF SCRIPT

## Set Date/Time suffix
YMDHM=`date +%y%m%d%H%M`

## Set working directory
BMEDIR=${HOME}/pictiontransit/extracts

## Set log and data files
BMELOG=bampfa_metadata_${YMDHM}.log
BMECSV=bampfa_metadata_${YMDHM}.csv

## Set connection parameters for bampfa_domain_bampfa database
CSHOST="localhost"
CSPORT="54321"
CSDB="bampfa_domain_bampfa"
CSUSER="bampfa"
CSCONNECT="-h $CSHOST -p $CSPORT -d $CSDB -U $CSUSER"

## Set connection parameters for piction_transit database
PTHOST="localhost"
PTPORT="5432"
PTDB="piction_transit"
PTUSER="piction"
PTCONNECT="-h $PTHOST -p $PTPORT -d $PTDB -U $PTUSER"

## Change to working directory
cd $BMEDIR

## Log start time, locations, filenames, connections
echo `date`:  INFO: bampfa_metadata_etl.sh START TIME | tee -a $BMELOG
echo `date`:  INFO: Extract Dir: $BMEDIR | tee -a $BMELOG
echo `date`:  INFO: Extract Log: $BMELOG | tee -a $BMELOG
echo `date`:  INFO: Extract CSV: $BMECSV | tee -a $BMELOG
echo `date`:  INFO: Starting Extract using psql $CSCONNECT | tee -a $BMELOG
echo '' >> $BMELOG

## Connect to bampfa_domain_bampfa database and export bampfa metadata
psql $CSCONNECT -at << BM_EXT >> $BMELOG

-- store data in temp table for export
create temp table temp_bampfa_metadata_mv as
 SELECT
    hcc.name AS objectcsid,
    cc.objectnumber AS idnumber,
    cb.sortableeffectiveobjectnumber AS sortobjectnumber,
    COALESCE(NULLIF(cb.artistdisplayoverride, ''), utils.concat_artists(hcc.name)) AS artistcalc,
    CASE
        WHEN (pcn.item = pc.birthplace) then pcn.item
        ELSE COALESCE(pcn.item || ', born ' || NULLIF(pc.birthplace, ''), pcn.item) END AS artistorigin,
    btg.bampfatitle AS title,
    pdg.datedisplaydate AS datemade,
    NULLIF(plg.objectproductionplace, '') AS site,
    utils.getdispl(cb.itemclass) AS itemclass,
    cc.physicaldescription AS materials,
    replace(mpg.dimensionsummary, '-', ' ') AS measurement,
    CONCAT_WS('; ',
        'University of California, Berkeley Art Museum and Pacific Film Archive',  
        NULLIF(cb.creditline, '')) AS fullbampfacreditline,
    COALESCE(NULLIF(cb.copyrightcredit, ''), pb.copyrightcredit) AS copyrightcredit,
    cb.photocredit,
    CONCAT_WS('; ', 
        utils.getdispl(st1.item), 
        utils.getdispl(st2.item), 
        utils.getdispl(st3.item), 
        utils.getdispl(st4.item), 
        utils.getdispl(st5.item)) AS subjects,
    CONCAT_WS('; ', 
        utils.getdispl(cl1.item), 
        utils.getdispl(cl2.item), 
        utils.getdispl(cl3.item)) AS collections,
    CONCAT_WS('; ', 
        utils.getdispl(ps1.item), 
        utils.getdispl(ps2.item), 
        utils.getdispl(ps3.item), 
        utils.getdispl(ps4.item), 
        utils.getdispl(ps5.item)) AS periodstyles,
    CONCAT_WS('-', 
        NULLIF(bdg.datedisplaydate, ''),
        NULLIF(ddg.datedisplaydate, '')) AS artistdates,
    '' AS caption,
    '' AS tags,
    COALESCE(NULLIF(cb.permissiontoreproduce, ''), 'Unknown') AS permissiontoreproduce,
    cas.item AS acquisitionsource,
    utils.getdispl(cb.legalstatus) AS legalstatus,
    c.updatedat
FROM hierarchy hcc
JOIN collectionobjects_common cc ON hcc.id = cc.id
JOIN misc m ON cc.id = m.id AND m.lifecyclestate <> 'deleted'
LEFT OUTER JOIN collectionobjects_bampfa cb ON cc.id = cb.id
JOIN collectionspace_core c ON cc.id = c.id
LEFT JOIN hierarchy hpdg 
    ON hpdg.parentid = cc.id 
    AND hpdg.name = 'collectionobjects_common:objectProductionDateGroupList' 
    AND hpdg.pos = 0
LEFT JOIN structureddategroup pdg ON hpdg.id = pdg.id
LEFT JOIN hierarchy hbtg 
    ON hbtg.parentid = cc.id 
    AND hbtg.name = 'collectionobjects_bampfa:bampfaTitleGroupList' 
    AND hbtg.pos = 0
LEFT JOIN bampfatitlegroup btg ON hbtg.id = btg.id
LEFT JOIN hierarchy hmpg 
    ON hmpg.parentid = cc.id 
    AND hmpg.name = 'collectionobjects_common:measuredPartGroupList' 
    AND hmpg.pos = 0
LEFT JOIN measuredpartgroup mpg ON hmpg.id = mpg.id
LEFT JOIN collectionobjects_bampfa_acquisitionsources cas ON cc.id = cas.id AND cas.pos = 0
LEFT JOIN hierarchy hppg 
    ON hppg.parentid = cc.id 
    AND hppg.name = 'collectionobjects_bampfa:bampfaObjectProductionPersonGroupList' 
    AND hppg.pos = 0
LEFT JOIN bampfaobjectproductionpersongroup ppg ON hppg.id = ppg.id
LEFT JOIN persons_common pc ON ppg.bampfaobjectproductionperson = pc.refname
LEFT JOIN persons_common_nationalities pcn ON pc.id = pcn.id AND pcn.pos = 0
LEFT JOIN hierarchy hbdg ON hbdg.parentid = pc.id AND hbdg.name = 'persons_common:birthDateGroup'
LEFT JOIN structureddategroup bdg ON hbdg.id = bdg.id
LEFT JOIN hierarchy hddg ON hddg.parentid = pc.id AND hddg.name = 'persons_common:deathDateGroup'
LEFT JOIN structureddategroup ddg ON hddg.id = ddg.id
LEFT JOIN persons_bampfa pb ON pc.id = pb.id
LEFT JOIN hierarchy hplg 
    ON hplg.parentid = cc.id 
    AND hplg.name = 'collectionobjects_common:objectProductionPlaceGroupList' 
    AND hplg.pos = 0
LEFT JOIN objectproductionplacegroup plg ON hplg.id = plg.id
LEFT JOIN collectionobjects_bampfa_subjectthemes st1 ON st1.id = cc.id AND st1.pos = 0
LEFT JOIN collectionobjects_bampfa_subjectthemes st2 ON st2.id = cc.id AND st2.pos = 1
LEFT JOIN collectionobjects_bampfa_subjectthemes st3 ON st3.id = cc.id AND st3.pos = 2
LEFT JOIN collectionobjects_bampfa_subjectthemes st4 ON st4.id = cc.id AND st4.pos = 3
LEFT JOIN collectionobjects_bampfa_subjectthemes st5 ON st5.id = cc.id AND st5.pos = 4
LEFT JOIN collectionobjects_bampfa_bampfacollectionlist cl1 ON cl1.id = cc.id AND cl1.pos = 0
LEFT JOIN collectionobjects_bampfa_bampfacollectionlist cl2 ON cl2.id = cc.id AND cl2.pos = 1
LEFT JOIN collectionobjects_bampfa_bampfacollectionlist cl3 ON cl3.id = cc.id AND cl2.pos = 2
LEFT JOIN collectionobjects_common_styles ps1 ON ps1.id = cc.id AND ps1.pos = 0
LEFT JOIN collectionobjects_common_styles ps2 ON ps2.id = cc.id AND ps2.pos = 1
LEFT JOIN collectionobjects_common_styles ps3 ON ps3.id = cc.id AND ps3.pos = 2
LEFT JOIN collectionobjects_common_styles ps4 ON ps4.id = cc.id AND ps4.pos = 3
LEFT JOIN collectionobjects_common_styles ps5 ON ps5.id = cc.id AND ps5.pos = 4
ORDER BY cb.sortableeffectiveobjectnumber;

select count(*)::text || ' : temp_bampfa_metadata_mv count' from temp_bampfa_metadata_mv;

-- extract data from temp table to CSV file
\copy temp_bampfa_metadata_mv to '$BMECSV' csv header

BM_EXT

## Log extract finish time
echo '' >> $BMELOG
echo `date`:  INFO: Finished Extract | tee -a $BMELOG
echo '' >> $BMELOG

## Get line count of bampfa_metadata extract from temp table
BAMCOUNT=`grep COPY $BMELOG | sed -e 's/^.* //'`

if [ -s $BMECSV ]; then
  ## Log data extract file name and line count
  echo `date`:  INFO: EXTRACTED BAMPFA metadata to: $BMECSV | tee -a $BMELOG
  CSVLC=`wc -l $BMECSV`
  echo `date`:  INFO: CSV file line count: $CSVLC | tee -a $BMELOG
  echo `date`:  INFO: Starting Load using psql $PTCONNECT | tee -a $BMELOG
  echo '' >> $BMELOG

  ## Connect to piction_transit database and load bampfa metadata
  psql $PTCONNECT -at << PT_LOAD >> $BMELOG

    -- log old bampfa_metadata_mv rowcount
    select count(*)::text || ' : OLD bampfa_metadata_mv count' from bampfa_metadata_mv;

    -- clear old archived data and archive old bampfa_metadata_mv data
    drop table if exists bampfa_metadata_mv_arc;
    create table bampfa_metadata_mv_arc as select * from bampfa_metadata_mv;

    -- clear old data and load new data from csv extract file
    truncate table bampfa_metadata_mv;
    \copy bampfa_metadata_mv from '$BMECSV' csv header

    -- log new bampfa_metadata_mv rowcount
    select count(*)::text || ' : NEW bampfa_metadata_mv count' from bampfa_metadata_mv;

PT_LOAD

  ## Log load finish time
  echo '' >> $BMELOG
  echo `date`:  INFO: Finished Load | tee -a $BMELOG
  echo '' >> $BMELOG
    
  ## Log CSV file info
  CSVLS=`ls -l $BMECSV`
  echo `date`:  INFO: $CSVLS | tee -a $BMELOG

  ## Get row count of piction_transit bampfa_metadata_mv table
  PTCOUNT=`psql $PTCONNECT -t -c 'select count(*) from bampfa_metadata_mv;'`

  ## Check extract count with load count
  if [ $BAMCOUNT -eq $PTCOUNT ]; then
    echo `date`:  INFO: BAMPFA Extract Count $BAMCOUNT equals Piction Load Count $PTCOUNT | tee -a $BMELOG
  else
    echo `date`: ERROR: BAMPFA Extract Count $BAMCOUNT does not equal Piction Load Count $PTCOUNT | tee -a $BMELOG
  fi

  ## Tar CSV extract, log info, and remove CSV file
  tar -zcvf ${BMECSV}.tar.gz $BMECSV
  CSVTARGZLS=`ls -l ${BMECSV}.tar.gz`
  echo `date`:  INFO: $CSVTARGZLS | tee -a $BMELOG
  rm -f $BMECSV

  ## Check log for errors
  ERRCOUNT=`grep -i error $BMELOG | wc -l`

  ## Clean up and send notification when errors have been logged
  if [ $ERRCOUNT -gt 0 ]; then
    ## Send error notification
    echo `date`: ERROR found in log file: keeping previous CSV and log files | tee -a $BMELOG
    mail -r "cspace-support@lists.berkeley.edu" -s "Piction bampfa_metadata_mv refresh" lkv@berkeley.edu \
      <<< "ERROR found in Piction bampfa_metadata_mv refresh log: $BMELOG"
  else
    ## Remove old CSV and log files
    RMFILE=`ls -t bampfa_metadata*.csv* | tail -1` 
    RMBASE=${RMFILE%.*.*.*}
    RMBLOG=${RMBASE}.log

    ## Check whether oldest CSV and log files are current
    if [ $RMBLOG == $BMELOG ]; then
      echo `date`: Keeping CSV and associated log files: current file are oldest | tee -a $BMELOG
    else
      rm -f ${RMBASE}.*
      echo `date`: Removing previous CSV file: $RMFILE and associated log file | tee -a $BMELOG
    fi

    ## Only send mail when there are errors in the log.
    #  mail -r "cspace-support@lists.berkeley.edu" -s "Piction bampfa_metadata_mv refresh" lkv@berkeley.edu \
    #    <<< "Piction bampfa_metadata_mv refresh DONE"
  fi

else
  ## Log CSV file error
  echo `date`: ERROR: BAMPFA metadata CSV file $BMECSV DOES NOT EXIST or IS EMPTY | tee -a $BMELOG
fi

## Log ETL finish time
echo `date`:  INFO: FINISHED bampfa_metadata ETL | tee -a $BMELOG

## END OF SCRIPT
