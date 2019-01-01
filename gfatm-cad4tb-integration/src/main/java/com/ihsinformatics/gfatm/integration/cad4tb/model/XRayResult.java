/* Copyright(C) 2018 Interactive Health Solutions, Pvt. Ltd.

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as
published by the Free Software Foundation; either version 3 of the License (GPLv3), or any later version.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program; if not, write to the Interactive Health Solutions, info@ihsinformatics.com
You can also access the license on the internet at the address: http://www.gnu.org/licenses/gpl-3.0.html

Interactive Health Solutions, hereby disclaims all copyright interest in this program written by the contributors.
*/
package com.ihsinformatics.gfatm.integration.cad4tb.model;

import java.io.Serializable;
import java.util.Date;

import org.json.JSONException;
import org.json.JSONObject;

import com.ihsinformatics.gfatm.integration.cad4tb.shared.Constant;
import com.ihsinformatics.util.DateTimeUtil;

/**
 * @author owais.hussain@ihsinformatics.com
 *
 */
public class XRayResult implements Serializable {

	private static final long serialVersionUID = -2396853698753897234L;
	private String patientId;
	private String studyId;
	private String seriesId;
	private Double cad4tbScore;
	private String testId;
	private Date testResultDate;
	private String orderId;
	private String diseaseExtent;
	private String radiologicalDiagnosis;
	private String abnormalDetailedDiagnosis;
	private String otherAbnormalDiagnosis;
	private String doctorNotes;
	private Integer cad4tbScoreRange;
	private String chestXRay;
	private Integer presumptiveTbCase;
	private Date returnVisitDate;

	/**
	 * @return the patientId
	 */
	public String getPatientId() {
		return patientId;
	}

	/**
	 * @param patientId the patientId to set
	 */
	public void setPatientId(String patientId) {
		this.patientId = patientId;
	}

	/**
	 * @return the studyId
	 */
	public String getStudyId() {
		return studyId;
	}

	/**
	 * @param studyId the studyId to set
	 */
	public void setStudyId(String studyId) {
		this.studyId = studyId;
	}

	/**
	 * @return the seriesId
	 */
	public String getSeriesId() {
		return seriesId;
	}

	/**
	 * @param seriesId the seriesId to set
	 */
	public void setSeriesId(String seriesId) {
		this.seriesId = seriesId;
	}

	/**
	 * @return the cad4tbScore
	 */
	public Double getCad4tbScore() {
		return cad4tbScore;
	}

	/**
	 * @param cad4tbScore the cad4tbScore to set
	 */
	public void setCad4tbScore(Double cad4tbScore) {
		this.cad4tbScore = cad4tbScore;
	}

	/**
	 * @return the testId
	 */
	public String getTestId() {
		return testId;
	}

	/**
	 * @param testId the testId to set
	 */
	public void setTestId(String testId) {
		this.testId = testId;
	}

	/**
	 * @return the testResultDate
	 */
	public Date getTestResultDate() {
		return testResultDate;
	}

	/**
	 * @param testResultDate the testResultDate to set
	 */
	public void setTestResultDate(Date testResultDate) {
		this.testResultDate = testResultDate;
	}

	/**
	 * @return the orderId
	 */
	public String getOrderId() {
		return orderId;
	}

	/**
	 * @param orderId the orderId to set
	 */
	public void setOrderId(String orderId) {
		this.orderId = orderId;
	}

	/**
	 * @return the diseaseExtent
	 */
	public String getDiseaseExtent() {
		return diseaseExtent;
	}

	/**
	 * @param diseaseExtent the diseaseExtent to set
	 */
	public void setDiseaseExtent(String diseaseExtent) {
		this.diseaseExtent = diseaseExtent;
	}

	/**
	 * @return the radiologicalDiagnosis
	 */
	public String getRadiologicalDiagnosis() {
		return radiologicalDiagnosis;
	}

	/**
	 * @param radiologicalDiagnosis the radiologicalDiagnosis to set
	 */
	public void setRadiologicalDiagnosis(String radiologicalDiagnosis) {
		this.radiologicalDiagnosis = radiologicalDiagnosis;
	}

	/**
	 * @return the abnormalDetailedDiagnosis
	 */
	public String getAbnormalDetailedDiagnosis() {
		return abnormalDetailedDiagnosis;
	}

	/**
	 * @param abnormalDetailedDiagnosis the abnormalDetailedDiagnosis to set
	 */
	public void setAbnormalDetailedDiagnosis(String abnormalDetailedDiagnosis) {
		this.abnormalDetailedDiagnosis = abnormalDetailedDiagnosis;
	}

	/**
	 * @return the otherAbnormalDiagnosis
	 */
	public String getOtherAbnormalDiagnosis() {
		return otherAbnormalDiagnosis;
	}

	/**
	 * @param otherAbnormalDiagnosis the otherAbnormalDiagnosis to set
	 */
	public void setOtherAbnormalDiagnosis(String otherAbnormalDiagnosis) {
		this.otherAbnormalDiagnosis = otherAbnormalDiagnosis;
	}

	/**
	 * @return the doctorNotes
	 */
	public String getDoctorNotes() {
		return doctorNotes;
	}

	/**
	 * @param doctorNotes the doctorNotes to set
	 */
	public void setDoctorNotes(String doctorNotes) {
		this.doctorNotes = doctorNotes;
	}

	/**
	 * @return the cad4tbScoreRange
	 */
	public Integer getCad4tbScoreRange() {
		return cad4tbScoreRange;
	}

	/**
	 * @param cad4tbScoreRange the cad4tbScoreRange to set
	 */
	public void setCad4tbScoreRange(Integer cad4tbScoreRange) {
		this.cad4tbScoreRange = cad4tbScoreRange;
	}

	/**
	 * @return the chestXRay
	 */
	public String getChestXRay() {
		return chestXRay;
	}

	/**
	 * @param chestXRay the chestXRay to set
	 */
	public void setChestXRay(String chestXRay) {
		this.chestXRay = chestXRay;
	}

	/**
	 * @return the presumptiveTbCase
	 */
	public Integer getPresumptiveTbCase() {
		return presumptiveTbCase;
	}

	/**
	 * @param presumptiveTbCase the presumptiveTbCase to set
	 */
	public void setPresumptiveTbCase(Integer presumptiveTbCase) {
		this.presumptiveTbCase = presumptiveTbCase;
	}

	/**
	 * @return the returnVisitDate
	 */
	public Date getReturnVisitDate() {
		return returnVisitDate;
	}

	/**
	 * @param returnVisitDate the returnVisitDate to set
	 */
	public void setReturnVisitDate(Date returnVisitDate) {
		this.returnVisitDate = returnVisitDate;
	}

	/**
	 * Convert JSON object into XRayResult object
	 * 
	 * @param json
	 * @return
	 */
	public static XRayResult fromJson(JSONObject json) throws JSONException {
		XRayResult xray = new XRayResult();
		JSONObject study = json.getJSONObject("Study");
		JSONObject series = json.getJSONObject("Series");
		xray.setStudyId(study.getString("StudyInstanceUID"));
		xray.setSeriesId(series.getString("SeriesInstanceUID"));
		xray.setCad4tbScore(json.getDouble("value"));
		xray.setPatientId(json.getString("PatientID"));
		String modifiedDate = series.getString("modified");
		xray.setTestResultDate(DateTimeUtil.fromString(modifiedDate, Constant.DATE_FORMAT));
		return xray;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return patientId + ", " + cad4tbScore + ", " + testResultDate + ", " + orderId;
	}
}
