package utils.DB;

import java.util.Map;

public interface ICommitable {
	void commitChanges();
	boolean hasUncommitedChanges();
}
