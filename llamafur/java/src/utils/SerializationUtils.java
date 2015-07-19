package utils;

import it.unimi.di.archive4j.Archive;
import it.unimi.di.archive4j.RandomAccessBitstreamArchive;
import it.unimi.dsi.util.Properties;
import it.unimi.dsi.webgraph.ImmutableGraph;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Scanner;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SerializationUtils {
	public static Logger LOGGER = LoggerFactory.getLogger(SerializationUtils.class);
	
	public static String noExtension(String name) {
		int pointIndex = StringUtils.lastIndexOf(name, '.');
		if (pointIndex == -1) return name;
		else return name.substring(0, pointIndex);
	}
	
	public static void saveSafe(Object objectToSave, String path) {
		Scanner scanner = new Scanner(System.in);
		boolean saved = false;
		do {
			try {
				save(objectToSave, path);
				saved = true;
			} catch (IOException e) {
				e.printStackTrace();
				System.err.println("The selected path (" + path + ")" +
						" has resulted in error. Please try another one.");
				path = scanner.nextLine();
			}
		} while (!saved);
		scanner.close();
	}
	
	public static void save(Object objectToSave, String path) throws IOException {
		File file = new File(path);
		if (file.exists())
			throw new IOException("File "+path+" already exists.");
		ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(file));
		stream.writeObject(objectToSave);
		stream.close();
	}
	
	
	public static File serializeTmp(Object objectToSave, String prefix) throws IOException {
		File tmp = File.createTempFile(prefix, ".ser.tmp");
		tmp.deleteOnExit();
		ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(tmp));
		stream.writeObject(objectToSave);
		stream.close();
		return tmp;
	}
	
	public static Object read(String path) throws IOException, ClassNotFoundException {
		if (path.endsWith(".graph"))
			return readGraph(path);
		else if (path.endsWith(".archive"))
			return readArchive4j(path);
		
		File file = new File(path);
		LOGGER.info("Reading " + file.getName() + "...");
		ObjectInputStream stream = new ObjectInputStream(new FileInputStream(file));
		Object result = stream.readObject();
		stream.close();
		LOGGER.info(file.getName() + " read.");
		return result;
	}
	
	public static Archive<?> readArchive4j(String path) throws IOException {
		LOGGER.info("Reading " + new File(path).getName() + " as an archive of Archive4j...");
		String archiveBasename = noExtension(path);
		Archive<?> archive;
		try {
			archive = RandomAccessBitstreamArchive.getInstance(archiveBasename, 
					new Properties(archiveBasename + ".properties"), null);
		} catch (ClassNotFoundException e) {
			throw new IOException(e);
		} catch (ConfigurationException e) {
			throw new IOException(e);
		}
		LOGGER.info("Archive read.");
		return archive;
	}

	public static ImmutableGraph readGraph(String path) throws IOException {
		LOGGER.info("Reading " + new File(path).getName() + " as a BVGraph...");
		ImmutableGraph graph = ImmutableGraph.load(noExtension(path));
		LOGGER.info("Graph read.");
		return graph;
	}
}
