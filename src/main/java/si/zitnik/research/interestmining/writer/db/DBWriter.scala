package si.zitnik.research.interestmining.writer.db

import java.sql._
import si.zitnik.research.interestmining.model.stackoverflow.User
import org.json.JSONObject

/**
 * Created with IntelliJ IDEA.
 * User: slavkoz
 * Date: 5/14/13
 * Time: 10:49 AM
 * To change this template use File | Settings | File Templates.
 */
class DBWriter {
  val connectionUrl = "jdbc:sqlserver://192.168.25.131\\SQLEXPRESS:1433;databaseName=interestmining;user=sa;password=xs;";
  Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver")
  val con = DriverManager.getConnection(connectionUrl)

  /**
   * val SQL = "INSERT INTO dbo.Users VALUES ('mirko', 'x:0.3;ffff:0.98;fff:0.7')";
   */
  /* using prepared statement instead
  def insert(SQL: String) {
    var stmt: Statement = null

    //INSERT
    stmt = con.createStatement()
    val updatedNo = stmt.executeUpdate(SQL)
    if (updatedNo == 0) {
      System.err.println("There were no rows updated!")
    }

    stmt.close()
  }     */
  def insert(stmt: PreparedStatement) = {
    val updatedNo = stmt.executeUpdate()
    if (updatedNo == 0) {
      System.err.println("There were no rows updated!")
    }
    stmt.close()
    updatedNo
  }

  def insertEvidence(stms: PreparedStatement, postId: String) = {
    con.setAutoCommit(false)
    var retVal = true
    var savePoint: Savepoint = null
    try {
      savePoint = con.setSavepoint()
      insert(stms)

      val rs = con.prepareStatement("SELECT  ident_current('dbo.Evidence')").executeQuery()
      rs.next()
      val evidenceId = rs.getInt(1)

      val stms1 = con.prepareStatement("INSERT INTO dbo.EvidencePost VALUES (?, ?)")
      stms1.setInt(1, evidenceId)
      stms1.setString(2, postId)
      insert(stms1)
    } catch {
      case e: Exception => if (e.getMessage.contains("FOREIGN KEY constraint")) {
        retVal = false
        con.rollback(savePoint)
      } else {
        e.printStackTrace()
        System.exit(-1)
      }
    }
    con.setAutoCommit(true)
    retVal
  }

  /**
   * Return categories
   * @param Id
   * @return
   */
  def userExists(Id: String): String = {
    val stmt = con.prepareStatement("SELECT categories FROM dbo.Users WHERE id LIKE ?")
    stmt.setString(1, Id)

    val rs = stmt.executeQuery()
    if (rs.next()) {
      return rs.getString(1)
    } else {
      return null
    }
  }


  def insertOrUpdateCategory(user: User) {
    val oldCategories = userExists(user.Id)
    if (oldCategories != null) {
      val jsonOldCategories = new JSONObject(oldCategories)
      val jsonNewCategories = user.categories

      val it = jsonNewCategories.keys()
      while (it.hasNext) {
        val key = it.next().toString
        if (jsonOldCategories.has(key)) {
          jsonOldCategories.put(key, jsonOldCategories.getInt(key) + jsonNewCategories.getInt(key))
        } else {
          jsonOldCategories.put(key, jsonNewCategories.get(key))
        }
      }

      val stmt = con.prepareStatement("UPDATE dbo.Users SET categories = ? WHERE id LIKE ?")
      stmt.setString(1, jsonOldCategories.toString())
      stmt.setString(2, user.Id)
      stmt.executeUpdate()
    } else {
      insert(user.toSql())
    }
  }

  def delete(SQL: String) {
    var stmt: Statement = null
    stmt = con.createStatement()
    stmt.execute(SQL)
    stmt.close()
  }

  def commit() {
    con.commit()
  }

  def close() {
    con.close()
  }
}

object DBWriter {
  private lazy val writer = new DBWriter()

  def instance() = {
    writer
  }
}

