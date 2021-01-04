# jassd
Java Automated Spring Sourcecode for Databases generator.  

For those that:

1. Do not want to use an ORM like Hibernate and prefer to use simple JDBC
2. Use Spring Boot
3. Have an existing database that they need to create POJOs and Dao classes for

Then this project will automatically generate java classes representing the tables, Dao interfaces for talking to the database, Dao implementation classes for getting, updating, and inserting data, as well as a simple controller, that can be copied into a Spring Boot project and be used as a starting point for further development.  It eliminates all of the bootstrapping code that would be needed to setup simple database access.

Validated with MySQL databases so far.

## Usage
Download the jar file or build your own version, update the config.properties with the database connection information and default package that you want to use, and then execute the jar file to have sources generated.


