Piction Transit Database Migration to QA instance

The piction_transit database was formerly a Postgres database hosted by the UCB Database Team.
Transitioning to a LYRASIS hosted environment required the piction_transit database be migrated
to a DevOps hosted Amazon RDS.  After Ops creates the Postgres instance on Amazon RDS,
run the commands in migrate_piction_transit_qa.txt to create the piction_transit database and objects,
restore/populate the tables, and set up the cron job to refresh the metadata nightly.

Scripts/commands:
1. migrate_piction_transit_qa.txt
   * Creates database and objects; restores data in piction* tables; populates bampfa_metadata_mv table.

2. extract_bampfa_metadata_mv.sql
   * SQL query used in migrate_piction_transit_qa.txt to extract bampfa metadata as a CSV file.

3. load_bampfa_metadata_mv.sql
   * SQL command used in migrate_piction_transit_qa.txt to load bampfa metadata from extracted CSV file.

5. crontab.txt
   * Creates cron job to run script ETL script (bampfa_metadata_etl.sh) on a nightly basis.

6. bampfa_metadata_etl.sh
   * Extracts bampfa metadata and loads into piction.bampfa_metadata_mv table in new piction_transit DB.

Migration Steps after RDS instance is created:

Outline of steps in migrate_piction_transit_qa.txt:
1. Dump piction schema from source database (piction_transit on cspace-prod).
2. In RDS create destination database and objects (piction_transit on Amazon RDS):
   * create database piction_transit
   * create user piction
   * create role piction_app_role
   * create user piction_app
   * create schema piction
   * grant usage on schema piction
   * create table piction.bampfa_metadat_mv (Retain name even though it is no longer a materialized view.)
   * grant select on piction.bampfa_metadat_mv
3. Run extract_bampfa_metadata_mv.sql on bampfa_domain_bampfa to extract data.
4. Transfer bampfa data file from cspace-prod to local.
5. Load data from bampfa data file into new bampfa_metadata_mv table in new piction_transit database.
6. Run pg_restore to recreate piction_interface* and piction_history* tables using dump file from Step 1.
7. In RDS:
   * Create sequences, set defaults, and sequence values
   * Create indexes
   * Grant privileges
8. Create new cron job on blacklight-(qa|prod) to refresh piction.bampfa_metadat_mv
   * See crontab.txt
   * Runs bampfa_metadata_etl.sh nightly at 12:05 AM


