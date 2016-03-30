package utils;

import java.util.Iterator;

/**
 * Produces the {@link Job}s for the {@link WorkerService}. Since this class only provides the ability to load
 * a {@link Job} and provide information about whether there is a next one or not, this class implements the
 * {@link Iterable} interface. Because there is the possibility of running multiple Producers, the next() method
 * is synchronized.
 * @author Christian Chartron
 *
 */
public class JobProducer implements Iterable<Job> {	
	@Override
	public Iterator<Job> iterator() {
		Iterator<Job> it = new Iterator<Job>() {
			private Job next = null;
			
		    private final Object lock = new Object();
			
			@Override
			public boolean hasNext() {
				synchronized (lock) {
					if (next == null)
						next = Job.loadNextPending();
					
					return next != null;
				}
			}

			@Override
			public Job next() {
				synchronized (lock) {
					if (next == null)
						next = Job.loadNextPending();						

					Job ret = next;
					next = null;
					
					return ret;
				}
			}
		};
		return it;
	}
	
}
