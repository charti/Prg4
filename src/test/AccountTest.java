package test;


import static org.junit.Assert.*;

import org.hamcrest.core.AnyOf;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import junit.framework.Assert;
import utils.DB.Account;
import utils.DB.DB;

public class AccountTest {

	private static Account acc = null;
	
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
		if ((acc = Account.load("Tester", "Testpw")) == null) {
			acc = new Account("Tester", "Testpw", "chr.chart@gmail.com");
			acc.commitChanges();
		}
	}
	
	@Test
	public void testLoad() {
		Account loaded = Account.load("Tester", "Testpw");
		
		assertArrayEquals(
			new String[] { 
					Integer.toString(loaded.getDbId()),
					loaded.getMail(),
					Boolean.toString(loaded.isAuthenticated()),
					loaded.getName(),
					Boolean.toString(false)
				},
			new String[] { 
					Integer.toString(loaded.getDbId()),
					loaded.getMail(),
					Boolean.toString(loaded.isAuthenticated()),
					loaded.getName(),
					Boolean.toString(loaded.hasUncommitedChanges())
				}
		);
	}
	
	@Test
	public void testNewAccount() throws Exception {
		Account newAccount = new Account("NewAccount", "newAccountpw", "mail@somewhere.de");
		
		assertEquals(newAccount.getDbId(), -1);
		assertEquals(newAccount.isAuthenticated(), false);
		assertEquals(newAccount.getName(), "NewAccount");
		assertEquals(newAccount.getMail(), "mail@somewhere.de");
		assertEquals(newAccount.hasUncommitedChanges(), true);
	}
	
	@Test public void testCommitChanges() throws Exception {
		Account commitedAccount = new Account("UnitTest", "UnitTestPw", "unit@test.com");
		try {
			commitedAccount.commitChanges();
			assertTrue(commitedAccount.getDbId() != -1);
			assertFalse(commitedAccount.hasUncommitedChanges());
			assertTrue(commitedAccount.isAuthenticated());
		}
		catch(Exception e) {
			e.printStackTrace();
			fail("Something went wrong.");
		}
		finally {
			DB.getMasterDB(db -> {
				db.delete("accounts", String.format(
					"dbId=%s AND name='%s' AND mail='%s' AND password='%s'", commitedAccount.getDbId(),
					commitedAccount.getName(), commitedAccount.getMail(),"UnitTestPw")); });
		}
	}
}
