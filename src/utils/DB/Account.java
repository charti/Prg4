package utils.DB;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.naming.AuthenticationException;

/**
 * An Account is needed to create, modify or load Contacts. Also it is needed to create a Job. This class
 * provides an access restriction for data. 
 * @author Christian Chartron
 *
 */
public class Account extends DbItem {
	
	private String name;
	private String mail;
	
	public String getMail() {
		return mail;
	}

	public void setMail(String mail) {
		this.mail = mail;
		cacheChange("mail", mail);
	}

	public String getName() {
		return name;
	}

	public boolean isAuthenticated() {
		return dbId != -1;
	}

	public int getDbId() {
		return dbId;
	}
	
	private Account(String name, String psswd) throws AuthenticationException
	{
		super("accounts");
		
		String where = String.format("Where name='%s' AND password='%s'", name, psswd);
		try {
			DB.getMasterDB(db -> {
				List<Map<String, Object>> res = db.select(getTable(), "dbId, mail", where);
				
				if (!res.isEmpty()) {
					dbId = (int) res.get(0).get("dbId");
					mail = (String) res.get(0).get("mail");				
				}
			});
			if (dbId != -1) {
				this.name = name;
			}
		}
		catch (IndexOutOfBoundsException e) { System.out.println(e.toString()); }
	}	
	
	/**
	 * Creates a new Account. You have to call {@link #commitChanges()} to write the Account to DB.
	 * 
	 * @param name Represents the unique identifier for the Account. Has to be a length of at least 4 chars.
	 * @param psswd A password has to be at least 8 chars long.
	 * @param mail You have to enter an email address.
	 * @throws Exception There are some requirements for the given params to be met.
	 */
	public Account(String name, String psswd, String mail) throws Exception {
		super("accounts");
		
		if (name.length() < 4)
			throw new Exception("Name has to be at least 4 char long.");
		if (psswd.length() < 8)
			throw new Exception("Psswd has to be at least 8 char long.");
		if (mail.length() == 0)
			throw new Exception("You have to enter an Email address.");
		
		this.mail = mail;
		cacheChange("mail", mail);
		this.name = name;
		cacheChange("name", name);
		cacheChange("password", psswd);
	}
	
	/**
	 * Loads an Account by name and password.
	 * @param name
	 * @param psswd
	 * @return Null if no account could be found.
	 */
	public static Account load(String name, String psswd)
	{
		Account acc = null;
		try { acc = new Account(name, psswd); }
		catch (AuthenticationException e) { e.printStackTrace(); }
		
		return acc.isAuthenticated() ? acc : null;
	}
	
	/**
	 * Loads an Account by dbId. 
	 * @param dbId
	 * @return Null if no Account could be found.
	 */
	public static Account load(int dbId) {
		List<Map<String, Object>> data = DB.getMasterDB(db -> {
			return db.select("accounts", "name, password", "Where dbId=" + dbId);
		});
		
		if (data.isEmpty())
			return null;
		
		return Account.load((String) data.get(0).get("name"), (String) data.get(0).get("password"));				
	}
}
