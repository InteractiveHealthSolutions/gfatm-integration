/* Copyright(C) 2014 Interactive Health Solutions, Pvt. Ltd.

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as
published by the Free Software Foundation; either version 3 of the License (GPLv3), or any later version.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program; if not, write to the Interactive Health Solutions, info@ihsinformatics.com
You can also access the license on the internet at the address: http://www.gnu.org/licenses/gpl-3.0.html

Interactive Health Solutions, hereby disclaims all copyright interest in this program written by the contributors. */


/**
 * Enum to represent type of Exception occurred in Application
 */

package com.ihsinformatics.gfatm.integration.shared;

/**
 * @author owais.hussain@ihsinformatics.com
 */
public enum ErrorType {
	AUTHENTICATION_ERROR,NULL_VALUE,INTERNET_CONNECTION_ERROR,INVALID_VALUE, DATA_ACCESS_ERROR, DATA_CONNECTION_ERROR, DATA_MISMATCH_ERROR, DELETE_ERROR, DUPLICATION_ERROR, EMPTY_DATA_ERROR, INSERT_ERROR, INVALID_DATA_ERROR, ID_INVALID, ITEM_NOT_FOUND, PARAMETER_MISSING, PARSING_ERROR, SESSION_EXPIRED, UNKNOWN_ERROR, UPDATE_ERROR, USER_NOT_FOUND, USER_ROLE_UNDEFINED
	,NO_DATA_RECEIVED
}
