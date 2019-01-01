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
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.joda.time.DateTime;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.ihsinformatics.gfatm.integration.cad4tb.model.XRayOrder;
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

	private static final Logger log = Logger.getLogger(Cad4tbImportService.class);

	private DatabaseUtil dbUtil;
	private OpenmrsMetaService openmrs;

	private int cad4tbUserId;
	private int fetchDurationHours;
	private int fetchDelay = 100;
	private String baseUrl = null;
	private String username = null;
	private String password = null;
	private String archiveName = null;
	private String projectName = null;

	public Cad4tbImportService() {
	}

	/**
	 * Initialize properties, database, and other stuff
	 */
	public void initialize(Properties properties) {
		// Initiate properties
		fetchDurationHours = Integer.parseInt(properties.getProperty("cad4tb.fetch_duration_hours", "120"));
		fetchDelay = Integer.parseInt(properties.getProperty("cad4tb.fetch_delay", "100"));
		baseUrl = properties.getProperty("cad4tb.url");
		username = properties.getProperty("cad4tb.username");
		password = properties.getProperty("cad4tb.password");
		archiveName = properties.getProperty("cad4tb.archive");
		projectName = properties.getProperty("cad4tb.project");
		//
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
		// Get CAD4TB user from properties and set mapping ID by searching in OpenMRS
		Object userId = dbUtil.runCommand(CommandType.SELECT,
				"select user_id from users where username = '" + username + "'");
		if (userId != null) {
			cad4tbUserId = Integer.parseInt(userId.toString());
			return;
		}
		cad4tbUserId = Integer.parseInt(properties.getProperty("cad4tb.openmrs.user_id"));
		openmrs = new OpenmrsMetaService(dbUtil);
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
		processOrders(openmrs.getXRayOrders(start.toDate(), end.toDate()));
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
			processOrders(openmrs.getXRayOrders(start.toDate(), end.toDate()));
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
		processOrders(openmrs.getXRayOrders(start.toDate(), end.toDate()));
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
	public void processOrders(List<XRayOrder> xrayOrders) throws MalformedURLException, InstantiationException,
			IllegalAccessException, ClassNotFoundException, SQLException {
		for (XRayOrder order : xrayOrders) {
			XRayResult xRayResult = getXrayResultFromApi(order.getPatientIdentifier(), order.getEncounterDatetime());
			if (xRayResult == null) {
				continue;
			}
			xRayResult.setOrderId(order.getOrderId());
			try {
				boolean success = processResult(xRayResult, order);
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
	public boolean processResult(XRayResult xRayResult, XRayOrder xrayOrder)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		// Skip if the Patient ID scheme does not match\
		if (!Cad4tbMain.DEBUG_MODE && !xRayResult.getPatientId().matches(Constant.PATIENT_ID_REGEX)) {
			log.warn("Patient ID " + xRayResult.getPatientId() + " is invalid!");
			return false;
		}
		// Fetch patient ID against the given identifier in CXR test
		log.info("Searching for Patient...");
		if (dbUtil == null) {
			log.error("Database has not been initialized. Exiting sadly :-(");
			System.exit(-1);
		}
		Date dateCreated = xRayResult.getTestResultDate();
		// If another XRay result is found with same order ID, then skip
		if (openmrs.xrayResultExists(xrayOrder.getPatientGeneratedId(), xRayResult.getOrderId())) {
			log.warn("Record already exists against this Result: " + xRayResult.toString());
			return false;
		}
		return openmrs.saveXrayResult(xrayOrder.getPatientGeneratedId(), xrayOrder.getLocationId(), cad4tbUserId,
				dateCreated, xRayResult);
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
	public XRayResult getXrayResultFromApi(String patientId, Date orderDate)
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
				if (xray.getCad4tbScore() < Constant.NORMAL_CUTOFF_SCORE) {
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
}
