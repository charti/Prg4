package utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.stream.Stream;

import javax.naming.spi.DirectoryManager;

import utils.*;
import utils.DB.Account;

/**
 * Part of the example implementation. This is an abstract base class for importing from csv files.
 * By extending this class you only need to implement {@link #handleEntry(Map)} and the 
 * {@link #getSerializiationData()} function. Also you will need an deserialization constructor.
 * @author Christian Chartron
 *
 */
public abstract class CsvImport extends Job {
	private List<String> head;
	private List<Map<String, String>> data;
	private boolean initilized = false;
	protected int currentLine = 0;

	protected List<String> getHead() {
		return head;
	}
	
	public boolean isInitilized() {
		return initilized;
	}
	
	protected CsvImport(Account acc, String path) {
		super(acc);
		
		readFile(path);
	}
	
	/**
	 * 
	 * @return A Map which represents an entry, specified by the first row of the parsed file, which is the 
	 * {@link #head}.
	 */
	protected Map<String, String> getEntryTemplate()
	{
		HashMap<String, String> template = new HashMap<String, String>();
		head.forEach(prop -> template.put(prop, ""));
		
		return template;
	}
	
	private void readHead(BufferedReader reader) throws IOException {
		head = Arrays.asList(reader.readLine().split(";"));		
	}
	
	private void readData(BufferedReader reader) throws IOException {
		String line = null;
		data = new ArrayList<Map<String, String>>();
				
		while ((line = reader.readLine()) != null) {
			Map<String, String> entry = new HashMap<String, String>();
			List<String> splitted = Arrays.asList(line.split(";"));
			
			for (String prop : head)
				entry.put(prop, splitted.get(head.indexOf(prop)));
			
			if (entry.size() != head.size())
				throw new IndexOutOfBoundsException(
						"readData() failed. Head- size and fetched entry size are not equal.");
			
			data.add(entry);
		}
	}
	
	private void readFile(String path) {
		FileReader fileInput= null;
		BufferedReader reader = null;
		
		try {
			fileInput = new FileReader(path);
			reader = new BufferedReader(fileInput);
			
			readHead(reader);
			readData(reader);
			
			initilized = true;
		}
		catch(IOException e){
			e.printStackTrace();
		}
		catch(IndexOutOfBoundsException e) {
			e.printStackTrace();
		}
		finally {
			try {
				if (reader != null) reader.close();
				if (fileInput != null) fileInput.close();
			}
			catch (IOException e) { e.printStackTrace(); }
		}
	}
	
	/**
	 * Saves the data to the BaseDataPath with a UUID as filename.
	 */
	protected void save() {
		File dir = new File(Settings.getSetting("BaseDataPath"));
		if (getSavePath() == null) { 
			if (!dir.exists()) {
				dir.mkdirs();				
			}
			
			String path = String.format("%s\\%s.csv",
					dir.getPath(),java.util.UUID.randomUUID());
			
			setSavePath(new File(path).getPath());
		}
		FileWriter writer = null;
		try {
			writer = new FileWriter(getSavePath());
			
			List<String> head = getHead();
			writer.write(String.join(";", head));
			writer.write("\n");
			
			for (Map<String, String> e : data) {
				List<String> sorted = new ArrayList<String>();
				for (String prop : head) {
					sorted.add(e.get(prop));
				}
				writer.write(String.join(";", sorted) + "\n");	
			}
		} catch (IOException e) { System.out.println(e.toString()); }
		finally {
			if (writer != null) {
				try {
					writer.close();
				}catch (IOException e) {
					e.printStackTrace();
				}
			}
		}		
	}
	
	@Override
	protected void runInternal() {
		if (isInitilized()) {
			int lineCount = 0;
			for (Map<String, String> entry : data) {
				if (stop)
					break;
				if (lineCount == currentLine) {
				List<String> fail = handleEntry(entry);
				writeFail(fail);
				}

				lineCount = lineCount + 1;
				this.currentLine = lineCount;
			}
		}
	}
	
	/**
	 * Writes the given fails to console.
	 * @param failed
	 */
	private void writeFail(List<String> failed) {
		if (failed.isEmpty())
			return;
		
		System.out.println("Fail Start");
		
		for (String fail : failed) {
			System.out.println(String.format("Line %s: '%s'", Integer.toString(currentLine), fail ));
		}

		System.out.println("Fail End");
	}
	
	/**
	 * This methods needs to be implemented by the special importer.
	 * It handles the import line- wise.
	 * @param entry One entry of data.
	 * @return Contains all failed properties and the reason for failing
	 */
	protected abstract List<String> handleEntry(Map<String, String> entry);
}
