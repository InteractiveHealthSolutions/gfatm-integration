/* Copyright(C) 2018 Interactive Health Solutions, Pvt. Ltd.

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as
published by the Free Software Foundation; either version 3 of the License (GPLv3), or any later version.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program; if not, write to the Interactive Health Solutions, info@ihsinformatics.com
You can also access the license on the internet at the address: http://www.gnu.org/licenses/gpl-3.0.html

Interactive Health Solutions, hereby disclaims all copyright interest in this program written by the contributors.
 */

package com.ihsinformatics.gfatm.integration.cad4tb.shared;

/**
 * @author owais.hussain@ihsinformatics.com
 *
 */
public final class Constant {

	private Constant() {
		// Hide the implicit public interface
	}

	public static final String PATIENT_ID_REGEX = "^[A-Za-z0-9]{5}\\-[0-9]$";
	public static final String ARCHIVE_NAME = "archive-test";
	public static final String CAD4TB_API_PROJECT_NAME = "api-test-project";
	public static final String TEST_TYPE = "cad4tb";

	public static final int XRAY_ORDER_ENCOUNTER_TYPE = 185;
	public static final int XRAY_ENCOUNTER_TYPE = 186;
	public static final int ORDER_ID_CONCEPT = 165715;

	public static final int XRAY_RESULT_CONCEPT = 162202;
	public static final int ERROR_CONCEPT = 165361;
	public static final int CAD4TB_CONCEPT = 164306;
	public static final int ERROR_CODE_CONCEPT = 164348;

	public static final int NORMAL_SCORE_RANGE_CONCEPT = 165800;
	public static final int ABNORMAL_SCORE_RANGE_CONCEPT = 165801;
	public static final int YES_CONCEPT = 1065;
	public static final int NO_CONCEPT = 1066;

}
