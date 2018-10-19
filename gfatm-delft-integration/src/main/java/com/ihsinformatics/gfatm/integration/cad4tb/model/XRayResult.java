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

import lombok.Data;

/**
 * @author owais.hussain@ihsinformatics.com
 *
 */
public @Data class XRayResult implements Serializable {

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
}
