package com.ihsinformatics.gfatm.integration.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.UUID;

import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONObject;

import com.ihsinformatics.gfatm.integration.shared.Constant;
import com.ihsinformatics.gfatm.integration.shared.AppManager;
import com.ihsinformatics.util.CommandType;
import com.ihsinformatics.util.DatabaseUtil;
import com.ihsinformatics.util.DateTimeUtil;

/**
 * @author Shujaat 
 *
 */

public class ServerService {

	private Date dateEntered;
	private String userId;
	private DatabaseUtil dbUtil;
	private static ServerService instance = null;
	private static final Logger log = Logger.getLogger(ServerService.class);
	private Connection con;
	public static final DateFormat dformat = new SimpleDateFormat(
			DateTimeUtil.SQL_DATETIME);
	public static final SimpleDateFormat format = new SimpleDateFormat(
			DateTimeUtil.SQL_DATETIME);

	public ServerService() {
		this.dbUtil = AppManager.getInstance().getDatabaseUtil();
		this.userId = AppManager.getInstance().getUserId();
	}

	public static ServerService getInstance() {

		if (instance == null) {
			instance = new ServerService();
		}
		return instance;
	}

	/**
	 * 
	 * @param url
	 * @param authKey
	 * @return
	 */
	public JSONArray httpRequest(String url, String authKey)throws IOException {
		
		
		HttpsURLConnection httpConnection;
		//if you don't need the self sign certificate then kindly comment line of code.
		HttpsURLConnection.setDefaultHostnameVerifier(getCertificate());
		URL obj = new URL(url);
		httpConnection = (HttpsURLConnection) obj.openConnection();
		httpConnection.setRequestMethod("GET");
		httpConnection.setRequestProperty("Accept", "application/json");
		httpConnection.setRequestProperty("Content-Type", "text/html; charset=UTF-8");
		httpConnection.setRequestProperty("Authorization", "Basic " + authKey);
		httpConnection.setDoOutput(true);
		int responseCode = httpConnection.getResponseCode();
		System.out.println("\nSending 'GET' request to URL : " + url);
		if (responseCode == HttpURLConnection.HTTP_OK) {
			InputStream inputStream = httpConnection.getInputStream();
			InputStreamReader inputStreamReader = new InputStreamReader(
					inputStream);
			BufferedReader in = new BufferedReader(inputStreamReader);
			String inputLine;
			StringBuffer responseBuffer = new StringBuffer();
			while ((inputLine = in.readLine()) != null) {
				responseBuffer.append(inputLine);
			}
			in.close();
			httpConnection.disconnect();
			JSONObject jsonObj = new JSONObject(responseBuffer.toString());
			JSONArray returnJsonArray = jsonObj.getJSONArray("data");
			return returnJsonArray;
		}else
			return null;
	}

	private HostnameVerifier getCertificate() {

		// self signed certificate used
		TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {
			public java.security.cert.X509Certificate[] getAcceptedIssuers() {
				return null;
			}

			public void checkClientTrusted(X509Certificate[] certs,
					String authType) {
			}

			public void checkServerTrusted(X509Certificate[] certs,
					String authType) {
			}
		} };

		// Install the all-trusting trust manager
		SSLContext sc;
		try {
			sc = SSLContext.getInstance("SSL");
			sc.init(null, trustAllCerts, new java.security.SecureRandom());
			HttpsURLConnection
					.setDefaultSSLSocketFactory(sc.getSocketFactory());
		} catch (NoSuchAlgorithmException | KeyManagementException e2) {
			e2.printStackTrace();
		}
		// Create all-trusting host name verifier
		HostnameVerifier allHostsValid = new HostnameVerifier() {
			public boolean verify(String hostname, SSLSession session) {
				return true;
			}
		};

		return allHostsValid;
	}

	public String saveUserForm(JSONObject dataObject) throws SQLException,
			InstantiationException, IllegalAccessException,
			ClassNotFoundException {
     String errorMessage ="";
		try {
			dateEntered = dformat.parse(dataObject.getString("date_entered"));
		} catch (ParseException e) {
			e.printStackTrace();
		}
		StringBuilder query = new StringBuilder(
				"INSERT INTO  user_form (user_form_id,user_form_type_id ,user_id, duration_seconds, date_entered ,date_created,created_by ,created_at ,date_changed , changed_by , changed_at , uuid) VALUES ");
		query.append("(0,'" + Constant.ccCrmUserFormType);
		query.append("','" + userId);
		query.append("'," + null);
		query.append(",'" + DateTimeUtil.toSqlDateString(dateEntered));
		query.append("',current_timestamp()");
		query.append(",'" + userId);
		query.append("'," + null);
		query.append("," + null);
		query.append("," + null);
		query.append("," + null);
		query.append(",'" + dataObject.getString("uuid") + "')");
		
		Integer userFormId =-1;
		try {
			userFormId = insertData(query.toString());
		} catch (SQLException | InstantiationException | 
				IllegalAccessException | ClassNotFoundException e) {
			errorMessage = e.getMessage();
		}
		if (userFormId == -1)
			return "ERROR : "+errorMessage;
		else
			return saveUserFormResult(userFormId, dataObject);
	}

	public String saveUserFormResult(int userFormId, JSONObject data)
			throws InstantiationException, IllegalAccessException,
			ClassNotFoundException, SQLException {
		String resultMsg = "";
		// too be continue
		JSONArray names = data.names();
		for (int i = 0; i < names.length(); i++) {
			String value = "";
			String name = names.getString(i);
			Integer elementId;
		 if (name.equals("manual_call_detail")) {
				JSONArray manualCallDetails = data
						.getJSONArray("manual_call_detail");
				if (manualCallDetails.length()>=1) {
					
						for (int j = 0; j < manualCallDetails.length(); j++) {
		
							JSONObject manual = manualCallDetails.getJSONObject(j);
							JSONArray manualCallAtrNames = manual.names();
		
							for (int k = 0; k < manualCallAtrNames.length(); k++) {
								String ManualCallNames = manualCallAtrNames.getString(k);
								elementId = getElementByName(ManualCallNames);
								if (elementId != -1) {
									value = manual.isNull(ManualCallNames) ? "Null"
											: manual.getString(ManualCallNames)
													.toString();
									saveResult(userFormId, elementId, value);// save the userResultForm
								} else {
									log.error("Not saved :" + ManualCallNames + ": "
											+ value);
								}
							}
						}
				}
			} else {

				if (name.equals("user_id")) {
					elementId = getElementByName("CRM_USER_ID");
				} else {
					elementId = getElementByName(name);
				}
				if (elementId != -1) {
						if (!name.equals("source_of_referral"))
							value = data.isNull(name) ? "Null" : data.getString(name).toString();
						else
							value = data.isNull(name) ? "Null" : data.getJSONArray("source_of_referral").toString();
						
						resultMsg = saveResult(userFormId, elementId, value);// insert
																		
				} else {
					log.warn("Not saved :" + name + ": " + value);
				}
			}
		}
		return resultMsg;
	}

	/**
	 * Get the last date from gfatm database, get the maximum date_entered.
	 * @return
	 * @throws SQLException
	 */
	public String getLastEnteredDate() throws SQLException {
		String lastDate = "";
		String query = "SELECT MAX(date(date_entered)) as date_entered FROM user_form where user_form_type_id =9";

		try {
			con = dbUtil.getConnection();
			PreparedStatement ps = con.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				lastDate = rs.getString("date_entered");
			}
			rs.close();
			ps.close();
			con.close();
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException | SQLException e) {
			con.close();
		}

		return lastDate;
	}

	/**
	 * data log method is used here to save the error data into gfatm database.
	 * this errors are insert into dataLog table.
	 * @param obj
	 * @param description
	 * @throws SQLException
	 */
	public void datalog(JSONObject obj, String description) throws SQLException {

		StringBuilder query = new StringBuilder(
				"INSERT INTO data_log (log_id,log_type,entity_name,record,description,date_created,created_by,created_at,uuid) VALUES ");
		query.append("(0,'" + CommandType.ALTER);
		query.append("','"
				+ AppManager.getInstance().getUserForm());
		query.append("','" + obj.toString() + "'");
		query.append(",'" + description + "'");
		query.append(",current_timestamp()");
		query.append(",'" + userId);
		query.append("',2");
		query.append(",'" + UUID.randomUUID().toString() + "')");
		try {
			insertData(query.toString());
		} catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	private Integer getElementByName(String elementName) throws SQLException {

		Integer elementId = -1;
		String query = "SELECT distinct element_id as element_id FROM gfatm.element where element_name ='"+elementName+"'";
		try {
		    Connection	con = dbUtil.getConnection();
			PreparedStatement ps = con.prepareStatement(query);
			ResultSet rs = ps.executeQuery();
			while (rs.next()) {
				elementId = rs.getInt("element_id");
			}
			rs.close();
			ps.close();
			con.close();
		} catch (InstantiationException | IllegalAccessException
				| ClassNotFoundException | SQLException e) {
			e.printStackTrace();
		}
		return elementId;
	}

	private String saveResult(Integer userFormId, Integer elementId,
			String result) throws SQLException {

		String responseDetail = "";
		String errorMessage = "";
		Integer userFormResultId = -1;
		StringBuilder query = new StringBuilder(
				"INSERT INTO user_form_result (user_form_result_id,user_form_id, element_id, result , date_created , created_by , created_at, date_changed , changed_at , uuid) VALUES ");
		query.append("(0,'" + userFormId);
		query.append("'," + elementId);
		query.append(",'" + result);
		query.append("','" + DateTimeUtil.toSqlDateString(new Date()));
		query.append("','" + userId);
		query.append("'," + null);
		query.append("," + null);
		query.append("," + null);
		query.append(",'" + UUID.randomUUID().toString() + "')");
		
		try {
			userFormResultId = insertData(query.toString());

		} catch (SQLException | InstantiationException | IllegalAccessException | ClassNotFoundException e) {
			errorMessage = e.getMessage();
		} finally {
			if (userFormResultId != -1) {
				responseDetail = "SUCCESS";
			} else {
				responseDetail = "ERROR:" + errorMessage;
			}
		}
		return responseDetail;
	}

	private Integer insertData(String query) throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {

		Integer userFormResultId = -1;
			Connection con = dbUtil.getConnection();
			PreparedStatement ps = con.prepareStatement(query);
			ps.executeUpdate(query.toString(), Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) {
				userFormResultId = rs.getInt(1);
				log.info("Inserted : "+userFormResultId);
			}
			rs.close();
			ps.close();
			con.close();
		return userFormResultId;
	}

}
