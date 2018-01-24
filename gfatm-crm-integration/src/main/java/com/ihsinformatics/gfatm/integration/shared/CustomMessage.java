/* Copyright(C) 2014 Interactive Health Solutions, Pvt. Ltd.

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as
published by the Free Software Foundation; either version 3 of the License (GPLv3), or any later version.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program; if not, write to the Interactive Health Solutions, info@ihsinformatics.com
You can also access the license on the internet at the address: http://www.gnu.org/licenses/gpl-3.0.html

Interactive Health Solutions, hereby disclaims all copyright interest in this program written by the contributors. */
/**
 * Error provider class
 */

package com.ihsinformatics.gfatm.integration.shared;

/**
 * @author owais.hussain@ihsinformatics.com
 */
public class CustomMessage {
	public static String getInfoMessage(InfoType infoType) {
		String message = "";
		switch (infoType) {
		case ACCESS_GRANTED:
			message = "Access granted, you have successfully logged in.";
			break;
		case CONFIRM_CLOSE:
			message = "Are you sure you want to close the application?";
			break;
		case CONFIRM_OPERATION:
			message = "This operation is irreversable. Are you sure you want to proceed?";
			break;
		case CONNECTION_SUCCESSFUL:
			message = "Connection with the server was successful.";
			break;
		case DELETED:
			message = "Data deleted successfully.";
			break;
		case ID_VALID:
			message = "ID is valid";
			break;
		case INSERTED:
			message = "Data inserted successfully.";
			break;
		case OPERATION_SUCCESSFUL:
			message = "Operation was successful.";
			break;
		case SESSION_RENEWED:
			message = "Session has been renewed.";
			break;
		case UPDATED:
			message = "Data updated successfully.";
			break;
		case VALID:
			message = "Data validated.";
			break;
		case STOP_THREAD:
			message ="We need to stop all the threads, due to the internet connection.Today's data will be sync tomorrow on same time.";	
			break;
		}
		return message;
	}

	public static String getErrorMessage(ErrorType errorType) {
		String error = "";
		switch (errorType) {
		case AUTHENTICATION_ERROR:
			error = "Authentication failed! Please enter valid password/code.";
			break;
		case DATA_ACCESS_ERROR:
			error = "Access failed! You may not have sufficient rights.";
			break;
		case DATA_CONNECTION_ERROR:
			error = "Could not connect to Data source! Please check your connection settings.";
			break;
		case DATA_MISMATCH_ERROR:
			error = "Data or value(s) did not match!";
			break;
		case DELETE_ERROR:
			error = "Error in deleting data. Some other data may be dependent on the record you want to delete.";
			break;
		case DUPLICATION_ERROR:
			error = "Duplication error! Record already exists in the database.";
			break;
		case EMPTY_DATA_ERROR:
			error = "Empty data field! Please fill in the required field(s) first.";
			break;
		case ID_INVALID:
			error = "ID is either invalid or not registered for this operation.";
			break;
		case INSERT_ERROR:
			error = "Error in inserting data. Please make sure you are not violating validation rules.";
			break;
		case ITEM_NOT_FOUND:
			error = "Data not found.";
			break;
		case INVALID_DATA_ERROR:
			error = "Invalid data! Please make sure you are not violating validation rules.";
			break;
		case PARAMETER_MISSING:
			error = "One or more Parameters are invalid or missing. Please make sure you have defined valid parameters.";
			break;
		case PARSING_ERROR:
			error = "Data cannot be parsed. Please make sure data is in valid format.";
			break;
		case SESSION_EXPIRED:
			error = "Session expired! Please re-login to continue operation.";
			break;
		case UNKNOWN_ERROR:
			error = "Unknown error encountered! Please check error log for details.";
			break;
		case UPDATE_ERROR:
			error = "Error in updating data. Please make sure you are not violating validation rules.";
			break;
		case USER_NOT_FOUND:
			error = "User not found! Please enter correct user name.";
			break;
		case USER_ROLE_UNDEFINED:
			error = "User role not found or undefined! Please contact Administrator or select a different Role.";
			break;
		case NULL_VALUE:
			error = "The provided uuid was null; unable to insert data with null uuid.";
			break;
		case INVALID_VALUE:
			error = "The provided value is Invalid";
		case INTERNET_CONNECTION_ERROR:
			error ="Currently internet Connection is not available";
		case NO_DATA_RECEIVED:
			error = "No data received – err empty response";
		}
		return error;
	}
}
