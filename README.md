Fedora 4 Upgrade Utilities
==========================

Utilities for version upgrades of the [Fedora Commons repository](http://github.com/fcrepo4/fcrepo4).

Upgrading from 4.3, 4.5, 4.6 to Fedora 4.7

* BackupFixer:
    * Fedora 4.7.0 transitions to a new backend store, and therefore requires a data upgrade.
    * It should be run on the directory produced by a /fcr:backup request, prior to requesting /fcr:restore
    * For details on executing /fcr:backup and /fcr:restore, see [documentation](https://wiki.duraspace.org/display/FEDORA47/RESTful+HTTP+API+-+Backup+and+Restore)
    * Warning: This utility is destructive (but likely not in a bad way), and should be run against a copy of the backup if the backup is irreplaceable.

Building
--------

To build the JAR file

``` sh
mvn package
```

Running
-------

1. Backup your repository
    * ```sh
        curl -X POST $HOST:$PORT/$CONTEXT/rest/fcr:backup
        ```

2. Restore your repository
    * ```sh
        curl -X POST --data-binary "/tmp/fcrepo4-data/path/to/backup/directory" $HOST:$PORT/$CONTEXT/rest/fcr:restore
        ```

    * Optional step "2b": If the restore operation results in errors similar to those documented in this [ticket](https://jira.duraspace.org/browse/FCREPO-2069),
then run the utility documented below:

        ``` sh
        java -jar /path/to/fcrepo4-upgrade-utils/target/fcrepo-upgrade-utils-4.8.0-SNAPSHOT.jar /path/to/backup/directory
        ```

        If you ran this optional step, then retry step "2".


Earlier Version Upgrades
------------------------

Upgrading from 4.2 to Fedora 4.3

* See [documentation](https://github.com/fcrepo4-exts/fcrepo4-upgrade-utils/tree/4.2-4.3)