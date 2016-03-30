import java.sql.SQLException;
import java.time.Duration;

import utils.ContactImporter;
import utils.Job;
import utils.JobProducer;
import utils.WorkerService;
import utils.DB.Account;

public class Application {

	public static void main(String[] args) throws SQLException {
		try {
			for (int i = 1; i <= 4; i++) {
				Job job = new ContactImporter(Account.load("Tester", "Testpw"), "chr.chart@gmail.com",
						String.format("mock%d.csv", i));
				job.run(Duration.ofMillis(10000));
			}
			
			new WorkerService(new JobProducer());
		} catch (Exception e) { e.printStackTrace(); }
	}
}