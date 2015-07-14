Pictionbridge updates a UC Berkeley museum's CollectionSpace instance with data sent from the museum's Piction digital asset management system. Updates are pushed by Piction into a database table. Pictionbridge may be run periodically to read updates from that table. It then creates, updates, or deletes records from CollectionSpace as necessary, using CollectionSpace's REST API.

# Development

Maven is required.

To build: `mvn clean install`

This creates compiled artifacts in the `target` directory, including a distribution package (`pictionbridge-[version]-dist.tar.gz`).

To run in development: `mvn exec:java`

This runs Pictionbridge out of the `target` directory. Environment variables must be set, as described below.


# Installation

To install on a server, copy the distribution file (`pictionbridge-[version]-dist.tar.gz`) to the desired location, and expand it.

# Configuration

## Environment Variables

The following environment variables must be set to run the program:

`PICTION_BRIDGE_HOME`

The full path to the Pictionbridge installation directory. This is the directory that was created by expanding the distribution file, and contains the `bin`, `conf`, `lib`, `logs`, and `work` subdirectories.

Note: It is not necessary to set this variable when running in development via `mvn exec:java`. Maven will automatically set the installation directory to the `target` directory. If `PICTION_BRIDGE_HOME` is set, it will have no effect.

`PICTION_BRIDGE_CONF`

The name of a configuration profile to use. This should correspond to a file in `$PICTION_BRIDGE_HOME/conf`, but should not have the .xml extension.

`PICTION_BRIDGE_DB_USER`

The name of the database user to use to access the table containing updates. This database user must exist, and have appropriate permissions on the table.

`PICTION_BRIDGE_DB_PW`

The password of the `PICTION_BRIDGE_DB_USER` user.

`PICTION_BRIDGE_CSPACE_USER`

The name of a CollectionSpace user to use to make updates to the CollectionSpace instance. This CollectionSpace user must exist, and have appropriate permissions on Media and Collection Object records.

`PICTION_BRIDGE_CSPACE_PW`

The password of the `PICTION_BRIDGE_CSPACE_USER` user.

If none of the required variables are set in the environment, pictionbridge will attempt to source them from a file named `.pictionbridge` in the running user's home directory.

## Configuration Profiles

For the configuration profile named by `$PICTION_BRIDGE_CONF`, the profile's settings may be changed by editing `$PICTION_BRIDGE_HOME/conf/$PICTION_BRIDGE_CONF.xml`. See the existing configuration files for all of the available configuration options. Some highlights:

`databaseUpdateMonitor`/`limit`: The maximum number of updates to retrieve per invocation.
`databaseUpdateMonitor`/`dataSource`/`url`: The location of the database to monitor for updates from Piction.

`cspaceRestUploader`/`servicesUrlTemplate`: The location of the CollectionSpace services layer REST API.
`cspaceRestUploader`/`pauseBetweenUpdatesMillis`: A number of milliseconds to pause between each update to CollectionSpace. This may be used for throttling.

`bmuFilenameParser`/`uploadUrl`: The location of the BMU API, used for parsing filenames.

## Logging

Logging may be configured by editing `$PICTION_BRIDGE_HOME/conf/log4j2.xml`.

# Running

Once installed from the distribution package, the program may be executed by running the script `$PICTION_BRIDGE_HOME\bin\pictionbridge`. This script ensures that the required environment variables are set. If none are set, it attempts to source them from a file named `.pictionbridge` in the running user's home directory.

To run during development, see above.