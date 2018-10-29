package com.ihsinformatics.gfatm.web.cad4tb;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import com.ihsinformatics.util.DateTimeUtil;

public class Cad4TbFileUploadServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	private Map<String, Integer> headerIndices;

	/**
	 * @see HttpServlet#HttpServlet()
	 */
	public Cad4TbFileUploadServlet() {
		super();
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
		final String UPLOAD_DIRECTORY = "C:/workspace";
		String content = null;
		if (ServletFileUpload.isMultipartContent(request)) {
			try {
				List<FileItem> fileItems = new ServletFileUpload(new DiskFileItemFactory()).parseRequest(request);
				for (FileItem item : fileItems) {
					if (!item.isFormField()) {
						File fileSaveDir = new File(UPLOAD_DIRECTORY);
						if (!fileSaveDir.exists()) {
							fileSaveDir.mkdir();
						}
						// String name = new File(item.getName()).getName();
						// item.write(new File(UPLOAD_DIRECTORY + File.separator + name));
						content = new String(item.get());
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			PrintWriter out = response.getWriter();
			String output = processOutput(content);
			out.print(output);
		}
	}

	public String processOutput(String content) {
		StringBuilder output = new StringBuilder();
		if (content == null) {
			output.append("ERROR! Data could not be read from the file.");
		}
		String[] rows = content.split("\r\n");
		// Read and match the header
		String header = rows[0];
		if (!checkHeader(header.toLowerCase())) {
			output.append("ERROR! File header is missing one or more mandatory columns.");
			return output.toString();
		}
		headerIndices = detectIndices(header);
		if (!checkDateRange(rows)) {
			output.append("ERROR! Multiple dates detected! Data can only be processed for a single date per file.");
			return output.toString();
		}
		for (int i = 1; i < rows.length; i++) {
			output.append(processRow(headerIndices, rows[i]));
		}
		return output.toString();
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
	 */
	public String processRow(Map<String, Integer> headerIndices, String row) {
		String[] parts = row.split(",");
		String patientId = parts[headerIndices.get("PatientID")];
		String date = parts[headerIndices.get("StudyDate")];
		String time = parts[headerIndices.get("StudyTime")];
		String cad4tb = parts[headerIndices.get("CAD4TB 5")];
		return patientId + ", " + date + ", " + time + ", " + cad4tb;
	}

}