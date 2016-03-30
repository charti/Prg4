package test;

import utils.DB.DB;

public abstract class Helpers {

	public static int deleteAllFrom(String table) {
		return DB.getMasterDB(db -> {
			return db.delete(table, "dbId > 0");
		});
	}
}
