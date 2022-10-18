-- export bampfa_metadata_mv data

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

\copy temp_bampfa_metadata_mv to '~/csc2051/bampfa_metadata_mv.csv' csv header
