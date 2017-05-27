package sanskrit_coders.db.couchbaseLite

import java.net.URL

import com.couchbase.lite.auth.BasicAuthenticator
import com.couchbase.lite.{Database, Document, Query, UnsavedRevision}
import dbUtils.{collectionUtils, jsonHelper}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.io.StdIn
import scala.collection.JavaConversions._

/**
  * Created by vvasuki on 5/26/17.
  */
object couchbaseLiteUtils {
  val log = LoggerFactory.getLogger(getClass.getName)
  def replicate(database: Database, replicationUrl: String = "http://127.0.0.1:5984/",
                replicationUser: String = "vvasuki", doPull: Boolean = false) = {
    import com.couchbase.lite.replicator.Replication
    val url = new URL(replicationUrl + database.getName)
    log.info("replicating to " + url.toString())
    var replicationPw = ""
    if (replicationPw.isEmpty) {
      log info "Enter password"
      replicationPw = StdIn.readLine().trim
    }
    val auth = new BasicAuthenticator(replicationUser, replicationPw)

    val push = database.createPushReplication(url)
    push.setAuthenticator(auth)
    push.setContinuous(true)
    push.addChangeListener(new Replication.ChangeListener() {
      override def changed(event: Replication.ChangeEvent): Unit = {
        log.info(event.toString)
      }
    })
    push.start

    if (doPull) {
      val pull = database.createPullReplication(url)
      //    pull.setContinuous(true)
      pull.setAuthenticator(auth)
      pull.addChangeListener(new Replication.ChangeListener() {
        override def changed(event: Replication.ChangeEvent): Unit = {
          log.info(event.toString)
        }
      })
      pull.start
    }
  }

  def purgeDatabase(database: Database) = {
    val result = database.createAllDocumentsQuery().run
    val docObjects = result.iterator().map(_.getDocument).map(_.purge())
  }

  def updateDocument(document: Document, jsonMap: Map[String, Object], merge: Boolean = false) = {
    document.update(new Document.DocumentUpdater() {
      override def update(newRevision: UnsavedRevision): Boolean = {
        val jsonMapJava = collectionUtils.toJava(jsonMap).asInstanceOf[java.util.Map[String, Object]]
        //        log debug jsonMapJava.getClass.toString
        if (merge) {
          var properties = newRevision.getUserProperties
          properties.putAll(jsonMapJava)
          newRevision.setUserProperties(properties)
        } else {
          newRevision.setUserProperties(jsonMapJava)
        }
        true
      }
    })
  }

  def listCaseClassObjects(query: Query) = {
    val result = query.run
    val docObjects = result.iterator().map(_.getDocument).map(doc => {
      val jsonMap = collectionUtils.toScala(doc.getUserProperties).asInstanceOf[mutable.Map[String, _]]
      //      val jsonMap = doc.getUserProperties
      jsonHelper.fromJsonMap(jsonMap)
    })
    //    log info s"We have ${quotes.length} quotes."
    docObjects.foreach(obj => {
      log info obj.toString
      log info jsonHelper.getJsonMap(obj).toString()
    })
  }

}
