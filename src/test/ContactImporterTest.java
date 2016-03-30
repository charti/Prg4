package test;

import static org.junit.Assert.*;

import java.time.Duration;
import java.time.Period;

import org.junit.BeforeClass;
import org.junit.Test;

import utils.ContactImporter;
import utils.DB.Account;
import utils.DB.DB;
import utils.Job.State;

public class ContactImporterTest {
	
	private static Account acc = null;
	private static final String MOCK_FILE = "mock.csv";
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		if (acc == null)
			if ((acc = Account.load("Tester", "Testpw")) == null) {
				acc = new Account("Tester", "Testpw", "chr.chart@gmail.com");
				acc.commitChanges();
			}

		Helpers.deleteAllFrom("contacts");
	}

	@Test
	public void testContactImporter() {
		ContactImporter importer = new ContactImporter(acc, "chr.chart@gmail.com", MOCK_FILE);
		importer.run();
		
		long count = DB.getMasterDB(db -> {
			return (long) db.select("contacts", "Count(dbId)", "").get(0).get("Count(dbId)");
		});
		
		assertEquals(1999, count);
		
		Helpers.deleteAllFrom("contacts");
	}

}
