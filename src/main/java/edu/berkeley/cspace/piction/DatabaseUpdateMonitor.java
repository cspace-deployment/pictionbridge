package edu.berkeley.cspace.piction;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class DatabaseUpdateMonitor implements UpdateMonitor {
	private static final Logger logger = LogManager.getLogger(DatabaseUpdateMonitor.class);
	
	private static final String JDBC_DRIVER = "org.postgresql.Driver";
	
	public static final String DEFAULT_HOST = "dba-postgres-dev-32.ist.berkeley.edu";
	public static final int    DEFAULT_PORT = 5113;
	public static final String DEFAULT_DATABASE = "bampfa_domain_bampfa";
	public static final String DEFAULT_TABLE = "piction.piction_interface";
	public static final String DEFAULT_WORK_PATH = "/tmp/piction-bridge";
	
	public static final String ENV_VAR_HOST = "PICTION_DB_HOST";
	public static final String ENV_VAR_PORT = "PICTION_DB_PORT";
	public static final String ENV_VAR_DATABASE= "PICTION_DB_NAME";
	public static final String ENV_VAR_TABLE = "PICTION_DB_TABLE";
	public static final String ENV_VAR_USER = "PICTION_DB_USER";
	public static final String ENV_VAR_PASSWORD = "PICTION_DB_PW";
	
	public static final String WORK_PATH = "/tmp/piction-bridge";
	public static final String BINARY_DIR = "binaries";
	
	private static final int BUFFER_SIZE = 4096;
	
	private String host;
	private int port;
	private String database;
	private String table;
	
	private Connection connection;
	
	public DatabaseUpdateMonitor() {
		createWorkDirectories();
		
		Map<String, String> env = System.getenv();
		
		this.host = env.containsKey(ENV_VAR_HOST) ? env.get(ENV_VAR_HOST) : DEFAULT_HOST;		
		this.port = env.containsKey(ENV_VAR_PORT) ? Integer.parseInt(env.get(ENV_VAR_PORT)) : DEFAULT_PORT;
		this.database = env.containsKey(ENV_VAR_DATABASE) ? env.get(ENV_VAR_DATABASE) : DEFAULT_DATABASE;		
		this.table = env.containsKey(ENV_VAR_TABLE) ? env.get(ENV_VAR_TABLE) : DEFAULT_TABLE;
		
		String user = env.containsKey(ENV_VAR_USER) ? env.get(ENV_VAR_USER) : "";
		String password = env.containsKey(ENV_VAR_PASSWORD) ? env.get(ENV_VAR_PASSWORD) : "";
		
		connect(user, password);
	}
	
	private void createWorkDirectories() {
		Path binaryPath = FileSystems.getDefault().getPath(WORK_PATH, BINARY_DIR);
		
		try {
			Files.createDirectories(binaryPath);
		} catch (IOException e) {
			logger.error("failed to create work directory " + binaryPath, e);
			
			throw(new UpdateMonitorException(e));
		}
	}
	
	private void connect(String user, String password) {
		logger.debug("registering JDBC driver " + JDBC_DRIVER);
		
		try {
			Class.forName(JDBC_DRIVER);
		} catch (ClassNotFoundException e) {
			logger.fatal("could not find JDBC driver " + JDBC_DRIVER, e);
			
			throw(new UpdateMonitorException(e));
		}
		
		String url = getDatabaseUrl();
		
		logger.info("connecting to database " + url);
				
		try {
			this.connection = DriverManager.getConnection(url, user, password);
		} catch (SQLException e) {
			logger.fatal("could not connect to database", e);
			
			throw(new UpdateMonitorException(e));
		}
	}
	
	public void close() {
		try {
			connection.close();
		} catch (SQLException e) {
			logger.warn("error closing connection", e);
		}
	}
	
	private String getDatabaseUrl() {
		return "jdbc:postgresql://" + this.host + ":" + this.port + "/" + this.database;
	}
	
	public boolean hasUpdates() {
		return (getUpdateCount() > 0);
	}

	public int getUpdateCount() {
		Integer count = query("SELECT COUNT(*) FROM " + table, new ResultProcessor<Integer>() {
			public Integer processResults(ResultSet results) throws SQLException {
				results.next();
				return results.getInt(1);
			}
		});
		
		return count;
	}

	public List<PictionUpdate> getUpdates() {
		return getUpdates(null);
	}
	
	public List<PictionUpdate> getUpdates(Integer limit) {
		String sql = "SELECT id, piction_id, filename, mimetype, img_size, img_height, img_width, object_csid, media_csid, blob_csid, action, relationship, dt_addedtopiction, dt_uploaded, bimage FROM " + table + " ORDER BY dt_uploaded";
		
		if (limit != null) {
			sql += " LIMIT " + limit.toString();
		}
		
		logger.debug("executing query: " + sql);
		
		List<PictionUpdate> updates = query(sql, new ResultProcessor<List<PictionUpdate>>() {
			public List<PictionUpdate> processResults(ResultSet results) throws SQLException {
				List<PictionUpdate> updates = new ArrayList<PictionUpdate>();
				
				while (results.next()) {
					String actionString = results.getString(11);
					UpdateAction action = null;
					
					try {
						action = UpdateAction.valueOf(actionString);
					}
					catch(IllegalArgumentException e) {
						logger.warn("skipping update with unknown action " + actionString);
						continue;
					}
					
					PictionUpdate update = new PictionUpdate();
					
					update.setId(results.getLong(1));
					update.setPictionId(results.getInt(2));
					update.setFilename(results.getString(3));
					update.setMimeType(results.getString(4));
					update.setImgSize(results.getInt(5));
					update.setImgHeight(results.getInt(6));
					update.setImgWidth(results.getInt(7));
					update.setObjectCsid(results.getString(8));
					update.setMediaCsid(results.getString(9));
					update.setBlobCsid(results.getString(10));
					update.setAction(action);
					update.setRelationship(results.getString(12));
					update.setDateTimeAddedToPiction(results.getTimestamp(13));
					update.setDateTimeUploaded(results.getTimestamp(14));
					update.setBinaryFile(extractBinary(results.getBinaryStream(15), update));

					logger.debug("found update\n" + update.toString());
					
					updates.add(update);
				}
				
				return updates;
			}
		});
		
		return updates;
	}

	private String getBinaryFilename(PictionUpdate update) {
		return Long.toString(update.getId());
	}
	
	private File extractBinary(InputStream in, PictionUpdate update) {
		File file = FileSystems.getDefault().getPath(WORK_PATH, BINARY_DIR, getBinaryFilename(update)).toFile();
		int bytesRead = 0;
		
		logger.debug("extracting binary for update " + update.getId() + " to " + file.getPath());
		
		try {
			if (file.exists()) {
				logger.warn("binary file " + file.getPath() + " exists and will be overwritten");
			}
			else {
				file.createNewFile();
			}
			
			FileOutputStream out = new FileOutputStream(file);
			
			byte[] buffer = new byte[BUFFER_SIZE];
			int length = 0;
	
			while ((length = in.read(buffer)) != -1) {
				bytesRead += length;
				out.write(buffer, 0, length);
			}
	
			in.close();
			out.close();
			
			if (update.getImgSize() != bytesRead) {
				logger.warn("binary for update " + update.getId() + " has incorrect size: expected " + update.getImgSize() + ", found " + bytesRead);
			}
		}
		catch(IOException e) {
			logger.error("error extracting binary for update " + update.getId(), e);
			return null;
		}
		
		return file;
	}
	
	private <T> T query(String sql, ResultProcessor<T> handler) {
		Statement statement = null;
		ResultSet results = null;
		T processedResults = null;
		
		try {
			statement =  connection.createStatement();
			results = statement.executeQuery(sql);
	
			if (results != null) {
				processedResults = handler.processResults(results);
			}			
		}
		catch(SQLException e) {
			logger.fatal("Error executing query", e);
			
			throw(new UpdateMonitorException(e));
		}
		finally {
			try {
				if (results != null) results.close();
			}
			catch(SQLException e) {
				logger.warn("error closing result", e);
			}
			
			try {
				if (statement != null) statement.close();
			}
			catch(SQLException e) {
				logger.warn("error closing statement", e);
			}
		}
		
		return processedResults;
	}
	
	private interface ResultProcessor<T> {
		public T processResults(ResultSet results) throws SQLException;
	}
}
