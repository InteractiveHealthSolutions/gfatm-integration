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

/**
 * @author owais.hussain@ihsinformatics.com
 *
 */
public class XRayOrder implements Serializable {

	private static final long serialVersionUID = -4760842436910695991L;
	private Integer patientGeneratedId;
	private String patientIdentifier;
	private Integer locationId;
	private Date encounterDatetime;
	private Date dateCreated;
	private String orderId;

	/**
	 * @return the patientGeneratedId
	 */
	public Integer getPatientGeneratedId() {
		return patientGeneratedId;
	}

	/**
	 * @param patientGeneratedId the patientGeneratedId to set
	 */
	public void setPatientGeneratedId(Integer patientGeneratedId) {
		this.patientGeneratedId = patientGeneratedId;
	}

	/**
	 * @return the patientIdentifier
	 */
	public String getPatientIdentifier() {
		return patientIdentifier;
	}

	/**
	 * @param patientIdentifier the patientIdentifier to set
	 */
	public void setPatientIdentifier(String patientIdentifier) {
		this.patientIdentifier = patientIdentifier;
	}

	/**
	 * @return the locationId
	 */
	public Integer getLocationId() {
		return locationId;
	}

	/**
	 * @param locationId the locationId to set
	 */
	public void setLocationId(Integer locationId) {
		this.locationId = locationId;
	}

	/**
	 * @return the encounterDatetime
	 */
	public Date getEncounterDatetime() {
		return encounterDatetime;
	}

	/**
	 * @param encounterDatetime the encounterDatetime to set
	 */
	public void setEncounterDatetime(Date encounterDatetime) {
		this.encounterDatetime = encounterDatetime;
	}

	/**
	 * @return the dateCreated
	 */
	public Date getDateCreated() {
		return dateCreated;
	}

	/**
	 * @param dateCreated the dateCreated to set
	 */
	public void setDateCreated(Date dateCreated) {
		this.dateCreated = dateCreated;
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
	

	/* (non-Javadoc)
	 * @see java.lang.Object#hashCode()
	 */
	@Override
	public int hashCode() {
		final int prime = 29;
		int result = 1;
		result = prime * result + ((encounterDatetime == null) ? 0 : encounterDatetime.hashCode());
		result = prime * result + ((locationId == null) ? 0 : locationId.hashCode());
		result = prime * result + ((patientGeneratedId == null) ? 0 : patientGeneratedId.hashCode());
		result = prime * result + ((patientIdentifier == null) ? 0 : patientIdentifier.hashCode());
		result = prime * result + ((dateCreated == null) ? 0 : dateCreated.hashCode());
		return result;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		XRayOrder other = (XRayOrder) obj;
		if (encounterDatetime == null) {
			if (other.encounterDatetime != null)
				return false;
		} else if (!encounterDatetime.equals(other.encounterDatetime))
			return false;
		if (locationId == null) {
			if (other.locationId != null)
				return false;
		} else if (!locationId.equals(other.locationId))
			return false;
		if (patientGeneratedId == null) {
			if (other.patientGeneratedId != null)
				return false;
		} else if (!patientGeneratedId.equals(other.patientGeneratedId))
			return false;
		if (patientIdentifier == null) {
			if (other.patientIdentifier != null)
				return false;
		} else if (!patientIdentifier.equals(other.patientIdentifier))
			return false;
		return true;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString() {
		return patientGeneratedId + ", " + patientIdentifier + ", " + locationId + ", " + encounterDatetime + ", "
				+ orderId;
	}
}
