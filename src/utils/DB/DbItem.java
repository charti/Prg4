package utils.DB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Extend this class if you create an new Object, which can be written to DB. It provides functionality to
 * check for uncommited changes and for commiting these.
 * @author Christian Chartron
 *
 */
public abstract class DbItem implements ICommitable {
	protected int dbId = -1;
	private String table = null;
	private Map<String, Object> uncommitedChanges = null;

	protected Map<String, Object> getUncommitedChanges() {
		return uncommitedChanges;
	}
	
	protected String getTable() {
		return table;
	}
	
	/**
	 * Adds an Entry<String, Object> to the {@link #uncommitedChanges} Map. Use this in every setter call of 
	 * your child class.
	 * @param name
	 * @param value
	 */
	protected void cacheChange(String name, Object value) {
		uncommitedChanges.put(name, value);
	}
	
	protected DbItem(String table) {
		uncommitedChanges = new HashMap<String, Object>();
		this.table = table;
	}
	
	public boolean hasUncommitedChanges() {
		return !uncommitedChanges.isEmpty();
	}
	
	/**
	 * Writes changes to the DB.
	 */
	public void commitChanges() {
		if (hasUncommitedChanges())
			DB.getMasterDB(db -> {
				if (dbId == -1)
					dbId = db.insert(table, uncommitedChanges);
				else
					db.update(table, "dbId = " + dbId, uncommitedChanges);
			});
		
		if (dbId != -1)
			uncommitedChanges.clear();		
	}	
}