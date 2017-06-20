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
  * Use with the following:
  *   implicit def databaseToCouchbaseLiteDb(s: Database) = new CouchbaseLiteDb(s)
  */
class CouchbaseLiteDb(val d: Database) {
  val log = LoggerFactory.getLogger(getClass.getName)

  import com.couchbase.lite.replicator.Replication
  class ChangeListener extends Replication.ChangeListener {
    var changeIndex = 0
    override def changed(event: Replication.ChangeEvent): Unit = {
      if (changeIndex % 50 == 0) {
        log.info(s"changeIndex $changeIndex " + event.toString)
      }
      changeIndex = changeIndex + 1
    }
  }

  def replicate(replicationUrl: String = "http://127.0.0.1:5984/",
                replicationUser: String = "vvasuki", doPull: Boolean = false) = {
    val url = new URL(replicationUrl + d.getName)
    log.info("replicating to " + url.toString())
    var replicationPw = ""
    if (replicationPw.isEmpty) {
      log info "Enter password"
      replicationPw = StdIn.readLine().trim
    }
    val auth = new BasicAuthenticator(replicationUser, replicationPw)

    val push = d.createPushReplication(url)
    push.setAuthenticator(auth)
    push.setContinuous(true)
    push.addChangeListener(new ChangeListener())
    push.start

    if (doPull) {
      val pull = d.createPullReplication(url)
      //    pull.setContinuous(true)
      pull.setAuthenticator(auth)
      pull.addChangeListener(new ChangeListener())
      pull.start
    }
  }

  def purgeDatabase() = {
    val result = d.createAllDocumentsQuery().run
    val docObjects = result.iterator().map(_.getDocument).map(_.purge())
  }

  def updateDocument(key: String, jsonMap: Map[String, Object], merge: Boolean = false) = {
//    log debug (jsonMap.toString())
    //    sys.exit()
//    avoid dumping excessively long keys - can cause trouble with URLs in browsers.
    def finalizeKey(key: String): String = {
      if (key.length <= 750) {
        return key
      } else {
        return key.substring(0, 749) + "__rest_hashed__" + key.substring(750).hashCode().toString
      }
    }
    val finalKey = finalizeKey(key)
    val document = d.getDocument(key)
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

  def listCaseClassObjects(query: Query, explicitJsonClass: Class[_] = null): Iterator[Any] = {
    val result = query.run
    return result.iterator().map(_.getDocument).map(doc => {
      val jsonMap = collectionUtils.toScala(doc.getUserProperties).asInstanceOf[mutable.Map[String, Any]]
      if (explicitJsonClass != null) {
//        jsonMap.put(jsonHelper.JSON_CLASS_FIELD_NAME, explicitJsonClass)
        jsonMap.put("jsonClass", explicitJsonClass.getSimpleName.replace("$", ""))
      }
      //      val jsonMap = doc.getUserProperties
      jsonHelper.fromJsonMap(jsonMap)
    })
  }


}

