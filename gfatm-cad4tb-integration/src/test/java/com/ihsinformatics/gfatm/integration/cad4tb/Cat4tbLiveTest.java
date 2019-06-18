/**
 * 
 */
package com.ihsinformatics.gfatm.integration.cad4tb;

import org.json.JSONArray;
import org.json.JSONObject;

import com.ihsinformatics.gfatm.integration.cad4tb.model.XRayResult;
import com.ihsinformatics.gfatm.integration.cad4tb.shared.Constant;
import com.ihsinformatics.gfatm.integration.cad4tb.shared.HttpUtil;

/**
 * @author owais.hussain@ihsinformatics.com
 *
 */
public class Cat4tbLiveTest {

	public static void main(String[] args) {
		try {
			// IJSUK-6, 8ERSJ-6, ACXY0-5
			String patientId = "IJSUK-6";

			String baseUrl = "https://ihn.cad4tb.care/api/v1/";
			String username = "developer.gfatm@gmail.com";
			String password = "IHSDeveloper";
			String archiveName = "archive";
			String projectName = "ihn";

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
			for (int i = 0; i < results.length(); i++) {
				JSONObject result = results.getJSONObject(i);
				result.put("PatientID", patientId);
				result.put("Study", study);
				result.put("Series", series);
				XRayResult xray = XRayResult.fromJson(result);
				if (xray.getCad4tbScore() < Constant.NORMAL_CUTOFF_SCORE) {
					xray.setCad4tbScoreRange(Constant.NORMAL_SCORE_RANGE_CONCEPT);
					xray.setPresumptiveTbCase(Constant.NO_CONCEPT);
				} else {
					xray.setCad4tbScoreRange(Constant.ABNORMAL_SCORE_RANGE_CONCEPT);
					xray.setPresumptiveTbCase(Constant.YES_CONCEPT);
				}
				System.out.println(xray);
			}
		} catch (Exception e) {
		}
	}

}
