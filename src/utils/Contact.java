package utils;

import java.util.List;
import java.util.Map;

import utils.DB.Account;
import utils.DB.DB;
import utils.DB.DbItem;

/**
 * Part of the example implementation. You can create a new Contact by using the constructor. You have to
 * call {@link #commitChanges()} to write the Contact to DB.
 * @author Christian Chartron
 */
public class Contact extends DbItem {	
	private String firstname;
	private String lastname;
	private String city;
	private String zipCode;
	private Account account = null;
	
	public String getFirstname() {
		return firstname;
	}

	public void setFirstname(String firstname) {
		this.firstname = firstname;
		cacheChange("firstname", firstname);
	}

	public String getLastname() {
		return lastname;
	}

	public void setLastname(String lastname) {
		this.lastname = lastname;
		cacheChange("lastname", lastname);
	}

	public String getCity() {
		return city;
	}

	public void setCity(String city) {
		this.city = city;
		cacheChange("city", city);
	}

	public String getZipCode() {
		return zipCode;
	}

	public void setZipCode(String zipCode) {
		this.zipCode = zipCode;
		cacheChange("zipCode", zipCode);
	}
	
	public Account getAccount() {
		return account;
	}

	public int getDbId() {
		return dbId;
	}

	public Contact(Account account, String firstname, String lastname, String city, String zipCode) 
			throws Exception {
		super("contacts");

		if (!account.isAuthenticated())
			throw new Exception("Account isn't authenticated.");		
		if ((Contact.load(account, firstname, lastname)) != null)
			throw new Exception("Contact excists already. Consider using Contact#load.");

		if (firstname.isEmpty())
			throw new IllegalArgumentException("firstname can't be empty.");
		if (lastname.isEmpty())
			throw new IllegalArgumentException("lastname can't be empty.");
		if (city.isEmpty())
			throw new IllegalArgumentException("city can't be empty.");
		if (zipCode.isEmpty())
			throw new IllegalArgumentException("zipCode can't be empty.");
		
		this.account = account;
		setFirstname(firstname);
		setLastname(lastname);
		setCity(city);
		setZipCode(zipCode);
		
		cacheChange("accId", account.getDbId());
	}
	
	/**
	 * Constructor used to load a {@link Contact} from DB.
	 */
	private Contact(Account account, int dbId) {
		super("contacts");
		
		try {
			List<Map<String, Object>> data = null;
			if (account.isAuthenticated())
				data = DB.getMasterDB(db -> {
					return db.select(getTable(), "*",
							String.format("Where accId=%s AND dbId=%s", account.getDbId(),dbId));
				});

			if (!data.isEmpty()) {
				firstname = (String) data.get(0).get("firstname");
				lastname = (String) data.get(0).get("lastname");
				city = (String) data.get(0).get("city");
				zipCode = (String) data.get(0).get("zipCode");
				this.dbId = dbId;
				this.account = account;
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Loads a Contact from DB.
	 * @param account You have to pass the {@link Account} which was used to create the Contact.
	 * @param firstname
	 * @param lastname
	 * @return
	 */
	public static Contact load(Account account, String firstname, String lastname) {
		int dbId = -1;
		
		try {
			dbId = DB.getMasterDB(db -> {
				List<Map<String, Object>> ret = db.select("contacts", "dbId",
					String.format("Where firstname='%s' AND lastname='%s' AND accId=%s", 
							firstname, lastname, account.getDbId()));				
				return ret.isEmpty() ? -1 : (int)ret.get(0).get("dbId");
				});
		}
		catch (Exception e) {
			System.out.println(e.toString());
		}
		
		Contact contact = new Contact(account, dbId);
		return contact.dbId > 0 ? contact : null;
	}
}
