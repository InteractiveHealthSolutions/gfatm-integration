package com.ihsinformatics.gfatm.integration.shared;

import com.ihsinformatics.util.DatabaseUtil;
import java.util.Properties;

public class AppManager {
   private static AppManager instance =null;
   
	private DatabaseUtil databaseUtil;
	private Properties props;
	private String baseUrl;
	private String authKey;
	private String userForm;
	private String userId;

	public AppManager() {
	}
	
	public static AppManager getInstance(){
		
		if (instance == null) 
			instance = new AppManager();	
		return instance;	
	}
		
	public DatabaseUtil getDatabaseUtil() {
		return databaseUtil;
	}
	public void setDatabaseUtil(DatabaseUtil databaseUtil) {
		this.databaseUtil = databaseUtil;
	}
	public Properties getProps() {
		return props;
	}
	public void setProps(Properties props) {
		this.props = props;
	}
	public String getBaseUrl() {
		return baseUrl;
	}
	public void setBaseUrl(String baseUrl) {
		this.baseUrl = baseUrl;
	}
	public String getAuthKey() {
		return authKey;
	}
	public void setAuthKey(String authKey) {
		this.authKey = authKey;
	}

	public String getUserForm() {
		return userForm;
	}

	public void setUserForm(String userForm) {
		this.userForm = userForm;
	}

	public String getUserId() {
		return userId;
	}

	public void setUserId(String userId) {
		this.userId = userId;
	}
	

}
