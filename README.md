# playdb
A Play Framework DB Module.

This Play module allows you to start the application without needing a
connection to a database. The connection pool handler (HikariCP) will handle
creating and maintaining a database connection.

## Setps to Install

1. Add `"com.github.njlg" %% "playdb" % "0.1"` as a `libraryDependencies`
2. Configure each database configuration to have `hikaricp.initializationFailFast = false`.
   In your `application.conf` change this:
```
db {
  api {
    driver: org.mariadb.jdbc.Driver
    url: "jdbc:mysql://mysql.host.com/common"
    username: "username"
    password: "p4ssw0rd"
  }
}
```
  To this:
```
  db {
    api {
      driver: org.mariadb.jdbc.Driver
      url: "jdbc:mysql://mysql.host.com/common"
      username: "username"
      password: "p4ssw0rd"
      hikaricp: {
        initializationFailFast: false
      }
    }
  }
```

3. Disable the default Play driver &amp; enable this one.
  ```
  // disable this because it crashes the app if it cannot connect to all databases
  play.modules.disabled += "play.api.db.DBModule"

  // use this to start the app without a connection
  play.modules.enabled += "com.github.njlg.playdb.DBModule"
  ```
