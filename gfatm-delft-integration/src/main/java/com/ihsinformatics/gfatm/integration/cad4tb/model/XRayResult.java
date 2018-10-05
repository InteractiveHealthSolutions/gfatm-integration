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
import java.text.ParseException;
import java.util.Date;

import org.json.JSONObject;

import com.ihsinformatics.util.DateTimeUtil;

/**
 * @author owais.hussain@ihsinformatics.com
 *
 */
public class XRayResult implements Serializable {

	private static final long serialVersionUID = -2396853698753897234L;
	public static final String DEFAULT_DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss";
	private String patientId;
	private Date testDate;

	/**
	 * Read object from JSON and set current
	 * 
	 * @param json
	 * @throws ParseException
	 * @throws Exception
	 */
	public void fromJson(JSONObject json) throws ParseException {
		if (json.has("patientId")) {
			patientId = String.valueOf(json.get("patientId"));
		}
	}

	/**
	 * Parse the results and assigns values to respective attributes
	 * 
	 * @param json
	 */
	public void parseResults(JSONObject json) {
	}

	/**
	 * Convert current object into JSON
	 * 
	 * @return
	 */
	public JSONObject toJson() {
		JSONObject json = new JSONObject();
		json.put("patientId", patientId);
		json.put("testEndedOn", testDate == null ? null : DateTimeUtil.toSqlDateString(testDate));
		return json;
	}

	@Override
	public String toString() {
		return toString();
	}
}
