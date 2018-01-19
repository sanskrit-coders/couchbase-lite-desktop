package sanskrit_coders.db.couchbaseLite

import _root_.java.io.File

import dbSchema.dcs.{DcsBook, DcsObject, DcsSentence}
import dbUtils.jsonHelper
//import com.couchbase.lite.{Database, Manager, JavaContext, Document, UnsavedRevision, Query, ManagerOptions}
//import org.json4s.jackson.Serialization
//import com.couchbase.lite.{Database, Manager, JavaContext, Document, UnsavedRevision, Query, ManagerOptions}
import com.couchbase.lite.{Database, JavaContext, Manager}
//import org.json4s.jackson.Serialization
import org.slf4j.LoggerFactory

// This version of the database uses Java (rather than Android) API.
class DcsCouchbaseLiteDB() {
  implicit def databaseToCouchbaseLiteDb(s: Database) = new CouchbaseLiteDb(s)
  val log = LoggerFactory.getLogger(getClass.getName)
  var booksDb: Database = null
  var sentencesDb: Database = null
  var dbManager: Manager = null

  def openDatabasesLaptop(readOnly: Boolean = false) = {
    val managerOptions = Manager.DEFAULT_OPTIONS
    managerOptions.setReadOnly(readOnly)
    dbManager = new Manager(new JavaContext("data") {
      override def getRootDirectory: File = {
        val rootDirectoryPath = "/home/vvasuki/dcs-scraper"
        new File(rootDirectoryPath)
      }
    }, managerOptions )
    dbManager.setStorageType("ForestDB")
    booksDb = dbManager.getDatabase("dcs_books")
    sentencesDb = dbManager.getDatabase("dcs_sentences")
  }

  def replicateAll(replicationUrl: String = "http://127.0.0.1:5984/",
                   replicationUser: String = "vvasuki", doPull: Boolean = false) = {
    booksDb.replicate(replicationUrl = replicationUrl, replicationUser = replicationUser, doPull = doPull)
    sentencesDb.replicate(replicationUrl = replicationUrl, replicationUser = replicationUser, doPull = doPull)
  }


  def closeDatabases = {
    booksDb.close()
    sentencesDb.close()
  }

  def purgeAll = {
    booksDb.purgeDatabase()
    sentencesDb.purgeDatabase()
  }

  def updateBooksDb(dcsBook: DcsObject): Boolean = {
    val jsonMap = jsonHelper.getJsonMap(dcsBook)
    if (dcsBook.dcsId % 50 == 0) {
      log debug (jsonMap.toString())
    }
    //    sys.exit()
    booksDb.updateDocument(dcsBook.getKey, jsonMap)
    return true
  }

  def updateSentenceDb(dcsSentence: DcsSentence): Boolean = {
    val jsonMap = jsonHelper.getJsonMap(dcsSentence)
    if (dcsSentence.dcsId % 50 == 0) {
      log debug (jsonMap.toString())
    }
    //    sys.exit()
    sentencesDb.updateDocument(dcsSentence.getKey, jsonMap)
    return true
  }

  def getSentences: Iterator[DcsSentence] = {
    val query = sentencesDb.createAllDocumentsQuery()
    sentencesDb.listCaseClassObjects(query=query, explicitJsonClass=DcsSentence.getClass).map(_.asInstanceOf[DcsSentence])
  }

  def getSentencesWithoutAnalysis(): Iterator[DcsSentence] = {
      getSentences.filter(_.dcsAnalysisDecomposition == None)
  }

  def getBook(title: String) : DcsBook = {
    import scala.collection.JavaConverters._
    //    The index below was created in couchdb and later not synced into couchbase lite files (though I tried).
    val query = booksDb.getExistingView("book_index").createQuery()
    log.debug(booksDb.getExistingView("chapter_index").getTotalRows.toString)
    query.setKeys(List(title.asInstanceOf[AnyRef]).asJava)
    log.debug(query.getKeys.toString)
    val results = booksDb.listCaseClassObjects(query = query)
    if (results.hasNext) {
      return results.next().asInstanceOf[DcsBook]
    } else {
      return null
    }
  }
}
