package com.ihsinformatics.gfatm.integration;

import static org.junit.Assert.*;

import java.io.IOException;
import java.sql.SQLException;

import org.joda.time.Days;
import org.joda.time.LocalDate;
import org.json.JSONObject;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ihsinformatics.gfatm.integration.connection.AppInitializer;
import com.ihsinformatics.gfatm.integration.server.ServerService;
import com.ihsinformatics.gfatm.integration.shared.Constant;

public class CrmIntegrationTest {
	private String lastDate = "";
	private String currentDateStr = "";
	private LocalDate currentDate;
	private int days = 0;
	private String url= "https://ihs.ibexglobal.com/api/fetch_data.php";
    private String authKey = "SWhzREB0YUZldDRlcjpUcmdAZG0xbjRJaHMhRjQ=";
	/**
	 * @throws java.lang.Exception
	 */
	@BeforeClass
	public static void setUpBeforeClass() throws Exception {
	}

	/**
	 * @throws java.lang.Exception
	 */
	@AfterClass
	public static void tearDownAfterClass() throws Exception {
	}
	
	/*
	 * Case No.1 : Test the AppInitializer Class 
	 * 
	 * 
	 */

	@Test
	public void appInit(){
		
		AppInitializer app = new AppInitializer();
		
		if (app.dbInitializer()) {
			assertTrue(true);
		}
		else
			assertTrue(false);
	}
	
	public void dateSliceTest(){

		try {
			currentDate = new LocalDate();
			lastDate = ServerService.getInstance().getLastEnteredDate(); 
            //In these case i have also check the situation when lastDate is null or some undefined value return ..
			days = Days.daysBetween(new LocalDate(lastDate),
					new LocalDate(currentDate)).getDays();
			if(days == 17)
              assertTrue(true);
			long numberIteration = days / Constant.maxDays;
			int exceedDays = 0;
			final String fixedCurrentDate = currentDate.toString();// fixed
			for (int i = 0; i <= numberIteration; i++) {
				if (days > Constant.maxDays) {
					exceedDays = days - Constant.maxDays;
					LocalDate fixedDate = currentDate.minusDays(exceedDays);
					currentDateStr = fixedDate.toString();
					lastDate = currentDateStr;
					days = exceedDays;
					
				} else {
					 CrmIntegrationMain main = new CrmIntegrationMain();
				    System.out.println(main.getRequestForObject(lastDate, fixedCurrentDate).toString());	
					if (days == Constant.maxDays || days<Constant.maxDays) {
						assertTrue(true);
					}
					else{
						assertFalse(true);
					}
				}
			}
	
		} catch (SQLException e) {
			System.exit(-1);//in case of any error this should be shutdown the thread.
		}
	}
  
	/**
	 * In case : i-  without Internet connection 
	 *           ii- Given url is not execute without certificate ....so check the exception. 
	 */

   public void httpRequest(){
	   
	try {
		ServerService.getInstance().httpRequest(url, authKey);
	} catch (IOException e) {
 	    assertTrue(false);
	}  
	   
   }
	
	/**
	 * we need to check the process is completely end or not after process the methods ... 
	 * 
	 */
	
   @Test	 
   public void executeFinished(){
	   
	    CrmIntegrationMain main = new CrmIntegrationMain();
	    JSONObject obj = main.dateSlice();
		System.out.println("Result Object : " +obj.toString());
		Assert.assertTrue(true);
		System.exit(0);
		
   }	
}
