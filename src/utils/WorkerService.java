package utils;

import java.io.IOException;
import java.io.InputStream;
import java.lang.Thread.State;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Scanner;

/**
 * This Service polls the {@link JobProducer} for pending {@link Job}s. It loads the MaxConsumer amount of
 * Jobs and execute them. The MaxConsumer amount can be set in the settings.config.
 * @author Christian Chartron
 *
 */
public class WorkerService {	
	private List<Thread> workers = new ArrayList<Thread>();
	private Iterator<Job> producer = null;
	private boolean stop = false;
	
	private int maxConsumer = 0;
	
	public WorkerService(JobProducer producer) {
		this.producer = producer.iterator();
		maxConsumer = Integer.parseInt(Settings.getSetting("MaxConsumer"));
		
		run();
	}
	
	private void run() {
		System.out.println("Any input ends the WorkerService");
		Scanner in = new Scanner(System.in);
		InputStream inp = System.in;
		
		while (!stop && timeout(5)) {
			cleanJobs();
			produceJobs();
			System.out.println(workers.size() + " Consumer running.");
			try {
				stop = (inp.available() > 0);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
	}
	
	private void cleanJobs() {
		if (workers.isEmpty())
			return;
		
		workers.removeIf(w -> {
			if (w.getState() == State.TERMINATED) {
				System.out.println(String.format("Worker: Consumer %s finished",
						workers.indexOf(w)));
				return true;
			}
			else
				return false;
			});
	}
	
	private void produceJobs() {			
		while (workers.size() < maxConsumer && producer.hasNext()) {
			Thread worker = new Thread(producer.next());
			workers.add(worker);
			
			worker.start();
			System.out.println(String.format("Worker: Consumer %s started",
					workers.indexOf(worker)));
		}		
	}
	
	private boolean timeout(int seconds) {
		try {
			Thread.currentThread().join(seconds * 1000);
		} catch (InterruptedException e) {
			System.out.println(String.format("Worker: Timeout error: %s", e.toString()));
		}
		return true;
	}
}
