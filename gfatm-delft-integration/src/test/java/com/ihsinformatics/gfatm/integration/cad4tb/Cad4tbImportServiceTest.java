/* Copyright(C) 2018 Interactive Health Solutions, Pvt. Ltd.

This program is free software; you can redistribute it and/or modify it under the terms of the GNU General Public License as
published by the Free Software Foundation; either version 3 of the License (GPLv3), or any later version.
This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.

See the GNU General Public License for more details. You should have received a copy of the GNU General Public License along with this program; if not, write to the Interactive Health Solutions, info@ihsinformatics.com
You can also access the license on the internet at the address: http://www.gnu.org/licenses/gpl-3.0.html

Interactive Health Solutions, hereby disclaims all copyright interest in this program written by the contributors.
*/
package com.ihsinformatics.gfatm.integration.cad4tb;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.joda.time.DateTime;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.ihsinformatics.gfatm.integration.cad4tb.model.XRayResult;

/**
 * @author owais.hussain@ihsinformatics.com
 *
 */
public class Cad4tbImportServiceTest {

	static Properties properties;
	private Cad4tbImportService service;

	/**
	 * @throws java.lang.Exception
	 */
	@Before
	public void setUp() throws Exception {
		properties = new Properties();
		properties.put("connection.username", "root");
		properties.put("connection.password", "jingle94");
		properties.put("connection.url", "jdbc:mysql://localhost:3306/openmrs?autoReconnect=true&useSSL=false");
		properties.put("connection.default_schema", "openmrs");
		properties.put("connection.driver_class", "com.mysql.jdbc.Driver");
		properties.put("cad4tb.url", "https://cloud.cad4tb.care/api/v1/");
		properties.put("cad4tb.username", "ali.habib@ihsinformatics.com");
		properties.put("cad4tb.password", "ihsdelft123");
		properties.put("cad4tb.openmrs.username", "owais.hussain");
		properties.put("cad4tb.openmrs.user_id", "5");
		properties.put("cad4tb.openmrs.password", "Jingle94");
		properties.put("cad4tb.fetch_duration_hours", "1200");
		properties.put("cad4tb.fetch_delay", "10");
		service = new Cad4tbImportService();
		service.initialize(properties);
	}

	/**
	 * @throws java.lang.Exception
	 */
	@After
	public void tearDown() throws Exception {
	}

	/**
	 * Test method for
	 * {@link com.ihsinformatics.gfatm.integration.cad4tb.Cad4tbImportService#jsonToResult(org.json.JSONObject)}.
	 */
	@Test
	public void testJsonToResult() {
		JSONObject result1 = new JSONObject(
				"{\"PatientID\":\"012345\",\"Study\":{\"StudyTime\":\"142205\",\"ReferringPhysicianName\":null,\"InstitutionName\":null,\"StudyDate\":\"2013-03-05\",\"StudyDescription\":null,\"StudyInstanceUID\":\"1.2.392.200046.100.2.1.110162968453886.130305142205\"},\"Series\":{\"KVP\":120,\"SeriesInstanceUID\":\"1.2.392.200046.100.2.1.110162968453886.130305142205.1\",\"ImageType\":\"[\\\"DERIVED\\\", \\\"PRIMARY\\\", \\\"\\\"]\",\"SeriesDescription\":\"chest pa\",\"StationName\":null,\"PhotometricInterpretation\":\"MONOCHROME1\",\"ImageOrientationPatient\":null,\"Manufacturer\":\"Canon Inc.\",\"SpacingBetweenSlices\":null,\"PatientOrientation\":null,\"modified\":\"2017-11-14T06:18:47.481548Z\",\"SeriesNumber\":1,\"SeriesTime\":\"142205.000000\",\"BodyPartExamined\":\"CHEST\",\"SOPClassUID\":\"1.2.840.10008.5.1.4.1.1.1.1\",\"NumberOfFrames\":null,\"Rows\":2480,\"PatientPosition\":null,\"TransferSyntaxUID\":null,\"ManufacturerModelName\":\"CXDI\",\"form_2\":null,\"ImageComments\":null,\"Columns\":1976,\"ImagePositionPatient\":null,\"SeriesDate\":\"2013-03-05\",\"SliceThickness\":null,\"Modality\":\"DX\"},\"name\":\"CAD4TB 5\",\"value\":63.5533,\"algorithm\":\"CAD4TB\"}");
		JSONObject result2 = new JSONObject(
				"{\"PatientID\":\"012345\",\"Study\":{\"StudyTime\":\"143228\",\"ReferringPhysicianName\":null,\"InstitutionName\":null,\"StudyDate\":\"2013-03-05\",\"StudyDescription\":null,\"StudyInstanceUID\":\"1.2.392.200046.100.2.1.110162968453886.130305143228\"},\"Series\":{\"KVP\":120,\"SeriesInstanceUID\":\"1.2.392.200046.100.2.1.110162968453886.130305143228.1\",\"ImageType\":\"[\\\"DERIVED\\\", \\\"PRIMARY\\\", \\\"\\\"]\",\"SeriesDescription\":\"chest pa\",\"StationName\":null,\"PhotometricInterpretation\":\"MONOCHROME1\",\"ImageOrientationPatient\":null,\"Manufacturer\":\"Canon Inc.\",\"SpacingBetweenSlices\":null,\"PatientOrientation\":null,\"modified\":\"2017-09-23T08:23:03.202624Z\",\"SeriesNumber\":1,\"SeriesTime\":\"143228.000000\",\"BodyPartExamined\":\"CHEST\",\"SOPClassUID\":\"1.2.840.10008.5.1.4.1.1.1.1\",\"NumberOfFrames\":null,\"Rows\":2376,\"PatientPosition\":null,\"TransferSyntaxUID\":null,\"ManufacturerModelName\":\"CXDI\",\"form_2\":null,\"ImageComments\":null,\"Columns\":1984,\"ImagePositionPatient\":null,\"SeriesDate\":\"2013-03-05\",\"SliceThickness\":null,\"Modality\":\"DX\"},\"name\":\"CAD4TB 5\",\"value\":23.1987,\"algorithm\":\"CAD4TB\"}");
		JSONObject result3 = new JSONObject(
				"{\"PatientID\":\"012345\",\"Study\":{\"StudyTime\":\"142205\",\"ReferringPhysicianName\":null,\"InstitutionName\":null,\"StudyDate\":\"2013-03-05\",\"StudyDescription\":null,\"StudyInstanceUID\":\"1.2.392.200046.100.2.1.110162968453886.130305142205\"},\"Series\":{\"KVP\":120,\"SeriesInstanceUID\":\"1.2.392.200046.100.2.1.110162968453886.130305142205.1\",\"ImageType\":\"[\\\"DERIVED\\\", \\\"PRIMARY\\\", \\\"\\\"]\",\"SeriesDescription\":\"chest pa\",\"StationName\":null,\"PhotometricInterpretation\":\"MONOCHROME1\",\"ImageOrientationPatient\":null,\"Manufacturer\":\"Canon Inc.\",\"SpacingBetweenSlices\":null,\"PatientOrientation\":null,\"modified\":\"2017-11-14T06:19:45.300216Z\",\"SeriesNumber\":1,\"SeriesTime\":\"142205.000000\",\"BodyPartExamined\":\"CHEST\",\"SOPClassUID\":\"1.2.840.10008.5.1.4.1.1.1.1\",\"NumberOfFrames\":null,\"Rows\":2480,\"PatientPosition\":null,\"TransferSyntaxUID\":null,\"ManufacturerModelName\":\"CXDI\",\"form_2\":null,\"ImageComments\":null,\"Columns\":1976,\"ImagePositionPatient\":null,\"SeriesDate\":\"2013-03-05\",\"SliceThickness\":null,\"Modality\":\"DX\"},\"name\":\"CAD4TB 5\",\"value\":63.5533,\"algorithm\":\"CAD4TB\"}");
		List<JSONObject> list = Arrays.asList(result1, result2, result3);
		try {
			for (JSONObject json : list) {
				XRayResult result = XRayResult.fromJson(json);
				assertNotNull(result);
				assertNotNull(result.getStudyId());
				assertNotNull(result.getTestResultDate());
				assertNotNull(result.getPatientId());
			}
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test method for
	 * {@link com.ihsinformatics.gfatm.integration.cad4tb.Cad4tbImportService#getXrayResultFromApi(java.lang.String, java.util.Date)}.
	 */
	@Test
	public void testGetXrayResultsByOrder() {
		try {
			DateTime orderDate = new DateTime(2017, 11, 10, 0, 0);
			XRayResult result = service.getXrayResultFromApi("012345", orderDate.toDate());
			assertNotNull(result);
		} catch (Exception e) {
			fail(e.getMessage());
		}
	}

	/**
	 * Test method for
	 * {@link com.ihsinformatics.gfatm.integration.cad4tb.Cad4tbImportService#saveXrayResult(int, int, int, org.joda.time.DateTime, com.ihsinformatics.gfatm.integration.cad4tb.model.XRayResult)}.
	 */
	@Test
	public void testSaveXrayResult() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link com.ihsinformatics.gfatm.integration.cad4tb.Cad4tbImportService#getXRayResultQuery(int, int, int, java.lang.Integer, java.util.Date, java.lang.String, java.lang.Double)}.
	 */
	@Test
	public void testGetXRayResultQuery() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link com.ihsinformatics.gfatm.integration.cad4tb.Cad4tbImportService#getOrderIdQuery(int, int, int, com.ihsinformatics.gfatm.integration.cad4tb.model.XRayResult, java.lang.Integer, java.util.Date, java.lang.String)}.
	 */
	@Test
	public void testGetOrderIdQuery() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link com.ihsinformatics.gfatm.integration.cad4tb.Cad4tbImportService#saveXrayResultEncounter(int, int, int, org.joda.time.DateTime)}.
	 */
	@Test
	public void testSaveXrayResultEncounter() {
		fail("Not yet implemented"); // TODO
	}

	/**
	 * Test method for
	 * {@link com.ihsinformatics.gfatm.integration.cad4tb.Cad4tbImportService#processResult(com.ihsinformatics.gfatm.integration.cad4tb.model.XRayResult)}.
	 */
	@Test
	public void testProcessResult() {
		fail("Not yet implemented"); // TODO
	}
}
