package kr.co.smenglish.couponmgr;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;

import kr.co.smenglish.das.sql.DAS;
import kr.co.smenglish.das.sql.RecordSet;
import kr.co.smenglish.vitalinkmgr.VitaLinkMng;

import org.apache.log4j.Logger;
 
public class CouponServer2 {
	static Logger logger = Logger.getLogger(CouponServer2.class);
	
	private static String lastRunDay;
	private static String nextRunDay;
	private static String beforeRunDay;
	private boolean canCheckAddCompany = false;
	private long currentDay  = -1;
	private long currentHour = -1;
	private long currentMinute = -1;
	private ArrayList requireDateList = null;
	private String _organizationTypeCD="N";
	private String _couponType="N";
	private String _franchiseCD="N";
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws InterruptedException {
	
		CouponServer2 couponServer = new CouponServer2();
		couponServer.Exectue();
	}
	
	public void Exectue() throws InterruptedException {
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		logger.info("---- START : " + sdf.format(Calendar.getInstance().getTime()));
		
		while(true) { 
			long interval = getNextRun();
			logger.info(String.format("[%s][%s] - WAKE-UP : %s", lastRunDay, nextRunDay , sdf.format(Calendar.getInstance().getTime())));
			logger.info(String.format("--> Run : [%s] <--", sdf.format(Calendar.getInstance().getTime())));
			
			this.runVitaDataManage();
			//this.runVitaDataManage2();
			
			Thread.sleep(3000000); 
		}
	}
	
	private void runVitaDataManage() {
		VitaLinkMng vl = new VitaLinkMng();
		logger.info("---- runVitaDataManage : ");
		vl.olympiadLink();
	}
	
	private void runVitaDataManage2() {
		VitaLinkMng vl = new VitaLinkMng();
		logger.info("---- runVitaDataManage2 : ");
		vl.olympiadLink2();
	}

	private long getNextRun()
	{ 
		DAS das = null;
		long rval = 30000;
		RecordSet rset = null ;
		try
		{
			das = new DAS(DAS_CONFIG);
			String sql = "select  to_char(systimestamp-1, 'yyyymmdd') BEFOREDAY, to_char(systimestamp, 'yyyymmdd') DAY, to_char(systimestamp, 'HH24') HOUR, to_number(to_char(systimestamp, 'MI')) MINUTE,  " +
				"to_number(to_char(systimestamp, 'SS')) SECOND, to_number(to_char(systimestamp, 'dd')) TODAY from dual ";

			
			rset = das.select(sql, null);

			if (rset.next())
			{
				
				beforeRunDay = rset.getString("BEFOREDAY");
				nextRunDay = rset.getString("DAY");
				
				//beforeRunDay = "20110814";
				//nextRunDay = "20110815";
				
				long minute = rset.getLong("MINUTE"); 
				long second = rset.getLong("SECOND");
				String hour = rset.getString("HOUR");
				long toDay = rset.getLong("TODAY");
				
				rval = ((60 - minute) * 60 - second + 2) * 1000;

				currentHour =  rset.getLong("HOUR");
				currentDay = toDay;
				currentMinute = rset.getLong("MINUTE");
			}
			
		} catch (Exception e){
			logger.warn(e);
		} finally {
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
			if (das != null) try { das.close(); } catch(Exception ex) {}
		}
		
		return rval;
	}
	
	private final String DAS_CONFIG = "EN";
	
}

