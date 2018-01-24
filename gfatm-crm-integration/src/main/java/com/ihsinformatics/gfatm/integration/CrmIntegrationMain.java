package com.ihsinformatics.gfatm.integration;

import java.io.IOException;
import java.sql.SQLException;
import java.util.regex.PatternSyntaxException;

import org.apache.log4j.Logger;
import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ihsinformatics.gfatm.integration.connection.AppInitializer;
import com.ihsinformatics.gfatm.integration.server.ServerService;
import com.ihsinformatics.gfatm.integration.shared.Constant;
import com.ihsinformatics.gfatm.integration.shared.CustomMessage;
import com.ihsinformatics.gfatm.integration.shared.AppManager;
import com.ihsinformatics.gfatm.integration.shared.ErrorType;

/**
 * @author Shujaat
 *
 */

public class CrmIntegrationMain {

	private String lastDate = "";
	private String currentDateStr = "";
	private LocalDate currentDate;
	private int days = 0;
	private static final Logger log = Logger
			.getLogger(CrmIntegrationMain.class);
	private int numberRowInsert = 0, numberRowNotInsert = 0;

	public static void main(String[] args) {

		AppInitializer appInitializer = new AppInitializer();
		appInitializer.readProperties();

		CrmIntegrationMain main = new CrmIntegrationMain();
		JSONObject obj = main.dateSlice();
		log.info("EndUpResult:- " + obj);
		System.exit(0);
	}
  
	/**
   * In this method we create a slice of dates, means the given url get data of seven days maximum  
   * @return {@link JSONObject}
   */
	public JSONObject dateSlice() {

		JSONObject returnVal = new JSONObject();
		try {
			currentDate = new LocalDate();
			lastDate = ServerService.getInstance().getLastDate();

			days = Days.daysBetween(new LocalDate(lastDate),
					new LocalDate(currentDate)).getDays();

			long numberIteration = days / Constant.maxDays;
			JSONArray jsonArray = new JSONArray();
			int exceedDays = 0;
			final String fixedCurrentDate = currentDate.toString();// fixed
			for (int i = 0; i <= numberIteration; i++) {
				if (days > Constant.maxDays) {
					exceedDays = days - Constant.maxDays;
					LocalDate fixedDate = currentDate.minusDays(exceedDays);
					currentDateStr = fixedDate.toString();
					returnVal = getRequestForObject(lastDate, currentDateStr);
					lastDate = currentDateStr;
					days = exceedDays;
				} else {
					returnVal = getRequestForObject(lastDate, fixedCurrentDate);
				}
				if (returnVal.length() > 0 || returnVal != null) {
					jsonArray.put(returnVal);
					returnVal = null;
				}
			}
			returnVal = new JSONObject();
			returnVal.put("Results", jsonArray);
		} catch (SQLException e) {
			log.error(e.getMessage());
			System.exit(-1);//in case of any error this should be shutdown the thread.
		}
		return returnVal;
	}

	public JSONObject getRequestForObject(String lastDate, String currentDate) {

		JSONObject jsonResponse = new JSONObject();
		JSONObject returnResponse = new JSONObject();
		JSONArray resultDataList = null;
		try {
				resultDataList = ServerService.getInstance().httpRequest(
						AppManager.getInstance().getBaseUrl()
						+ "?start_date=" + lastDate + "&end_date="
						+ currentDate,AppManager.getInstance().getAuthKey());	
				// In 'No record found' case against the date range, then we return respective message.
				if (resultDataList == null || resultDataList.length() == 0) {
						jsonResponse.put("response",CustomMessage.getErrorMessage(ErrorType.NO_DATA_RECEIVED));
						returnResponse = jsonResponse;
				} else {
					returnResponse = execute(resultDataList);
				}
		 } catch (IOException e1) {
				e1.printStackTrace();
		 } catch (JSONException e) {
				e.printStackTrace();
			}
		
	return returnResponse;
	}

	public JSONObject execute(JSONArray data) {

		JSONObject returnResponse = new JSONObject();
		String result = "";
		JSONObject object = null;
		for (int i = 0; i < data.length(); i++) {
			try {
				
				object = (JSONObject) data.get(i);
				if (object.isNull("uuid")) {
					result = CustomMessage.getErrorMessage(ErrorType.NULL_VALUE);
				} else
					result = ServerService.getInstance().saveUserForm(object);
				
			} catch (InstantiationException e) {
				result = e.getMessage();
			} catch (IllegalAccessException e) {
				result = e.getMessage();
			} catch (SQLException e) {
				result = e.getMessage();
			} catch (JSONException e) {
				result = e.getMessage();
				e.printStackTrace();
			} catch (PatternSyntaxException e) {
				result = e.getMessage();
				e.printStackTrace();
			} catch (ClassNotFoundException e) {
				result = e.getMessage();
				e.printStackTrace();
			} finally {
				try {
					JSONObject jsonResponse = new JSONObject();
					if (result.equalsIgnoreCase("SUCCESS")) {
						numberRowInsert++;
					} else {
						numberRowNotInsert++;
						jsonResponse.put("Index", i);
						jsonResponse
								.put("Lead ID", object.getString("lead_id"));
						jsonResponse.put("uuid", object.isNull("uuid") ? "NULL"
								: object.getString("uuid"));
						ServerService.getInstance().datalog(jsonResponse, result);
					}
				} catch (JSONException | SQLException e) {
					e.printStackTrace();
				}
			}
		}
		try {
			returnResponse.put("Number Of Row Inserted", numberRowInsert);
			returnResponse
					.put("Number Of Row Not Inserted", numberRowNotInsert);
		} catch (JSONException e) {
			e.printStackTrace();
		}
	return returnResponse;
	}

}
