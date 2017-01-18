package playdb

import com.typesafe.config.Config
import play.api.db._
import play.api.inject.{Injector, NewInstanceInjector}

import scala.util.control.NonFatal
import play.api.{Configuration, Environment, Logger}

/**
  * Implementation of the DB API.
  */
class BetterDBApi(
  configuration: Map[String, Config],
  defaultConnectionPool: ConnectionPool = new HikariCPConnectionPool(Environment.simple()),
  environment: Environment = Environment.simple(),
  injector: Injector = NewInstanceInjector) extends DBApi {

  import BetterDBApi._

  lazy val databases: Seq[Database] = {
    configuration.map {
      case (name, config) =>
        val pool = ConnectionPool.fromConfig(config.getString("pool"), injector, environment, defaultConnectionPool)
        new PooledDatabase(name, config, environment, pool)
    }.toSeq
  }

  private lazy val databaseByName: Map[String, Database] = {
    databases.map(db => (db.name, db)).toMap
  }

  def database(name: String): Database = {
    databaseByName.getOrElse(name, throw new IllegalArgumentException(s"Could not find database for $name"))
  }

  /**
    * Try to connect to all data sources.
    */
  def connect(logConnection: Boolean = false): Unit = {
    databases foreach { db =>
      try {
        db.getConnection.close()
        if (logConnection) logger.info(s"Database [${db.name}] connected at ${db.url}")
      } catch {
        case NonFatal(e) =>
          // this is only an issue if initializeFailFast is true
          val dbConfig = configuration.get(db.name).get
          if(dbConfig.hasPath("hikaricp.initializationFailFast") && dbConfig.getBoolean("hikaricp.initializationFailFast")) {
            throw Configuration(configuration(db.name)).reportError("url", s"Cannot connect to database [${db.name}]", Some(e))
          }
          else {
            logger.info(s"Database [${db.name}] is not connected, but continuing anyway...")
          }
      }
    }
  }

  def shutdown(): Unit = {
    databases foreach (_.shutdown())
  }

}

object BetterDBApi {
  private val logger = Logger(classOf[BetterDBApi])
}
