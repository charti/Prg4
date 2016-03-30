package utils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import utils.DB.Account;

/**
 * Part of the example implementation. This class represents a {@link Job} and a {@link CsvImport}.
 * @author Christian Chartron
 *
 */
public class ContactImporter extends CsvImport {

	/**
	 * Deserialization constructor
	 */
	public ContactImporter(int dbId, Account rem, String mail, Map<String, Object> args) {
		super(rem, (String) args.get("savePath"));
		setSavePath((String) args.get("savePath"));
		this.currentLine = ((Double) args.get("currentLine")).intValue();
		setEMail(mail);
		this.setDbId(dbId);
	}
	
	/**
	 * Creates a new import {@link Job}
	 * @param acc The {@link Account} which is the creator of this import.
	 * @param eMail
	 * @param path File which should be imported.
	 */
	public ContactImporter(Account acc, String eMail, String path) {
		super(acc, path);
		
		this.setEMail(eMail);
	}

	@Override
	protected List<String> handleEntry(Map<String, String> entry) {
		List<String> ret = new ArrayList<String>();
		Contact contact = null;
				
		try {
			if((contact = Contact.load(getAccount(), entry.get("firstname"), entry.get("lastname")))
					== null) {
				contact = new Contact(getAccount(), entry.get("firstname"),
						entry.get("lastname"), entry.get("city"),
						entry.get("zipCode"));

				contact.commitChanges();
			}
		}
		catch(Exception e) {
			ret.add(e.toString());
		}
		
		return ret;
	}

	@Override
	protected Map<String, Object> getSerializiationData() {
		save();
		
		Map<String, Object> ret = new HashMap<String, Object>();
		ret.put("currentLine", this.currentLine);
		ret.put("savePath", this.getSavePath());		
		return ret;
	}	
}