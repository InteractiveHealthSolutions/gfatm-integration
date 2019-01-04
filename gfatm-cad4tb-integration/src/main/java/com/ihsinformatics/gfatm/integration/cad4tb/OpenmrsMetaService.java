/* Copyright(C) 2018 Interactive Health Solutions, Pvt. Ltd.

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as
published by the Free Software Foundation; either version 3 of the License (GPLv3), or any later version.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program; if not, write to the Interactive Health Solutions, info@ihsinformatics.com
You can also access the license on the internet at the address: http://www.gnu.org/licenses/gpl-3.0.html

Interactive Health Solutions, hereby disclaims all copyright interest in this program written by the contributors.
*/
package com.ihsinformatics.gfatm.integration.cad4tb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.log4j.Logger;

import com.ihsinformatics.gfatm.integration.cad4tb.model.XRayOrder;
import com.ihsinformatics.gfatm.integration.cad4tb.model.XRayResult;
import com.ihsinformatics.gfatm.integration.cad4tb.shared.Constant;
import com.ihsinformatics.util.CommandType;
import com.ihsinformatics.util.DatabaseUtil;
import com.ihsinformatics.util.DateTimeUtil;

/**
 * @author owais.hussain@ihsinformatics.com
 *
 */
public class OpenmrsMetaService {

	private static final Logger log = Logger.getLogger(OpenmrsMetaService.class);
	private DatabaseUtil dbUtil;

	public OpenmrsMetaService(DatabaseUtil dbUtil) {
		this.dbUtil = dbUtil;
	}

	@SuppressWarnings("deprecation")
	public boolean authenticate(String username, String password) {
		boolean result = false;
		try {
			if (username != null && password != null) {
				if (username.equals("") || password.equals("")) {
					return result;
				}
				String query = "select password, salt from openmrs.users inner join user_role using (user_id) where username = '"
						+ username + "' and retired = 0 and role in ('System Developer', 'Program Manager')";
				Object[][] data = dbUtil.getTableData(query);
				if (data != null) {
					String hash = data[0][0].toString();
					String salt = data[0][1].toString();
					if (com.ihsinformatics.util.SecurityUtil.hashMatches(hash, password + salt)) {
						result = true;
					}
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}

	/**
	 * Returns XRay orders as list of Object arrays from Encounters between given
	 * stard and end date
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	public List<XRayOrder> getXRayOrders(Date start, Date end) {
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
		query.append("and e.encounter_datetime between '" + DateTimeUtil.toSqlDateTimeString(start) + "' and '"
				+ DateTimeUtil.toSqlDateTimeString(end) + "'");
		Object[][] xrayOrders = dbUtil.getTableData(query.toString());
		List<XRayOrder> orders = new ArrayList<>();
		for (Object[] row : xrayOrders) {
			int k = 0;
			XRayOrder order = new XRayOrder();
			order.setPatientGeneratedId(Integer.parseInt(row[k++].toString()));
			order.setPatientIdentifier(row[k++].toString());
			order.setLocationId(Integer.valueOf(row[k++].toString()));
			order.setEncounterDatetime(DateTimeUtil.fromSqlDateString(row[k++].toString()));
			order.setDateCreated(DateTimeUtil.fromSqlDateTimeString(row[k++].toString()));
			order.setOrderId(row[k].toString());
			orders.add(order);
		}
		return filterDuplicates(orders);
	}

	/**
	 * Returns XRay orders as list of Object arrays from Encounters in given order
	 * date
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	public List<XRayOrder> getXRayOrders(String patientIdentifier, Date orderDate) {
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
		query.append("and datediff(e.encounter_datetime, '" + DateTimeUtil.toSqlDateString(orderDate) + "') <= 1 ");
		query.append("and pid.identifier = '" + patientIdentifier + "'");
		Object[][] xrayOrders = dbUtil.getTableData(query.toString());
		List<XRayOrder> orders = new ArrayList<>();
		for (Object[] row : xrayOrders) {
			int k = 0;
			XRayOrder order = new XRayOrder();
			order.setPatientGeneratedId(Integer.parseInt(row[k++].toString()));
			order.setPatientIdentifier(row[k++].toString());
			order.setLocationId(Integer.valueOf(row[k++].toString()));
			order.setEncounterDatetime(DateTimeUtil.fromSqlDateString(row[k++].toString()));
			order.setDateCreated(DateTimeUtil.fromSqlDateTimeString(row[k++].toString()));
			order.setOrderId(row[k].toString());
			orders.add(order);
		}
		return filterDuplicates(orders);
	}

	/**
	 * Filters all patients which have multiple orders in a date
	 * 
	 * @param orders
	 * @return
	 */
	public List<XRayOrder> filterDuplicates(List<XRayOrder> orders) {
		List<XRayOrder> list = new ArrayList<>();
		for (int i = 0; i < orders.size(); i++) {
			int count = 1;
			for (int j = 0; j < orders.size(); j++) {
				if (i != j && orders.get(i).equals(orders.get(j))) {
					count++;
					break;
				}
			}
			if (count == 1) {
				list.add(orders.get(i));
			}
		}
		return list;
	}

	/**
	 * Returns whether an X-ray result exists in existing data matching the orderId
	 * or not
	 * 
	 * @param patientId
	 * @param orderId
	 * @return
	 */
	public boolean xrayResultExists(Integer patientId, String orderId) {
		long rows = -1;
		try {
			StringBuilder query = new StringBuilder();
			query.append("select count(*) from encounter as e ");
			query.append(
					"inner join obs as ord on ord.encounter_id = e.encounter_id and ord.voided = 0 and ord.concept_id = "
							+ Constant.ORDER_ID_CONCEPT + " ");
			query.append("where e.encounter_type = " + Constant.XRAY_RESULT_ENCOUNTER_TYPE + " ");
			query.append("and e.patient_id = " + patientId + " ");
			query.append("and ord.value_text = '" + orderId + "'");
			Object obj = dbUtil.runCommandWithException(CommandType.SELECT, query.toString());
			rows = Long.parseLong(obj.toString());
		} catch (Exception e) {
		}
		return rows > 0;
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
	public boolean saveXrayResult(Integer patientId, Integer encounterLocationId, Integer cad4tbUserId,
			Date dateEncounterCreated, XRayResult xrayResult)
			throws InstantiationException, IllegalAccessException, ClassNotFoundException, SQLException {
		List<String> queries = new ArrayList<>();
		// Save Encounter
		StringBuilder query = new StringBuilder(
				"INSERT INTO encounter (encounter_id, encounter_type, patient_id, location_id, encounter_datetime, creator, date_created, uuid) VALUES ");
		query.append("(0," + Constant.XRAY_RESULT_ENCOUNTER_TYPE);
		query.append("," + patientId);
		query.append("," + encounterLocationId);
		query.append(",'" + DateTimeUtil.toSqlDateString(dateEncounterCreated));
		query.append("'," + cad4tbUserId);
		query.append(",current_timestamp(),uuid())");
		Integer encounterId = -1;
		// Yes. I know this makes little sense, but if you have any better
		// ideas to fetch encounter ID of the record just inserted, then be
		// my guest. And please try executeUpdate Statement yourself before
		// making "that" suggestion

		// No execution in test mode
		if (!Cad4tbMain.DEBUG_MODE) {
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
				if (!Cad4tbMain.DEBUG_MODE) {
					dbUtil.runCommandWithException(CommandType.INSERT, q);
				}
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
		query.append("current_timestamp(),0,uuid())");
		return query;
	}

	public StringBuilder getXRayResultQuery(int patientId, int encounterLocationId, int cad4tbUserId,
			Integer encounterId, Date obsDate, XRayResult xray) {
		String queryPrefix = "INSERT INTO obs (obs_id,person_id,concept_id,encounter_id,obs_datetime,location_id,interpretation,value_numeric,comments,creator,date_created,voided,uuid) VALUES ";
		StringBuilder query = new StringBuilder(queryPrefix);
		query.append("(0," + patientId + "," + Constant.CAD4TB_SCORE_CONCEPT + "," + encounterId + ",");
		query.append("'" + DateTimeUtil.toSqlDateTimeString(obsDate) + "'," + encounterLocationId + ",NULL,");
		int score = new Double(Math.floor(xray.getCad4tbScore())).intValue();
		query.append(score + ",'Auto-saved by CAD4TB Integration Service.',");
		query.append(cad4tbUserId + ",");
		query.append("current_timestamp(),0,uuid())");
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
		query.append("current_timestamp(),0,uuid())");
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
		query.append("current_timestamp(),0,uuid())");
		return query;
	}
}
