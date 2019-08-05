Fedora 4 Utilities
==================

Utilities for maintaining the [Fedora Commons repository](http://github.com/fcrepo4/fcrepo4).

Building
--------

To build the JAR file

``` sh
mvn package
```

Running
-------


``` sh
java -i /path/to/4.7.x/export -o /optional/path/to/5.x/tranform-directory  -jar /path/to/fcrepo4-upgrade-utils/target/fcrepo-upgrade-utils-5.0.0-SNAPSHOT.jar
```

