package utils;

import java.sql.Date;
import java.sql.Time;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.gson.Gson;

import utils.DB.Account;
import utils.DB.DB;

/**
 * This class represents the abstract Job part of the {@link WorkerService}.
 * You need to extend this class to get a fully functional Job. This enables the child class
 * to get consumed by the {@link WorkerService}. Make sure to test in your child class whether the {@link #stop}
 * attribute is set. If you don't stop working, under the condition that stop == true, there is no guarantee
 * the serialization to the DB will be successful. Also make sure to test in short intervals, since the worker
 * waits just 1000ms for the thread to end.
 * 
 * Implements the {@link Runnable} interface for threading capability.
 * 
 * @author Christian Chartron
 *
 */
public abstract class Job implements Runnable {
	public enum State { New, Running, Suspended, Done }
	
	volatile protected boolean stop = false;
	
	volatile private State state;
	private int dbId = -1;
	private String eMail;
	private String Operation;
	private String savePath;	
	private Account account;
	
	public Account getAccount() { return account; }
	public State getState() { return state; }
	public String getEMail() { return eMail; }
	public String getSavePath() { return savePath; }
	public int getDbId() { return dbId; }
	public void stop() { stop = true; }
	
	protected void setDbId(int dbId) { this.dbId = dbId; }
	protected void setEMail(String eMail) { this.eMail  = eMail; }
	protected void setSavePath(String path) { savePath = path.replace("\\", "/"); }
			
	protected Job(Account acc) {
		account = acc;
		
		Operation = this.getClass().getName();
	}

	/**
	 * Loads the next pending {@link Job} from DB. The read {@link #Operation} is used to find a constructor for
	 * deserialization. This constructor has a signature of (int, Account, String, Map).
	 * 
	 * @return Returns null if no {@link Job} is pending.
	 */
	public static Job loadNextPending() {
		
		Map<String, Object> data;
		data = DB.getMasterDB(db -> {
			List<Map<String,Object>> select = db.select("workerjobs", "*",
					"Where lockedUntil <= now() AND doneTime is NULL");
			
			return select.isEmpty() ? new HashMap<String, Object>() : select.get(0);
		});
		
		if (data.isEmpty())
			return null;
		
		int dbId = (int) data.get("dbId");
		Account rem = Account.load((int) data.get("remitter"));
		String mail = (String) data.get("mail");		

		@SuppressWarnings("unchecked")
		Map<String, Object> args = new Gson().fromJson((String) data.get("arguments"),
				HashMap.class);
		Job job = null;
		try {
			job = (Job) Class.forName((String) data.get("operation"))
					.getConstructor(int.class, Account.class, String.class, Map.class)
					.newInstance(dbId, rem, mail, args);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		job.lockUntil(new Time(600000));
		
		return job;
	}
	
	/**
	 * Use this method to run the {@link Job} for a given {@link Duration}. The {@link Job} gets suspended to 
	 * DB if it doesn't finish in the given Timespan. There is another 1000ms wait for the Job to end itself.
	 * @param duration The given run time for this Job.
	 */
	public void run(Duration duration) {
		try {
			Thread worker = new Thread(this);
			if (state == State.Done)
				return;
			
			worker.start();
			worker.join(duration.toMillis());
			
			if (!worker.isAlive())
				return;
			else {
				stop = true;
				worker.join(1000);
				if (worker.isAlive()) {
					worker.interrupt();
					suspend();
				}
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Locks the {@link Job} and prevent it to get loaded before the 'time' is elapsed.
	 * @param time Renews the Lock for the given Time.
	 */
	public void lockUntil(Time time) {
		DB.getMasterDB(db -> {
			Date lock = new Date(Calendar.getInstance().getTimeInMillis() + time.getTime());
			String strLock = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(lock);
			
			Map<String, Object> data = new HashMap<String, Object>() {
				{
					put("lockedUntil", strLock);
				}
			};
			db.update("workerjobs", "dbId=" + dbId, data);
		});
	}	
	
	/**
	 * Extended from {@link Runnable} interface. Use this method to let this Job run without time limitations.
	 * The Job doesn't get serialized to DB. 
	 */
	public void run() {
		try {
			if (state == State.Done)
				return;
			
			state = State.Running;
			runInternal();

			if (!stop)
				done();				
			else if (stop)
				suspend();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * Inserts a new entry to workerjobs table or updates the entry if it is already present.
	 */
	private void suspend() {
		
		try {
			DB.getMasterDB(db -> {
				Date lock = new Date(Calendar.getInstance().getTimeInMillis() + 60000);
				String strLock = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(lock);
								
				if (dbId == -1) {
					Map<String, Object> data = new HashMap<String, Object>() {
						{
							put("remitter", account.getDbId());
							put("Operation", Operation);
							put("arguments", new Gson().toJson(getSerializiationData()));
							put("mail", account.getMail());
							put("lockedUntil", strLock);
						}
					};
					db.insert("workerjobs", data);
				}
				else
				{
					Map<String, Object> data = new HashMap<String, Object>() {
						{
							put("arguments", new Gson().toJson(getSerializiationData()));
							put("lockedUntil", strLock);
						}
					};					
					db.update("workerjobs", "dbId=" + dbId, data);
				}
			});
			
			state = State.Suspended;

			System.out.println("Job: " + (dbId != -1 ? Integer.toString(dbId) : "local") + " is SUSPENDED.");
		}
		catch(Exception e) {
			System.out.println(e.toString());
		}
	}
	
	/**
	 * Updates the doneTime column in workerjobs table. Doesn't do any clean up since this should do
	 * the administrator on maintenance.
	 */
	private void done() {
		if (dbId != -1 && state != State.Done){		
			DB.getMasterDB(db -> {
				Date now = new Date(Calendar.getInstance().getTimeInMillis());
				String strNow = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(now);
				Map<String, Object> data = new HashMap<String, Object>() {
					{
						put("doneTime", strNow);
					}
				};			
				db.update("workerjobs", "dbId=" + dbId, data);
			});
			state = State.Done;
		}		
		
		System.out.println("Job: " + (dbId != -1 ? Integer.toString(dbId) : "local") + " is DONE.");		
	}
	
	/**
	 * Has to be implemented by the child class. Here is the actual work done. Make sure to test in short
	 * intervals if the {@link stop} attribute is set.
	 **/
	protected abstract void runInternal();
	
	/**
	 * Serializes the Job to DB. Doesn't execute the Job.
	 */
	public void runInBackground() {
		if (state == State.Running) {
			System.out.println("The Job is already Running. Can't runInBackground()");
			return;
		}
		suspend();
	}
	
	/**
	 * Has to be implemented by the child class. Make sure to do all necessary actions for serialization (eg.
	 * saving data). 
	 * @return Returns a Map of String, Object which represents the needed data for serialization. 
	 */
	protected abstract Map<String, Object> getSerializiationData();
}
