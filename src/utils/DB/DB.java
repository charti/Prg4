package utils.DB;

import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import snaq.db.ConnectionPool;
import snaq.db.DBPoolDataSource;
import utils.Settings;

/**
 * This class implements DB access functionality. The connection configuration is done through settings.config.
 * You find a settings.config.example which you can just rename and enter your configuration.
 * 
 * The database access is pooled by DBpool 7.0 framework.
 * 
 * Use the {@link DB#getMasterDB(Function)} to access DB functionality.
 * @author Christian Chartron
 *
 */
public class DB {
	
	private static DB masterDB;
	private ConnectionPool pool = null;
	
	/**
	 * @param Fetches the configuration from settings.config. Loads 'name' + property.
	 */
	private DB(String name) {
		try {
			Driver drv = (Driver) Class.forName(Settings.getSetting(name + "Driver"))
					.newInstance();
			DriverManager.registerDriver(drv);

			pool = new ConnectionPool(name, 5, 10, 30, 180,
					Settings.getSetting(name + "Url"),
			        Settings.getSetting(name + "User"),
			        Settings.getSetting(name + "Password"));
			
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Accesses the master DB.
	 * @param block Executes the given block against the master DB.
	 * @return You can return any Object from the block.
	 */
	public static <T> T getMasterDB(Function<DB, T> block){
		if (masterDB == null)
			masterDB = new DB("masterDb");
			
		return block.apply(masterDB);
	}
	
	/**
	 * Accesses the master DB without a return from block.
	 * @param block Executes the given block against the master DB.
	 */
	public static void getMasterDB(Consumer<DB> block) {
		if (masterDB == null)
			masterDB = new DB("masterDb");
			
		block.accept(masterDB);
	}

	/**
	 * Inserts a new entry to the DB.
	 * @param table Specifies the targeted table.
	 * @param data A map of data to get inserted.
	 * @return Returns the index of the new entry.
	 */
	public int insert(String table, Map<String, Object> data) {		
		Connection con = null;
		Statement stmnt = null;
		ResultSet res = null;
		int dbId = -1;
		
		String sqlString = String.format("Insert into %s (%s) ", table,
				data.keySet().stream().map(e -> e).collect(Collectors.joining(",")));
		sqlString += String.format("Values(%s);", 
				data.values().stream().map(v -> v instanceof String ? String.format("'%s'", v) : v.toString())
					.collect(Collectors.joining(",")));
		try {
			con = pool.getConnection();
			stmnt = con.createStatement(Statement.RETURN_GENERATED_KEYS, ResultSet.CONCUR_READ_ONLY );
			
			stmnt.executeUpdate(sqlString, new String[] { "dbId" });
			res= stmnt.getGeneratedKeys();
			res.next();
			dbId = res.getInt(1);
		}
		catch (SQLException e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (res != null) res.close();
				if (stmnt != null) stmnt.close();
				if (con != null) con.close();
			}
			catch (SQLException e) {e.toString(); }
		}
		
		return dbId;
	}
	
	/**
	 * Updates an DB entry or entries.
	 * @param table Specifies the targeted table.
	 * @param where The condition for updating entries.
	 * @param data The new data which should be updated.
	 * @return The amount of rows effected.
	 */
	public int update(String table, String where, Map<String, Object> data) {
		if (data.isEmpty())
			return -1;
		
		Connection con = null;
		Statement stmnt = null;
		int rowsEffected = 0;
		
		String set = "Set ";
		for (Map.Entry<String, Object> entry : data.entrySet()) {
			set += String.format("%s = %s, ", entry.getKey(), entry.getValue() instanceof String ?
					String.format("'%s'", entry.getValue()) : entry.getValue());
		}
		set = set.substring(0, set.lastIndexOf(", "));
		
		String sqlString = String.format("Update %s %s Where %s", table, set, where);
		try {
			con = pool.getConnection();
			stmnt = con.createStatement();
			rowsEffected = stmnt.executeUpdate(sqlString);
		}
		catch (SQLException e) {
			rowsEffected = -1;
			System.out.println(e.toString());
		}
		finally {
			try {
				if (stmnt != null) stmnt.close();
				if (con != null) con.close();
			}
			catch (SQLException e) {e.printStackTrace(); }
		}		
		
		return rowsEffected;
	}
	
	/**
	 * Deletes an entry or entries from DB.
	 * @param table Specifies the targeted table.
	 * @param where The condition for deleting entries.
	 * @return The amount of rows effected.
	 */
	public int delete(String table, String where) 
	{	
		Connection con = null;
		Statement stmnt = null;
		int rowsEffected = 0;
		
		String sqlString = String.format("Delete From %s Where %s", table, where);
		try {
			con = pool.getConnection();
			stmnt = con.createStatement();
			rowsEffected = stmnt.executeUpdate(sqlString);
		}
		catch (SQLException e) {
			rowsEffected = -1;
			e.printStackTrace();
		}
		finally {
			try {
				if (stmnt != null) stmnt.close();
				if (con != null) con.close();
			}
			catch (SQLException e) {e.printStackTrace(); }
		}		
		
		return rowsEffected;
	}
	
	/**
	 * Selects entries from DB.
	 * @param table Specifies the targeted table.
	 * @param selector Selects the given columns.
	 * @param tail Gets appended to the transaction.
	 * @return A {@link List} of entries. The map- keys are the column names.
	 */
	public List<Map<String, Object>> select(String table, String selector, String tail) {		
		Connection con = null;
		Statement stmnt = null;
		ResultSet res = null;
		List<Map<String, Object>> list = new ArrayList<Map<String,Object>>();
		
		String sqlString = String.format("Select %s From %s %s", selector, table, tail);
		try {
			con = pool.getConnection();
			stmnt = con.createStatement();
			res = stmnt.executeQuery(sqlString);
			
			ResultSetMetaData meta = res.getMetaData();
			
			int count = meta.getColumnCount();
			
			while (res.next()) {
				Map<String, Object> entry = new HashMap<String, Object>();
				
				for (int i = 1; i <= count; i++) {
					String column = meta.getColumnName(i);
					Object value = res.getObject(i);
					
					entry.put(column, value);
				}
				list.add(entry);
			}
		}
		catch (SQLException e) {
			System.out.println(sqlString);
			e.printStackTrace();
		}
		finally {
			try {
				if (res != null) res.close();
				if (stmnt != null) stmnt.close();
				if (con != null) con.close();
			}
			catch (SQLException e) {e.printStackTrace(); }
		}
		
		return list;
	}
}
