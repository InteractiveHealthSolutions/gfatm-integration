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

import javax.net.ssl.HttpsURLConnection;

import org.json.JSONArray;
import org.json.JSONObject;

/**
 * @author owais.hussain@ihsinformatics.com
 *
 */
public class Cad4tbTest {

	/**
	 * @param args
	 */
	public static void main(String[] args) {

		try {
			String[] patientIDs = { "0", "1", "012345" };
			for (String patientID : patientIDs) {
				String base = "https://cloud.cad4tb.care/api/v1/";
				StringBuilder params = new StringBuilder();
				params.append("Archive=archive-test");
				params.append("&");
				params.append("PatientID=" + patientID);
				params.append("&");
				params.append("Project=api-test-project");

				// First fetch patient studies
				String url = base + "patient/studies/list/?" + params.toString();
				JSONArray studies = new JSONArray(httpsGet(url));
				JSONObject study = studies.getJSONObject(0);

				// Now fetch series for this study
				params.append("&");
				params.append("StudyInstanceUID=" + study.getString("StudyInstanceUID"));
				url = base + "patient/study/series/list/?" + params.toString();
				JSONArray serieses = new JSONArray(httpsGet(url));
				JSONObject series = serieses.getJSONObject(0);

				// Finally retrieve the results for this series
				params.append("&");
				params.append("SeriesInstanceUID=" + series.getString("SeriesInstanceUID"));
				params.append("&");
				params.append("Type=cad4tb");
				params.append("&");
				params.append("Results=CAD4TB%205");
				url = base + "/series/results/?" + params.toString();
				JSONArray results = new JSONArray(httpsGet(url));
				JSONObject result = results.getJSONObject(0);
				result.put("Study", study);
				result.put("Series", series);
				System.out.println(result);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static String httpsGet(String urlAddress) throws MalformedURLException {
		URL url = new URL(urlAddress);
		try {
			HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Content-Type", "application/json");
			conn.setRequestProperty("X-USERNAME", "ali.habib@ihsinformatics.com");
			conn.setRequestProperty("X-PASSWORD", "ihsdelft123");
			conn.setDoOutput(true);
			// printHttpsCertificates(conn);

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
}
