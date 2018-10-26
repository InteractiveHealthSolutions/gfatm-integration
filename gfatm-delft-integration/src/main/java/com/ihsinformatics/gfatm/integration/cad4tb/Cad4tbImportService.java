/* Copyright(C) 2018 Interactive Health Solutions, Pvt. Ltd.

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as
published by the Free Software Foundation; either version 3 of the License (GPLv3), or any later version.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program; if not, write to the Interactive Health Solutions, info@ihsinformatics.com
You can also access the license on the internet at the address: http://www.gnu.org/licenses/gpl-3.0.html

Interactive Health Solutions, hereby disclaims all copyright interest in this program written by the contributors.
*/

package com.ihsinformatics.gfatm.integration.cad4tb;

import java.net.MalformedURLException;
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
import java.util.concurrent.ThreadLocalRandom;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ihsinformatics.gfatm.integration.cad4tb.model.XRayResult;
import com.ihsinformatics.gfatm.integration.cad4tb.shared.Constant;
import com.ihsinformatics.gfatm.integration.cad4tb.shared.HttpUtil;
import com.ihsinformatics.util.CommandType;
import com.ihsinformatics.util.DatabaseUtil;
import com.ihsinformatics.util.DateTimeUtil;

/**
 * @author owais.hussain@ihsinformatics.com
 *
 */
public class Cad4tbImportService {

	private static final Double NORMAL_CUTOFF_SCORE = 70D;
	private static final Logger log = Logger.getLogger(Cad4tbImportService.class);

	private Properties properties;
	private DatabaseUtil dbUtil;

	private int cad4tbUserId;
	private int fetchDurationHours;
	private int fetchDelay = 100;
	private String baseUrl = null;
	private String username = null;
	private String password = null;
	private String archiveName = null;
	private String projectName = null;

	public Cad4tbImportService(Properties properties) {
		this.properties = properties;
		fetchDurationHours = Integer.parseInt(properties.getProperty("cad4tb.fetch_duration_hours", "120"));
		fetchDelay = Integer.parseInt(properties.getProperty("cad4tb.fetch_delay", "100"));
		baseUrl = properties.getProperty("cad4tb.url");
		username = properties.getProperty("cad4tb.username");
		password = properties.getProperty("cad4tb.password");
		archiveName = properties.getProperty("cad4tb.archive");
		projectName = properties.getProperty("cad4tb.project");
		initDatabase();
		setXRayUser();
	}

	/**
	 * Initialize database
	 */
	private void initDatabase() {
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
	private void setXRayUser() {
		String username = properties.getProperty("cad4tb.openmrs.username");
		Object userId = dbUtil.runCommand(CommandType.SELECT,
				"select user_id from users where username = '" + username + "'");
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
		query.append("select max(encounter_datetime) as max_date from encounter where encounter_type = ");
		query.append(Constant.XRAY_RESULT_ENCOUNTER_TYPE);
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
						+ Constant.XRAY_RESULT_ENCOUNTER_TYPE);
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
		// Fetch Encounters between start and end
		StringBuilder query = new StringBuilder();
		query.append(
				"select distinct e.patient_id, pid.identifier, e.location_id, e.encounter_datetime, e.date_created, ord.value_text as order_id from encounter as e ");
		query.append(
				"inner join obs as ord on ord.encounter_id = e.encounter_id and ord.voided = 0 and ord.concept_id = "
						+ Constant.ORDER_ID_CONCEPT + " ");
		query.append(
				"inner join patient_identifier as pid on pid.patient_id = e.patient_id and pid.identifier_type = 3 and pid.voided = 0 ");
		query.append("where e.voided = 0 and e.encounter_type = " + Constant.XRAY_ORDER_ENCOUNTER_TYPE + " ");
		if (Cad4tbMain.DEBUG_MODE) {
			// query.append("and e.patient_id in (select patient_id from patient_identifier
			// where identifier = '012345')");
			query.append("and timestampdiff(HOUR, e.date_created, current_timestamp()) <= " + fetchDurationHours);
		} else {
			query.append("and timestampdiff(HOUR, e.date_created, current_timestamp()) <= " + fetchDurationHours);
		}
		Object[][] xrayOrders = dbUtil.getTableData(query.toString());
		for (Object[] order : xrayOrders) {
			int k = 0;
			Integer patientId = Integer.parseInt(order[k++].toString());
			String patientIdentifier = order[k++].toString();
			Integer locationId = Integer.parseInt(order[k++].toString());
			Date encounterDatetime = DateTimeUtil.fromSqlDateTimeString(order[k++].toString());
			// Date dateCreated = DateTimeUtil.fromSqlDateTimeString(order[k++].toString());
			String orderId = order[k++].toString();
			if (Cad4tbMain.DEBUG_MODE) {
				String[] testIds = { "0", "00", "000" };
				int random = ThreadLocalRandom.current().nextInt(0, testIds.length + 1);
				patientIdentifier = testIds[random];
				encounterDatetime = DateTimeUtil.fromSqlDateTimeString("2017-09-23 00:00:00");
			}
			XRayResult xRayResult = getXrayResultByOrder(patientIdentifier, encounterDatetime);
			if (xRayResult == null) {
				continue;
			}
			xRayResult.setOrderId(orderId);
			try {
				boolean success = processResult(xRayResult, patientId, locationId);
				log.info("Imported result for " + xRayResult.getPatientId() + (success ? ": YES" : ": NO"));
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

	/**
	 * Takes a XRayResult object and stores into database after several validation
	 * checks
	 * 
	 * @param xRayResult
	 * @param locationId
	 * @return
	 * @throws InstantiationException
	 * @throws IllegalAccessException
	 * @throws ClassNotFoundException
	 * @throws SQLException
	 */
	public boolean processResult(XRayResult xRayResult, Integer patientId, Integer locationId)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		// Skip if the Patient ID scheme does not match\
		if (!Cad4tbMain.DEBUG_MODE && !xRayResult.getPatientId().matches(Constant.PATIENT_ID_REGEX)) {
			log.warn("Patient ID " + xRayResult.getPatientId() + " is invalid!");
			return false;
		}
		// Fetch patient ID against the given identifier in CXR test
		log.info("Searching for Patient...");
		if (dbUtil == null) {
			initDatabase();
		}
		DateTime dateCreated = new DateTime(xRayResult.getTestResultDate().getTime());
		long rows = -1;
		try {
			// If another XRay result is found with same order ID, then skip
			StringBuilder query = new StringBuilder();
			query.append("select count(*) from encounter as e ");
			query.append(
					"inner join obs as ord on ord.encounter_id = e.encounter_id and ord.voided = 0 and ord.concept_id = "
							+ Constant.ORDER_ID_CONCEPT + " ");
			query.append("where e.voided = 0 ");
			query.append("and e.encounter_type = " + Constant.XRAY_RESULT_ENCOUNTER_TYPE + " ");
			query.append("and e.patient_id = " + patientId + " ");
			query.append("and ord.value_text = '" + xRayResult.getOrderId() + "'");
			Object obj = dbUtil.runCommandWithException(CommandType.SELECT, query.toString());
			rows = Long.parseLong(obj.toString());
		} catch (SQLException e1) {
			// Do nothing
		}
		if (rows > 0) {
			log.warn("Record already exists against this Result: " + xRayResult.toString());
			return false;
		}
		return saveXrayResult(patientId, locationId, cad4tbUserId, dateCreated, xRayResult);
	}

	/**
	 * Fetch results from XRayResults using API. Business logic: The returned X-ray
	 * result is the one which was performed right after the orderDate. If there are
	 * multiple results for same date, then the latest one is picked
	 * 
	 * @param patientId,
	 * @return
	 * @throws MalformedURLException
	 * @throws JSONException
	 */
	public XRayResult getXrayResultByOrder(String patientId, Date orderDate)
			throws MalformedURLException, JSONException {
		try {
			StringBuilder params = new StringBuilder();
			params.append("Archive=" + archiveName);
			params.append("&PatientID=" + patientId);
			params.append("&Project=" + projectName);

			// First fetch patient studies
			String url = baseUrl + "patient/studies/list/?";
			String json = HttpUtil.httpsGet(url, params.toString(), username, password);
			JSONArray studies = new JSONArray(json);
			JSONObject study = studies.getJSONObject(0);

			// Now fetch series for this study
			params.append("&StudyInstanceUID=" + study.getString("StudyInstanceUID"));
			url = baseUrl + "patient/study/series/list/?";
			JSONArray serieses = new JSONArray(HttpUtil.httpsGet(url, params.toString(), username, password));
			JSONObject series = serieses.getJSONObject(0);

			// Finally retrieve the results for this series
			params.append("&SeriesInstanceUID=" + series.getString("SeriesInstanceUID"));
			params.append("&Type=" + Constant.TEST_TYPE);
			params.append("&Results=CAD4TB%205");
			url = baseUrl + "/series/results/?";
			JSONArray results = new JSONArray(HttpUtil.httpsGet(url, params.toString(), username, password));
			if (results.length() == 0) {
				return null;
			}
			List<XRayResult> xrays = new ArrayList<>();
			for (int i = 0; i < results.length(); i++) {
				JSONObject result = results.getJSONObject(i);
				result.put("PatientID", patientId);
				result.put("Study", study);
				result.put("Series", series);
				XRayResult xray = XRayResult.fromJson(result);
				// Keep only the X-rays done after the order date
				if (xray.getTestResultDate().before(orderDate)) {
					continue;
				}
				if (xray.getCad4tbScore() < NORMAL_CUTOFF_SCORE) {
					xray.setCad4tbScoreRange(Constant.NORMAL_SCORE_RANGE_CONCEPT);
					xray.setPresumptiveTbCase(Constant.NO_CONCEPT);
				} else {
					xray.setCad4tbScoreRange(Constant.ABNORMAL_SCORE_RANGE_CONCEPT);
					xray.setPresumptiveTbCase(Constant.YES_CONCEPT);
				}
				xrays.add(xray);
			}
			XRayResult result = null;
			// Return the latest X-ray in the group
			for (XRayResult xRayResult : xrays) {
				if (result == null) {
					result = xRayResult;
				} else if (xRayResult.getTestResultDate().after(result.getTestResultDate())) {
					result = xRayResult;
				}
			}
			return result;
		} catch (Exception e) {
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
		// Save Encounter
		StringBuilder query = new StringBuilder(
				"INSERT INTO encounter (encounter_id, encounter_type, patient_id, location_id, encounter_datetime, creator, date_created, uuid) VALUES ");
		query.append("(0," + Constant.XRAY_RESULT_ENCOUNTER_TYPE);
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

		// No execution in test mode
		// if (!Cad4tbMain.DEBUG_MODE)
		{
			Connection con = dbUtil.getConnection();
			PreparedStatement ps = con.prepareStatement(query.toString());
			ps.executeUpdate(query.toString(), Statement.RETURN_GENERATED_KEYS);
			ResultSet rs = ps.getGeneratedKeys();
			if (rs.next()) {
				encounterId = rs.getInt(1);
			}
			rs.close();
			ps.close();
		}

		Date obsDate = new Date();
		try {
			obsDate = xrayResult.getTestResultDate();
		} catch (Exception e) {
			log.error(e.getMessage());
		}

		// Query for XRay order ID
		if (xrayResult.getOrderId() != null) {
			query = getOrderIdQuery(patientId, encounterLocationId, cad4tbUserId, xrayResult, encounterId, obsDate);
			queries.add(query.toString());
		}

		// Query for XRay result observation
		query = getXRayResultQuery(patientId, encounterLocationId, cad4tbUserId, encounterId, obsDate, xrayResult);
		queries.add(query.toString());

		// Query for score range
		query = getXRayScoreRangeQuery(patientId, encounterLocationId, cad4tbUserId, encounterId, obsDate, xrayResult);
		queries.add(query.toString());

		// Query for presumptive decision
		query = getXRayPresumptiveTbQuery(patientId, encounterLocationId, cad4tbUserId, encounterId, obsDate,
				xrayResult);
		queries.add(query.toString());

		for (String q : queries) {
			try {
//				if (Cad4tbMain.DEBUG_MODE) {
//					log.info("Skipping query due to test mode." + q);
//					return true;
//				}
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

	public StringBuilder getOrderIdQuery(int patientId, int encounterLocationId, int cad4tbUserId, XRayResult xray,
			Integer encounterId, Date obsDate) {
		String queryPrefix = "INSERT INTO obs (obs_id,person_id,concept_id,encounter_id,obs_datetime,location_id,interpretation,value_text,comments,creator,date_created,voided,uuid) VALUES ";
		StringBuilder query = new StringBuilder(queryPrefix);
		query.append("(0," + patientId + "," + Constant.ORDER_ID_CONCEPT + "," + encounterId + ",");
		query.append("'" + DateTimeUtil.toSqlDateTimeString(obsDate) + "'," + encounterLocationId + ",NULL,");
		query.append("'" + xray.getOrderId() + "','Auto-saved by CAD4TB Integration Service.',");
		query.append(cad4tbUserId + ",");
		query.append("'" + DateTimeUtil.toSqlDateTimeString(new Date()));
		query.append("',0,'" + UUID.randomUUID().toString() + "')");
		return query;
	}

	public StringBuilder getXRayResultQuery(int patientId, int encounterLocationId, int cad4tbUserId,
			Integer encounterId, Date obsDate, XRayResult xray) {
		String queryPrefix = "INSERT INTO obs (obs_id,person_id,concept_id,encounter_id,obs_datetime,location_id,interpretation,value_numeric,comments,creator,date_created,voided,uuid) VALUES ";
		StringBuilder query = new StringBuilder(queryPrefix);
		query.append("(0," + patientId + "," + Constant.XRAY_RESULT_CONCEPT + "," + encounterId + ",");
		query.append("'" + DateTimeUtil.toSqlDateTimeString(obsDate) + "'," + encounterLocationId + ",NULL,");
		query.append(xray.getCad4tbScore() + ",'Auto-saved by CAD4TB Integration Service.',");
		query.append(cad4tbUserId + ",");
		query.append("'" + DateTimeUtil.toSqlDateTimeString(new Date()));
		query.append("',0,'" + UUID.randomUUID().toString() + "')");
		return query;
	}

	public StringBuilder getXRayScoreRangeQuery(int patientId, int encounterLocationId, int cad4tbUserId,
			Integer encounterId, Date obsDate, XRayResult xray) {
		String queryPrefix = "INSERT INTO obs (obs_id,person_id,concept_id,encounter_id,obs_datetime,location_id,interpretation,value_coded,comments,creator,date_created,voided,uuid) VALUES ";
		StringBuilder query = new StringBuilder(queryPrefix);
		query.append("(0," + patientId + "," + Constant.CAD4TB_SCORE_RANGE_CONCEPT + "," + encounterId + ",");
		query.append("'" + DateTimeUtil.toSqlDateTimeString(obsDate) + "'," + encounterLocationId + ",NULL,");
		query.append(xray.getCad4tbScoreRange() + ",'Auto-saved by CAD4TB Integration Service.',");
		query.append(cad4tbUserId + ",");
		query.append("'" + DateTimeUtil.toSqlDateTimeString(new Date()));
		query.append("',0,'" + UUID.randomUUID().toString() + "')");
		return query;
	}

	public StringBuilder getXRayPresumptiveTbQuery(int patientId, int encounterLocationId, int cad4tbUserId,
			Integer encounterId, Date obsDate, XRayResult xray) {
		String queryPrefix = "INSERT INTO obs (obs_id,person_id,concept_id,encounter_id,obs_datetime,location_id,interpretation,value_coded,comments,creator,date_created,voided,uuid) VALUES ";
		StringBuilder query = new StringBuilder(queryPrefix);
		query.append("(0," + patientId + "," + Constant.PRESUMPTIVE_TB_CXR_CONCEPT + "," + encounterId + ",");
		query.append("'" + DateTimeUtil.toSqlDateTimeString(obsDate) + "'," + encounterLocationId + ",NULL,");
		query.append(xray.getPresumptiveTbCase() + ",'Auto-saved by CAD4TB Integration Service.',");
		query.append(cad4tbUserId + ",");
		query.append("'" + DateTimeUtil.toSqlDateTimeString(new Date()));
		query.append("',0,'" + UUID.randomUUID().toString() + "')");
		return query;
	}
}
