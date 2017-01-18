package playdb

import javax.inject.{Inject, Provider, Singleton}

import com.typesafe.config.Config
import play.api.db._

import scala.concurrent.Future
import play.api.inject._
import play.api._
import play.db.NamedDatabaseImpl
import scala.collection.JavaConversions._

/**
  * DB runtime inject module.
  */
final class DBModule extends Module {
  def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    val dbKey = configuration.underlying.getString("play.db.config")
    val default = configuration.underlying.getString("play.db.default")
    val dbs = configuration.getConfig(dbKey).getOrElse(Configuration.empty).subKeys
    Seq(
      bind[DBApi].toProvider[DBApiProvider]
    ) ++ namedDatabaseBindings(dbs) ++ defaultDatabaseBinding(default, dbs)
  }

  private def bindNamed(name: String): BindingKey[Database] = {
    bind[Database].qualifiedWith(new NamedDatabaseImpl(name))
  }

  private def namedDatabaseBindings(dbs: Set[String]): Seq[Binding[_]] = dbs.toSeq.map { db =>
    bindNamed(db).to(new NamedDatabaseProvider(db))
  }

  private def defaultDatabaseBinding(default: String, dbs: Set[String]): Seq[Binding[_]] = {
    if (dbs.contains(default)) Seq(bind[Database].to(bindNamed(default))) else Nil
  }
}

/**
  * DB components (for compile-time injection).
  */
trait DBComponents {
  def environment: Environment
  def configuration: Configuration
  def connectionPool: ConnectionPool
  def applicationLifecycle: ApplicationLifecycle

  lazy val dbApi: DBApi = new DBApiProvider(environment, configuration, connectionPool, applicationLifecycle).get
}

/**
  * Inject provider for DB implementation of DB API.
  */
@Singleton
class DBApiProvider @Inject() (environment: Environment, configuration: Configuration,
  defaultConnectionPool: ConnectionPool, lifecycle: ApplicationLifecycle,
  injector: Injector = NewInstanceInjector) extends Provider[DBApi] {

  lazy val get: DBApi = {
    val config = configuration.underlying
    val dbKey = config.getString("play.db.config")
    val pool = ConnectionPool.fromConfig(config.getString("play.db.pool"), injector,
      environment, defaultConnectionPool)

    val configs = if (config.hasPath(dbKey)) {
      ConfigHelper(config).getPrototypedMap(dbKey, "play.db.prototype").mapValues(_.underlying)
    } else Map.empty[String, Config]

    val db = new BetterDBApi(configs, pool, environment)
    lifecycle.addStopHook { () => Future.successful(db.shutdown()) }
    db.connect(logConnection = environment.mode != Mode.Test)
    db
  }
}

class ConfigHelper(val underlying: Config) {
  def get(path: String) = {
    underlying.getObject(path).keySet().toList.map { key =>
      key -> underlying.getConfig(s"$path.$key")
    }
  }

  def getPrototypedMap(path: String, prototypePath: String = "prototype.%path"): Map[String, ConfigHelper] = {
    val prototype = if (prototypePath.isEmpty) {
      underlying
    } else {
      underlying.getConfig(prototypePath.replace("%path", path))
    }
    get(path).map {
      case (key, config) => key -> new ConfigHelper(config.withFallback(prototype))
    }.toMap
  }
}

object ConfigHelper {
  def apply(underlying: Config) = new ConfigHelper(underlying)
  def apply(configuration: Configuration) = new ConfigHelper(configuration.underlying)
}

/**
  * Inject provider for named databases.
  */
class NamedDatabaseProvider(name: String) extends Provider[Database] {
  @Inject private var dbApi: DBApi = _
  lazy val get: Database = dbApi.database(name)
}
