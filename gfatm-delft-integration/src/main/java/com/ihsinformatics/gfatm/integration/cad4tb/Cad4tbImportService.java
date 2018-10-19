/* Copyright(C) 2018 Interactive Health Solutions, Pvt. Ltd.

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as
published by the Free Software Foundation; either version 3 of the License (GPLv3), or any later version.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program; if not, write to the Interactive Health Solutions, info@ihsinformatics.com
You can also access the license on the internet at the address: http://www.gnu.org/licenses/gpl-3.0.html

Interactive Health Solutions, hereby disclaims all copyright interest in this program written by the contributors.
*/

package com.ihsinformatics.gfatm.integration.cad4tb;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import javax.net.ssl.HttpsURLConnection;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ihsinformatics.gfatm.integration.cad4tb.model.XRayResult;
import com.ihsinformatics.gfatm.integration.cad4tb.shared.Constant;
import com.ihsinformatics.util.CommandType;
import com.ihsinformatics.util.DatabaseUtil;
import com.ihsinformatics.util.DateTimeUtil;

/**
 * @author owais.hussain@ihsinformatics.com
 *
 */
public class Cad4tbImportService {

	private static final Logger log = Logger.getLogger(Cad4tbImportService.class);
	private static boolean testMode = false;

	private Properties properties;
	private DatabaseUtil dbUtil;

	private int cad4tbUserId;
	private int fetchDurationHours;
	private int fetchDelay = 100;
	private String baseUrl = null;
	private String username = null;
	private String password = null;

	public Cad4tbImportService(Properties properties) {
		this.properties = properties;
		fetchDurationHours = Integer.parseInt(properties.getProperty("cad4tb.fetch_duration_hours", "24"));
		fetchDelay = Integer.parseInt(properties.getProperty("cad4tb.fetch_delay", "100"));
		baseUrl = properties.getProperty("cad4tb.url");
		username = properties.getProperty("cad4tb.username");
		password = properties.getProperty("cad4tb.password");
		initDatabase();
		setXRayUser();
	}

	/**
	 * Initialize database
	 */
	public void initDatabase() {
		// Initiate properties
		String dbUsername = properties.getProperty("connection.username");
		String dbPassword = properties.getProperty("connection.password");
		String url = properties.getProperty("connection.url");
		String dbName = properties.getProperty("connection.default_schema");
		String driverName = properties.getProperty("connection.driver_class");
		dbUtil = new DatabaseUtil(url, dbName, driverName, dbUsername, dbPassword);
		if (!dbUtil.tryConnection()) {
			log.fatal("Unable to connect with database using " + dbUtil.toString());
			System.exit(0);
		}
	}

	/**
	 * Get CAD4TB user from properties and set mapping ID by searching in OpenMRS
	 */
	public void setXRayUser() {
		String username = properties.getProperty("cad4tb.openmrs.username");
		Object userId = dbUtil.runCommand(CommandType.SELECT,
				"select user_id from openmrs.users where username = '" + username + "'");
		if (userId != null) {
			cad4tbUserId = Integer.parseInt(userId.toString());
			return;
		}
		cad4tbUserId = Integer.parseInt(properties.getProperty("cad4tb.openmrs.user_id"));
	}

	/**
	 * Fetches last update date from Encounter table against
	 * {@code Constant.GXP_ENCOUNTER_TYPE}. If no results are there, then first date
	 * from Encounter table is picked.
	 * 
	 * @throws MalformedURLException
	 * @throws JSONException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws ParseException
	 * @throws SQLException
	 */
	public void importAuto() throws MalformedURLException, JSONException, InstantiationException,
			IllegalAccessException, ClassNotFoundException, ParseException, SQLException {
		StringBuilder query = new StringBuilder();
		query.append(
				"select ifnull(max(encounter_datetime), (select min(encounter_datetime) from encounter)) as max_date from encounter where encounter_type = ");
		query.append(Constant.XRAY_ENCOUNTER_TYPE);
		// Also restrict to the results entered by CAD4TB user
		query.append(" and ");
		query.append("creator = " + cad4tbUserId);
		String dateStr = dbUtil.getValue(query.toString());
		DateTime start = new DateTime().minusHours(fetchDurationHours);
		if (dateStr != null) {
			start = new DateTime(DateTimeUtil.fromSqlDateString(dateStr));
		}
		DateTime end = start.plusHours(fetchDurationHours);
		run(start, end);
	}

	/**
	 * Fetches last update date from Encounter table against
	 * {@code Constant.GXP_ENCOUNTER_TYPE} and for every date, new results are
	 * fetched and stored
	 * 
	 * @throws MalformedURLException
	 * @throws JSONException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws ParseException
	 * @throws SQLException
	 */
	public void importAll() throws MalformedURLException, JSONException, InstantiationException, IllegalAccessException,
			ClassNotFoundException, ParseException, SQLException {
		String dateStr = dbUtil.getValue(
				"select ifnull(max(encounter_datetime), (select min(encounter_datetime) from encounter)) as max_date from encounter where encounter_type = "
						+ Constant.XRAY_ENCOUNTER_TYPE);
		DateTime start = new DateTime(DateTimeUtil.fromSqlDateString(dateStr));
		DateTime end = start.plusDays(1);
		while (end.isBeforeNow()) {
			run(start, end);
			start = start.plusDays(1);
			end = end.plusDays(1);
		}
	}

	/**
	 * Fetch results of a specific date and import
	 * 
	 * @param start
	 * @throws MalformedURLException
	 * @throws JSONException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws ParseException
	 * @throws SQLException
	 */
	public void importForDate(DateTime start) throws MalformedURLException, JSONException, InstantiationException,
			IllegalAccessException, ClassNotFoundException, ParseException, SQLException {
		start = start.withHourOfDay(0).withMinuteOfHour(0).withSecondOfMinute(0);
		DateTime end = start.plusDays(1).minusSeconds(1);
		run(start, end);
	}

	/**
	 * This method fetches all GXP results entered between given range of dates and
	 * processes them
	 * 
	 * @param start
	 * @param end
	 * @throws MalformedURLException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws SQLException
	 */
	public void run(DateTime start, DateTime end) throws MalformedURLException, InstantiationException,
			IllegalAccessException, ClassNotFoundException, SQLException {
		// Fetch PatientIDs from Encounters between start and end
		String[] patientIds = { "0", "1", "2", "012345", "2239833", "1078617" };
		for (String patientId : patientIds) {
			List<XRayResult> results = getXrayResultsByPatientId(patientId);
			if (results == null) {
				continue;
			}
			for (XRayResult xrayResult : results) {
				try {
					processResult(xrayResult);
					try {
						Thread.sleep(fetchDelay);
					} catch (Exception e) {
						log.error(e.getMessage());
					}
				} catch (Exception e1) {
					log.error(e1.getMessage());
				}
			}
		}
	}

	/**
	 * Takes a XRayResult object and stores into database after several validation
	 * checks
	 * 
	 * @param xrayResult
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public void processResult(XRayResult xrayResult)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		// Skip if the Patient ID scheme does not match
		if (!xrayResult.getPatientId().matches(Constant.PATIENT_ID_REGEX)) {
			log.warn("Patient ID " + xrayResult.getPatientId() + " is invalid!");
			return;
		}
		// Fetch patient ID against the given identifier in GXP test
		log.info("Searching for Patient...");
		if (dbUtil == null) {
			initDatabase();
		}
		Object[] record = dbUtil.getRecord("openmrs.patient_identifier", "patient_id, location_id",
				"where identifier='" + xrayResult.getPatientId() + "'");
		Object patientIdentifier = record[0];
		if (patientIdentifier == null) {
			log.info("Patient ID " + xrayResult.getPatientId() + " not found.");
			return;
		}
		if (record[1] == null) {
			record[1] = 1;
		}
		Integer patientId = Integer.parseInt(patientIdentifier.toString());
		// Fetch Location ID from Deployment Short Name
		Integer encounterLocationId = Integer.parseInt(record[1].toString());
		DateTime dateCreated = new DateTime(xrayResult.getTestResultDate().getTime());
		// If an observation is found with same Cartridge ID, then continue
		StringBuilder filter = new StringBuilder();
		filter.append("where concept_id = " + Constant.CAD4TB_CONCEPT);
		filter.append(" and value_text = '" + xrayResult.getTestResultDate() + "'");
		// Search for all GX Test forms with missing results
		long rows = -1;
		try {
			rows = dbUtil.getTotalRows("openmrs.obs", filter.toString());
		} catch (SQLException e1) {
			// Do nothing
		}
		if (rows > 0) {
			log.warn("Record already exists against this Result: " + xrayResult.toString());
			return;
		}
		boolean success = saveXrayResult(patientId, encounterLocationId, cad4tbUserId, dateCreated, xrayResult);
		log.info("Imported result for " + xrayResult.getPatientId() + (success ? ": YES" : ": NO"));
	}

	/**
	 * Searches for latest result in a given array
	 * 
	 * @param results
	 * @return
	 * @throws ParseException
	 * @throws JSONException
	 */
	public JSONObject searchLatestResult(JSONArray results) throws ParseException, JSONException {
		JSONObject latest = null;
		Date latestDate = new DateTime().minusYears(99).toDate();
		for (int i = 0; i < results.length(); i++) {
			JSONObject currentObj = results.getJSONObject(i);
			// Look for testStartedOn variable
			String startedOnStr = currentObj.getString("testStartedOn");
			Date startedOn = null;
			try {
				startedOn = DateTimeUtil.fromString(startedOnStr, "yyyy-MM-dd'T'HH:mm:ss");
			} catch (Exception e) {
				startedOn = DateTimeUtil.fromString(startedOnStr, "yyyy-MM-dd'T'HH:mm:ss'Z'");
			}
			if (startedOn.after(latestDate)) {
				latestDate = startedOn;
				latest = currentObj;
			}
		}
		return latest;
	}

	/**
	 * Fetch results from XRayResults using API
	 * 
	 * @param patientId
	 * @return
	 * @throws MalformedURLException
	 * @throws JSONException
	 */
	public List<XRayResult> getXrayResultsByPatientId(String patientId) throws MalformedURLException, JSONException {
		try {
			StringBuilder params = new StringBuilder();
			params.append("Archive=" + Constant.ARCHIVE_NAME);
			params.append("&PatientID=" + patientId);
			params.append("&Project=" + Constant.CAD4TB_API_PROJECT_NAME);

			// First fetch patient studies
			String url = baseUrl + "patient/studies/list/?" + params.toString();
			JSONArray studies = new JSONArray(httpsGet(url));
			JSONObject study = studies.getJSONObject(0);

			// Now fetch series for this study
			params.append("&StudyInstanceUID=" + study.getString("StudyInstanceUID"));
			url = baseUrl + "patient/study/series/list/?" + params.toString();
			JSONArray serieses = new JSONArray(httpsGet(url));
			JSONObject series = serieses.getJSONObject(0);

			// Finally retrieve the results for this series
			params.append("&SeriesInstanceUID=" + series.getString("SeriesInstanceUID"));
			params.append("&Type=" + Constant.TEST_TYPE);
			params.append("&Results=CAD4TB%205");
			url = baseUrl + "/series/results/?" + params.toString();
			JSONArray results = new JSONArray(httpsGet(url));
			if (results.length() == 0) {
				return null;
			}
			List<XRayResult> xrays = new ArrayList<>();
			for (int i = 0; i < results.length(); i++) {
				JSONObject result = results.getJSONObject(i);
				result.put("Study", study);
				result.put("Series", series);
				XRayResult xray = jsonToXrayResult(result);
				// Set the remaining parameters
				xray.setPatientId(patientId);
				if (xray.getCad4tbScore() < 70D) {
					xray.setCad4tbScoreRange(Constant.NORMAL_SCORE_RANGE_CONCEPT);
					xray.setPresumptiveTbCase(Constant.NO_CONCEPT);
				} else {
					xray.setCad4tbScoreRange(Constant.ABNORMAL_SCORE_RANGE_CONCEPT);
					xray.setPresumptiveTbCase(Constant.YES_CONCEPT);
				}
				xrays.add(xray);
			}
			return xrays;
		} catch (Exception e) {
		}
		return null;
	}

	private XRayResult jsonToXrayResult(JSONObject json) throws ParseException {
		XRayResult xray = new XRayResult();
		JSONObject study = json.getJSONObject("Study");
		JSONObject series = json.getJSONObject("Series");
		xray.setStudyId(study.getString("StudyInstanceUID"));
		xray.setSeriesId(series.getString("StudySeriesUID"));
		xray.setCad4tbScore(json.getDouble("value"));
		xray.setPatientId(json.getString("PatientID"));
		String modifiedDate = series.getString("modified");
		xray.setTestResultDate(DateTimeUtil.fromString(modifiedDate, DateTimeUtil.detectDateFormat(modifiedDate)));
		return xray;
	}

	public String httpsGet(String urlAddress) throws MalformedURLException {
		URL url = new URL(urlAddress);
		try {
			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("X-USERNAME", username);
			conn.setRequestProperty("X-PASSWORD", password);
			conn.setDoOutput(true);

			// Read the response
			int responseCode = conn.getResponseCode();
			if (responseCode != HttpURLConnection.HTTP_OK) {
				System.out.println("Response code: " + responseCode);
			}
			InputStream inputStream = conn.getInputStream();
			InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
			BufferedReader br = new BufferedReader(inputStreamReader);
			StringBuilder response = new StringBuilder();
			String str = null;
			while ((str = br.readLine()) != null) {
				response.append(str);
			}
			conn.disconnect();
			return response.toString();
		} catch (MalformedURLException e) {
		} catch (IOException e) {
		}
		return null;
	}

	/**
	 * This lengthy method converts X-Ray Results in JSON format into SQL queries
	 * and saves in DB. If you have any refactoring ideas, you're welcome to do so
	 * 
	 * @param encounterId
	 * @param patientId
	 * @param dateEncounterCreated
	 * @param encounterLocationId
	 * @param geneXpertResult
	 * @return
	 * @throws Exception
	 */
	public boolean saveXrayResult(int patientId, int encounterLocationId, int cad4tbUserId,
			DateTime dateEncounterCreated, XRayResult xrayResult)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		List<String> queries = new ArrayList<String>();
		Integer encounterId = saveXrayResultEncounter(patientId, encounterLocationId, cad4tbUserId,
				dateEncounterCreated);
		StringBuilder query;
		Date obsDate = new Date();
		try {
			obsDate = xrayResult.getTestResultDate();
		} catch (Exception e) {
			log.error(e.getMessage());
		}
		String insertQueryPrefix = "INSERT INTO openmrs.obs (obs_id,person_id,concept_id,encounter_id,obs_datetime,location_id,interpretation,value_coded,value_datetime,value_numeric,value_text,comments,creator,date_created,voided,uuid) VALUES ";

		if (xrayResult.getOrderId() != null) {
			query = getOrderIdQuery(patientId, encounterLocationId, cad4tbUserId, xrayResult, encounterId, obsDate,
					insertQueryPrefix);
			queries.add(query.toString());
		}

		// Query for XRay result observation
		query = getXRayResultQuery(patientId, encounterLocationId, cad4tbUserId, encounterId, obsDate,
				insertQueryPrefix, xrayResult.getCad4tbScore());
		queries.add(query.toString());
		for (String q : queries) {
			try {
				if (testMode) {
					log.info("Skipping query due to test mode." + q);
					return true;
				}
				dbUtil.runCommandWithException(CommandType.INSERT, q);
			} catch (Exception e) {
				StringBuilder message = new StringBuilder();
				message.append("Query failed: ");
				message.append(query);
				message.append("\r\nException: ");
				message.append(e.getMessage());
				log.error(message.toString());
			}
		}
		return true;
	}

	public StringBuilder getXRayResultQuery(int patientId, int encounterLocationId, int cad4tbUserId,
			Integer encounterId, Date obsDate, String insertQueryPrefix, Double mtbResult) {
		boolean error = mtbResult.equals("ERROR");
		StringBuilder query = new StringBuilder(insertQueryPrefix);
		query.append("(0," + patientId + "," + Constant.XRAY_RESULT_CONCEPT + "," + encounterId + ",");
		query.append("'" + DateTimeUtil.toSqlDateTimeString(obsDate) + "'," + encounterLocationId + ",NULL,");
		if (error) {
			query.append(Constant.ERROR_CONCEPT + ",");
		} else {
			query.append(Constant.ERROR_CODE_CONCEPT + ",");
		}
		query.append("NULL,NULL,NULL,'Auto-saved by CAD4TB Integration Service.',");
		query.append(cad4tbUserId + ",");
		query.append(
				"'" + DateTimeUtil.toSqlDateTimeString(new Date()) + "',0,'" + UUID.randomUUID().toString() + "')");
		return query;
	}

	public StringBuilder getOrderIdQuery(int patientId, int encounterLocationId, int cad4tbUserId, XRayResult xray,
			Integer encounterId, Date obsDate, String queryPrefix) {
		StringBuilder query = new StringBuilder(queryPrefix);
		query.append("(0," + patientId + "," + Constant.ERROR_CODE_CONCEPT + "," + encounterId + ",");
		query.append("'" + DateTimeUtil.toSqlDateTimeString(obsDate) + "'," + encounterLocationId + ",NULL,NULL,NULL,");
		query.append(xray.getOrderId() + ",NULL,'Auto-saved by CAD4TB Integration Service.',");
		query.append(cad4tbUserId + ",");
		query.append(
				"'" + DateTimeUtil.toSqlDateTimeString(new Date()) + "',0,'" + UUID.randomUUID().toString() + "')");
		return query;
	}

	/**
	 * This method saves the Encounter part of GeneXpertResult
	 * 
	 * @param patientId
	 * @param encounterLocationId
	 * @param cad4tbUserId
	 * @param dateEncounterCreated
	 * @return
	 * @throws SQLException
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 */
	public Integer saveXrayResultEncounter(int patientId, int encounterLocationId, int cad4tbUserId,
			DateTime dateEncounterCreated)
			throws SQLException, InstantiationException, IllegalAccessException, ClassNotFoundException {
		StringBuilder query = new StringBuilder(
				"INSERT INTO openmrs.encounter (encounter_id, encounter_type, patient_id, location_id, encounter_datetime, creator, date_created, uuid) VALUES ");
		query.append("(0," + Constant.XRAY_ENCOUNTER_TYPE);
		query.append("," + patientId);
		query.append("," + encounterLocationId);
		query.append(",'" + DateTimeUtil.toSqlDateString(dateEncounterCreated.toDate()));
		query.append("'," + cad4tbUserId);
		query.append(",current_timestamp()");
		query.append(",'" + UUID.randomUUID().toString() + "')");
		Integer encounterId = -1;
		// Yes. I know this makes little sense, but if you have any better
		// ideas to fetch encounter ID of the record just inserted, then be
		// my guest. And please try executeUpdate Statement yourself before
		// making "that" suggestion
		Connection con = dbUtil.getConnection();
		PreparedStatement ps = con.prepareStatement(query.toString());
		ps.executeUpdate(query.toString(), Statement.RETURN_GENERATED_KEYS);
		ResultSet rs = ps.getGeneratedKeys();
		if (rs.next()) {
			encounterId = rs.getInt(1);
		}
		rs.close();
		ps.close();
		return encounterId;
	}
}
