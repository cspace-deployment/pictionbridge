package edu.berkeley.cspace.pictionbridge.monitor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.sql.DataSource;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;

import edu.berkeley.cspace.pictionbridge.update.Update;
import edu.berkeley.cspace.pictionbridge.update.UpdateAction;
import edu.berkeley.cspace.pictionbridge.update.UpdateRelationship;

abstract class CSpaceRowMapper implements RowMapper<Update> {
	private String defaultRelationshipValue;
	
	CSpaceRowMapper(String defaultRelationshipValue) {
		this.defaultRelationshipValue = defaultRelationshipValue;
	}
	
	public String getDefaultRelationshipValue() {
		return defaultRelationshipValue;
	}
}

public class DatabaseUpdateMonitor implements UpdateMonitor {
	private static final Logger logger = LogManager.getLogger(DatabaseUpdateMonitor.class);
	
	private static final String BINARY_DIR = "binaries";
	
	private Integer limit;
	private String defaultRelationshipValue;
	private String workPath;
	private String interfaceTable;
	private String logTable;
	
	private DataSource dataSource;
	private JdbcTemplate jdbcTemplate;
	
	public DatabaseUpdateMonitor() {

	}
	
	private void createWorkDirectories() {
		Path binaryPath = FileSystems.getDefault().getPath(getWorkPath(), BINARY_DIR);
		
		try {
			Files.createDirectories(binaryPath);
		} catch (IOException e) {
			logger.fatal("failed to create work directory " + binaryPath, e);
			
			throw(new UpdateMonitorException(e));
		}
	}

	@Override
	public boolean hasUpdates() {
		return (getUpdateCount() > 0);
	}
	
	@Override
	public int getUpdateCount() {
		return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + getInterfaceTable() + " WHERE dt_processed IS NULL", Integer.class);
	}

	@Override
	public List<Update> getUpdates(String defaultRelationshipValue) {
		Integer limit = getLimit();
		String sql = "SELECT id, piction_id, filename, mimetype, img_size, img_height, img_width, object_csid, object_number, action, relationship, dt_addedtopiction, dt_uploaded, bimage, sha1_hash, website_display_level FROM " + getInterfaceTable() + " WHERE (dt_uploaded > dt_processed) OR (dt_processed IS NULL) ORDER BY dt_uploaded";
		
		if (limit != null) {
			sql += " LIMIT " + limit.toString();
		}

		logger.debug("executing query: " + sql);

		List<Update> updates = jdbcTemplate.query(
			sql,
			new CSpaceRowMapper(defaultRelationshipValue) {
				public Update mapRow(ResultSet results, int rowNum) throws SQLException {
					Update update = new Update();
					
					update.setId(results.getLong(1));
					update.setPictionId(results.getInt(2));
					update.setFilename(results.getString(3));
					update.setMimeType(results.getString(4));
					update.setImgSize(results.getInt(5));
					update.setImgHeight(results.getInt(6));
					update.setImgWidth(results.getInt(7));
					update.setObjectCsid(results.getString(8));
					update.setObjectNumber(results.getString(9));
					
					String actionString = results.getString(10);
					UpdateAction action = null;
					try {
						action = UpdateAction.valueOf(actionString);
					} catch (IllegalArgumentException e) {
						logger.warn("update " + update.getId() + " has unknown action " + actionString);
					}
					update.setAction(action);

					//
					// Check the config to see if we have a default relationship to use if one is not supplied.
					//
					String relationshipString = results.getString(11);
					if (relationshipString == null || relationshipString.trim().isEmpty()) {
						relationshipString = getDefaultRelationshipValue();
					}
					//
					// Ensure the relationship value is valid
					//
					UpdateRelationship relationship = null;
					try {
						relationship = UpdateRelationship.valueOf(relationshipString);
					} catch(IllegalArgumentException e) {
						logger.warn("update " + update.getId() + " has unknown relationship value " + relationshipString);
					}
					update.setRelationship(relationship);

					update.setDateTimeAddedToPiction(results.getTimestamp(12));
					update.setDateTimeUploaded(results.getTimestamp(13));
					update.setBinaryFile(extractBinary(results.getBinaryStream(14), update));
					update.setHash(results.getString(15));
					update.setWebsiteDisplayLevel(results.getString(16));
					
					logger.info("found update\n" + update.toString());
					
					return update;
				}
			}
		);
		
		return updates;
	}
	
	@Override
	public void markUpdateComplete(Update update) {
		logger.debug("marking update " + update.getId() + " complete");
	
		int rowsAffected = jdbcTemplate.update("UPDATE " + getInterfaceTable() + " SET dt_processed = LOCALTIMESTAMP WHERE id = ?", Long.valueOf(update.getId()));

		if (rowsAffected != 1) {
			logger.warn("marking update " + update.getId() + " complete affected " + rowsAffected + " rows");
		}
	}
	
	@Override
	public void setObjectCSID(Update update) {
		logger.debug("setting object CSID " + update.getId() + " complete");
	
		int rowsAffected = jdbcTemplate.update("UPDATE " + getInterfaceTable() + " SET object_csid = ? WHERE id = ?",
				update.getObjectCsid(),
				Long.valueOf(update.getId()));

		if (rowsAffected != 1) {
			logger.warn("setting object CSID for update " + update.getId() + " complete affected " + rowsAffected + " rows");
		}
	}

	@Override
	public void deleteBinary(Update update) {
		logger.debug("deleting binary of update " + update.getId());
		
		int rowsAffected = jdbcTemplate.update("UPDATE " + getInterfaceTable() + " SET bimage = NULL WHERE id = ?", Long.valueOf(update.getId()));

		if (rowsAffected != 1) {
			logger.warn("deleting binary of update " + update.getId() + " affected " + rowsAffected + " rows");
		}
	}

	@Override
	public void deleteUpdate(Update update) {
		logger.debug("deleting update " + update.getId());

		int rowsAffected = jdbcTemplate.update("DELETE FROM " + getInterfaceTable() + " WHERE id = ?", Long.valueOf(update.getId()));
		
		if (rowsAffected != 1) {
			logger.warn("deletion of update " + update.getId() + " affected " + rowsAffected + " rows");
		}
	}

	@Override
	public void logUpdate(Update update) {
		logger.debug("logging update " + update.getId());
		
		int rowsAffected = jdbcTemplate.update("INSERT INTO " + getLogTable() + " SELECT * FROM " + getInterfaceTable() + " WHERE id = ?", Long.valueOf(update.getId()));

		if (rowsAffected != 1) {
			logger.warn("log of update " + update.getId() + " affected " + rowsAffected + " rows");
		}
	}
	
	private String getBinaryFilename(Update update) {
		return update.getFilename();
	}
	
	private Path getUpdateWorkDir(Update update) {
		return FileSystems.getDefault().getPath(getWorkPath(), BINARY_DIR, Long.toString(update.getId())).toAbsolutePath();
	}
	
	private File extractBinary(InputStream in, Update update) {
		Path dir = getUpdateWorkDir(update);
		
		try {
			Files.createDirectories(dir);
		} catch (IOException e) {
			logger.fatal("failed to create work directory " + dir, e);
			
			throw(new UpdateMonitorException(e));
		}
	
		File file = new File(dir.toFile(), getBinaryFilename(update));
		
		if (in == null) {
			if (file.exists()) {
				logger.warn("binary for update " + update.getId() + " is null -- using previously extracted file");
			}
			else {
				logger.error("binary not found for update " + update.getId());
				file = null;
			}
		}
		else {
			logger.debug("extracting binary for update " + update.getId() + " to " + file.getPath());
			
			try {
				if (file.exists()) {
					logger.warn("binary file exists and will be overwritten: " + file.getPath());
				}
				else {
					file.createNewFile();
				}
				
				FileOutputStream out = new FileOutputStream(file);
				
				int bytesCopied = IOUtils.copy(in, out);
					
				in.close();
				out.close();
				
				if (update.getImgSize() != bytesCopied) {
					logger.warn("binary for update " + update.getId() + " has incorrect size: expected " + update.getImgSize() + ", found " + bytesCopied);
				}
			}
			catch(IOException e) {
				logger.error("error extracting binary for update " + update.getId(), e);
				return null;
			}
		}
		
		return file;
	}
	
	@Override
	public String getDefaultRelationshipValue() {
		return defaultRelationshipValue;
	}
	
	@Override
	public void setDefaultRelationshipValue(String defaultRelationshipValue) {
		this.defaultRelationshipValue = defaultRelationshipValue;
	}

	@Override
	public void setLimit(Integer limit) {
		this.limit = limit;
	}

	@Override
	public Integer getLimit() {
		return limit;
	}
	
	public String getWorkPath() {
		return workPath;
	}

	public void setWorkPath(String workPath) {
		this.workPath = workPath;
		
		createWorkDirectories();
	}
	
	public String getInterfaceTable() {
		return interfaceTable;
	}

	public void setInterfaceTable(String interfaceTable) {
		if (!interfaceTable.matches("[A-Za-z][A-Za-z0-9_\\.]*")) {
			throw new IllegalArgumentException("illegal table name: " + interfaceTable);
		}
		
		this.interfaceTable = interfaceTable;
	}

	public String getLogTable() {
		return logTable;
	}

	public void setLogTable(String logTable) {
		if (!logTable.matches("[A-Za-z][A-Za-z0-9_\\.]*")) {
			throw new IllegalArgumentException("illegal table name: " + logTable);
		}

		this.logTable = logTable;
	}

	public DataSource getDataSource() {
		return dataSource;
	}

	public void setDataSource(DataSource dataSource) {
		this.dataSource = dataSource;
		this.jdbcTemplate = new JdbcTemplate(dataSource);
	}
}