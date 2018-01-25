package com.ihsinformatics.gfatm.integration.connection;

import java.io.InputStream;
import java.util.Properties;
import org.apache.log4j.Logger;
import com.ihsinformatics.gfatm.integration.shared.AppManager;
import com.ihsinformatics.util.DatabaseUtil;

/**
 * @author Shujaat
 *
 */

public class AppInitializer {
	
	private static final Logger log = Logger.getLogger(AppInitializer.class);
	private static final String PROP_FILE_NAME = "gfatm-crm-integration.properties";
	private static Properties prop;
	private String baseUrl;
	private String apiKey;
	private String userForm;
	private DatabaseUtil dbUtil;
	private String userId;

	
	public AppInitializer() {
		
		try{
			InputStream inputStream = Thread.currentThread()
					.getContextClassLoader()
					.getResourceAsStream(PROP_FILE_NAME);
			prop = new Properties();
			prop.load(inputStream);	
		} catch (Exception e) {
			log.error(e.getMessage());
			System.exit(-1);
		}
	}
	
	public boolean dbInitializer(){
		
		String dbUsername = prop.getProperty("connection.username");
		String dbPassword = prop.getProperty("connection.password");
		String url = prop.getProperty("connection.url");
		String dbName = prop
				.getProperty("connection.default_schema");
		String driverName = prop
				.getProperty("connection.driver_class");
		dbUtil = new DatabaseUtil(url, dbName, driverName, dbUsername,
				dbPassword);
		if(!dbUtil.tryConnection()){
			return false;
		  }
		baseUrl = prop.getProperty("callcenter.baseUrl");
		apiKey = prop.getProperty("callcenter.authKey");
		userForm = prop.getProperty("callcenter.userForm");
		userId = prop.getProperty("callcenter.userId");
		setValue();
		
    return true;
	}
	
	public void setValue(){
		//The properties fields introduce to the application manager.
		AppManager.getInstance().setDatabaseUtil(dbUtil);
		AppManager.getInstance().setAuthKey(apiKey);
		AppManager.getInstance().setBaseUrl(baseUrl);
		AppManager.getInstance().setProps(prop);
		AppManager.getInstance().setUserForm(userForm);
		AppManager.getInstance().setUserId(userId);		
	}
}
