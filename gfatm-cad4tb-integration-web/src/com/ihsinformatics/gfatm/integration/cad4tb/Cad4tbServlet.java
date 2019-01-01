package com.ihsinformatics.gfatm.integration.cad4tb;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.ThreadLocalRandom;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.xml.bind.ValidationException;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.log4j.Logger;

import com.ihsinformatics.gfatm.integration.cad4tb.model.XRayOrder;
import com.ihsinformatics.gfatm.integration.cad4tb.model.XRayResult;
import com.ihsinformatics.gfatm.integration.cad4tb.shared.Constant;
import com.ihsinformatics.util.ChecksumUtil;
import com.ihsinformatics.util.CommandType;
import com.ihsinformatics.util.DatabaseUtil;
import com.ihsinformatics.util.DateTimeUtil;

public class Cad4tbServlet extends HttpServlet {
	public static final boolean DEBUG_MODE = ManagementFactory.getRuntimeMXBean().getInputArguments().toString()
			.indexOf("-agentlib:jdwp") > 0;
	private static final Logger log = Logger.getLogger(Cad4tbServlet.class);
	private OpenmrsMetaService openmrs;

	private int cad4tbUserId;
	private String username = null;
	private DatabaseUtil dbUtil;

	private static final long serialVersionUID = 1L;
	private Map<String, Integer> headerIndices;
	private PrintWriter out;

	/**
	 * @throws IOException
	 * @see HttpServlet#HttpServlet()
	 */
	public Cad4tbServlet() throws IOException {
		super();
	}

	/**
	 * Read properties from file
	 * 
	 * @throws IOException
	 */
	public static Properties readProperties() throws IOException {
		InputStream inputStream = Thread.currentThread().getContextClassLoader()
				.getResourceAsStream(Constant.PROP_FILE_NAME);
		Properties properties = new Properties();
		properties.load(inputStream);
		return properties;
	}

	/**
	 * Initialize properties, database, and other stuff
	 */
	public void initialize(Properties properties) {
		// Initiate properties
		username = properties.getProperty("cad4tb.openmrs.username");
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
		} else {
			cad4tbUserId = Integer.parseInt(properties.getProperty("cad4tb.openmrs.user_id"));
		}
		openmrs = new OpenmrsMetaService(dbUtil);
	}

	/**
	 * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		doPost(request, response);
	}

	/**
	 * @see HttpServlet#doPost(HttpServletRequest request, HttpServletResponse
	 *      response)
	 */
	@Override
	protected void doPost(HttpServletRequest request, HttpServletResponse response)
			throws ServletException, IOException {
		out = response.getWriter();
		if (dbUtil == null) {
			initialize(readProperties());
		}
		final String UPLOAD_DIRECTORY = "../";
		String content = null;
		if (ServletFileUpload.isMultipartContent(request)) {
			try {
				List<FileItem> fileItems = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
				String username = "";
				String password = "";
				// Read credentials
				for (FileItem item : fileItems) {
					if (item.getFieldName().equalsIgnoreCase("username")) {
						username = new String(item.getString());
					} else if (item.getFieldName().equalsIgnoreCase("password")) {
						password = new String(item.getString());
					}
				}
				if (!openmrs.authenticate(username, password)) {
					out.print("AUTH_ERROR");
					return;
				}
				for (FileItem item : fileItems) {
					if (!item.isFormField()) {
						File fileSaveDir = new File(UPLOAD_DIRECTORY);
						if (!fileSaveDir.exists()) {
							fileSaveDir.mkdir();
						}
						content = new String(item.get());
					}
				}
				processUploadedResults(content);
			} catch (Exception e) {
				out.print(e.getMessage());
			}
		}
	}

	/**
	 * Execute process to import results from content into OpenMRS
	 * 
	 * @param content
	 */
	public void processUploadedResults(String content) {
		if (content == null) {
			out.print("ERROR! Data could not be read from the file.");
			return;
		}
		String[] rows = content.split("\r\n");
		// Read and match the header
		String header = rows[0];
		if (!checkHeader(header.toLowerCase())) {
			out.print("ERROR! File header is missing one or more mandatory columns.");
			return;
		}
		headerIndices = detectIndices(header);
		if (!checkDateRange(rows)) {
			out.print("ERROR! Multiple dates detected! Data can only be processed for a single date per file.");
			return;
		}
		out.print("PatientID,Date,Result,Description\r\n");
		for (int i = 1; i < rows.length; i++) {
			// Apply all validation rules on PatientID
			String patientId = getPatientIdFromRow(rows[i]);
			if (validatePatient(patientId, rows)) {
				String result = processRow(rows[i]);
				if (!result.replace("\r\n", "").trim().equals("")) {
					out.print(result);
				}
			}
		}
	}

	/**
	 * Applies validation rules on PatientID: Format, checksum, duplicates and
	 * patient exists
	 * 
	 * @param patientId
	 * @param rows
	 * @return
	 */
	public boolean validatePatient(String patientId, String[] rows) {
		// Skip invalid PatientIDs
		if (!patientId.matches(Constant.PATIENT_ID_REGEX)) {
			out.println(new StringBuilder(patientId + ",").append(",").append("WARNING,")
					.append("Patient ID is either missing or does not match the required pattern"));
			return false;
		}
		// Match check digit
		try {
			String id = patientId.substring(0, 5);
			int checksum = Integer.parseInt(patientId.substring(6));
			if (!ChecksumUtil.matchLuhnChecksum(id, checksum)) {
				throw new ValidationException("");
			}
		} catch (Exception e) {
			out.println(new StringBuilder(patientId + ",").append(",").append("WARNING,")
					.append("Patient ID is written incorrectly"));
			return false;
		}
		// Check for duplicates
		if (duplicateResultsExist(rows, patientId)) {
			out.println(
					new StringBuilder(patientId + ",").append(",").append("INFO,").append("Duplicate results found"));
			return false;
		}
		// Check if the Patient exists
		if (!patientExists(patientId)) {
			out.println(new StringBuilder(patientId + ",").append(",").append("WARNING,")
					.append("Patient does not exist in OpenMRS"));
			return false;
		}
		return true;
	}

	private boolean patientExists(String patientId) {
		try {
			long rows = dbUtil.getTotalRows("patient_identifier", "WHERE identifier = '" + patientId + "'");
			return rows > 0;
		} catch (SQLException e) {
			return false;
		}
	}

	/**
	 * Detects if the PatientID in given row exists more than once in all rows
	 * 
	 * @param rows
	 * @param row
	 * @return
	 */
	public boolean duplicateResultsExist(String[] rows, String patientId) {
		// Since we are counting the row itself as well
		int count = 0;
		for (String r : rows) {
			String duplicate = getPatientIdFromRow(r);
			if (duplicate.equals(patientId)) {
				count++;
			}
		}
		return (count > 1);
	}

	/**
	 * Fetch PatientID from given row
	 * 
	 * @param row
	 * @return
	 */
	public String getPatientIdFromRow(String row) {
		return row.split(",")[headerIndices.get("PatientID")].toUpperCase();
	}

	/**
	 * Picks random rows and matches the StudyDate. Returns false if data contains
	 * more than one date
	 * 
	 * @param rows
	 * @return
	 */
	public boolean checkDateRange(String[] rows) {
		int limit = (int) Math.round(rows.length * 0.1D);
		Date lastDate = null;
		for (int i = 0; i < limit; i++) {
			int random = ThreadLocalRandom.current().nextInt(1, rows.length - 1);
			String row = rows[random];
			String[] parts = row.split(",");
			Integer dateIndex = headerIndices.get("StudyDate");
			String dateStr = parts[dateIndex];
			if (lastDate == null) {
				lastDate = DateTimeUtil.fromSqlDateString(dateStr);
			} else {
				Date nextDate = DateTimeUtil.fromSqlDateString(dateStr);
				if (nextDate.compareTo(lastDate) != 0) {
					return false;
				}
			}
		}
		return true;
	}

	/**
	 * Checks for mandatory columns in the given header string
	 * 
	 * @param header
	 * @return
	 */
	public boolean checkHeader(String header) {
		return header.contains("patientid") && header.contains("patientname") && header.contains("studydate")
				&& header.contains("studytime") && header.contains("cad4tb 5");
	}

	/**
	 * Returns a map of each column and its respective index in the header
	 * 
	 * @param header
	 * @return
	 */
	public Map<String, Integer> detectIndices(String header) {
		Map<String, Integer> indices = new HashMap<>();
		String[] columns = header.split(",");
		for (int i = 0; i < columns.length; i++) {
			indices.put(columns[i], i);
		}
		return indices;
	}

	/**
	 * Imports a result row into database
	 * 
	 * @param headerIndices
	 * @param row
	 * @return
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 */
	public String processRow(String row) {
		StringBuilder result = new StringBuilder();
		try {
			String[] parts = row.split(",");
			String patientId = parts[headerIndices.get("PatientID")];
			String dateStr = parts[headerIndices.get("StudyDate")];
			String timeStr = parts[headerIndices.get("StudyTime")];
			String cad4tb = parts[headerIndices.get("CAD4TB 5")];
			String dateTimeStr = dateStr + timeStr;
			Date studyDate = DateTimeUtil.fromString(dateTimeStr, DateTimeUtil.SQL_DATE + "HHmmss");
			XRayOrder xrayOrder = null;
			result = new StringBuilder();
			List<XRayOrder> orders = openmrs.getXRayOrders(patientId, studyDate);
			// Check if any order exists
			if (orders.isEmpty()) {
				result.append(patientId + ",");
				result.append(dateStr + ",");
				result.append("WARNING,");
				result.append("No order was found on " + DateTimeUtil.toSqlDateString(studyDate));
				return result.append("\r\n").toString();
			}
			// Get closest OrderReturn the latest X-ray in the group
			for (XRayOrder order : orders) {
				if (xrayOrder == null) {
					xrayOrder = order;
				} else if (order.getEncounterDatetime().after(xrayOrder.getEncounterDatetime())) {
					xrayOrder = order;
				}
			}
			XRayResult xrayResult = new XRayResult();
			xrayResult.setOrderId(xrayOrder.getOrderId());
			xrayResult.setCad4tbScore(Double.valueOf(cad4tb));
			if (xrayResult.getCad4tbScore() < Constant.NORMAL_CUTOFF_SCORE) {
				xrayResult.setCad4tbScoreRange(Constant.NORMAL_SCORE_RANGE_CONCEPT);
				xrayResult.setPresumptiveTbCase(Constant.NO_CONCEPT);
			} else {
				xrayResult.setCad4tbScoreRange(Constant.ABNORMAL_SCORE_RANGE_CONCEPT);
				xrayResult.setPresumptiveTbCase(Constant.YES_CONCEPT);
			}
			xrayResult.setTestResultDate(studyDate);
			if (openmrs.xrayResultExists(xrayOrder.getPatientGeneratedId(), xrayOrder.getOrderId())) {
				result.append(patientId + ",");
				result.append(dateStr + ",");
				result.append("INFO,");
				result.append("Result already exists againts Order ID " + xrayOrder.getOrderId());
				return result.append("\r\n").toString();
			}
			boolean saved = openmrs.saveXrayResult(xrayOrder.getPatientGeneratedId(), xrayOrder.getLocationId(),
					cad4tbUserId, xrayOrder.getDateCreated(), xrayResult);
			result.append(patientId + ",");
			result.append(dateStr + ",");
			if (saved) {
				result.append("SUCCESS,");
				result.append("Result saved for Order ID " + xrayOrder.getOrderId());
			} else {
				result.append("ERROR,");
				result.append("Result saved for Order ID " + xrayOrder.getOrderId());
			}
		} catch (Exception e) {
		}
		return result.append("\r\n").toString();
	}
}
