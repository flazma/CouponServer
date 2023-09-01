package kr.co.smenglish.monthmgr;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;

import kr.co.smenglish.das.sql.DAS;
import kr.co.smenglish.das.sql.RecordSet;
import kr.co.smenglish.monthmgr.MonthMng;

import org.apache.log4j.Logger;
 
public class MonthServer {
	static Logger logger = Logger.getLogger(MonthServer.class);
	
	private static String currentTime;
	private static String currentHour;
	private int currentDay  = 0;
	private int lastDay = 0;
	private static String todate;
	private static String predate;
	private static String currentDate;
	private int refundLastDay = 0;
	private static int TcurrentDay = 0; //기본값:0, 테스트값:1
	private static String Tpredate = ""; //기본값:"", 테스트값:년-월
	private static int TrefundLastDay = 0; //기본값:0, 테스트값: 전월 마지막 일자
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws InterruptedException {
		if(args.length>0){
			TcurrentDay = Integer.parseInt(args[0]);
			Tpredate = args[1];
			TrefundLastDay = Integer.parseInt(args[2]);
		}
		
		MonthServer monthServer = new MonthServer();
		monthServer.Exectue();
	}
	
	public void Exectue() throws InterruptedException {
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		while(true) { 
			long interval = getNextRun(); //밀리세컨드
			currentTime = sdf.format(Calendar.getInstance().getTime());
			logger.info(String.format("[WAKE-UP] --> ORACLE TIME : %s ",todate));
			
			//TODO : TEST
			if(TcurrentDay>0){
				currentDay = TcurrentDay;
				predate = Tpredate;
				refundLastDay = TrefundLastDay;
				currentHour = "00";
			}

			//매일 자정, 코인 예약처리
			if(currentHour.equals("00")){
				runBookingMng("C");
			}
			
			//매월 1일(자정), 환불 및 차감 실행
			if(currentDay==1 && currentHour.equals("00")){
				logger.info(String.format("[RUN_DAY] --> ORACLE TIME : %s ",todate));
				runRefundCash(predate,refundLastDay); //환불처리
				runBookingMng("M"); //월결제 예약처리
				runCashMng(predate,refundLastDay); //차감처리
			}else if(currentDay>=(lastDay-5) && currentHour.equals("00")){
				//말일 5일전부터 환불예상캐시
				logger.info(String.format("[NEXT_REFUND_RUN_DAY] --> ORACLE TIME : %s ",todate));
				runNextRefundCash(currentDate,lastDay);
			}else if((currentDay==(lastDay-5)||currentDay==(lastDay-2)) && currentHour.equals("09")){
				//말일 5일전, 2일전 SMS
				logger.info(String.format("[SMS_RUN_DAY] --> ORACLE TIME : %s ",todate));
				runSmsMng(currentDate,refundLastDay); //캐시부족문자
			}else if(currentDay==5 && currentHour.equals("00")){
				//출판사과금 처리(매월 5일 자정..)2017-08-21 최현일 
				logger.info(String.format("[RUN_OVER_COUNT_COMPANY] --> ORACLE TIME : %s ",todate));
				runOverCountCompany();
			}
			
			Thread.sleep(interval); 
		}
	}
	
	/**
	 * 환불예상캐시
	 */
	private void runNextRefundCash(String predate, int refundLastDay) {
		MonthMng monthMng = new MonthMng();
		logger.info("---- runNextRefundCash : ");
		monthMng.refundCash(predate, refundLastDay,"N");
	}
	
	/**
	 * 환불
	 */
	private void runRefundCash(String predate, int refundLastDay) {
		MonthMng monthMng = new MonthMng();
		logger.info("---- runNextRefundCash : ");
		monthMng.refundCash(predate, refundLastDay,"Y");
	}
	
	/**
	 * 월과금처리
	 */
	private void runCashMng(String predate, int refundLastDay) {
		MonthMng monthMng = new MonthMng();
		logger.info("---- runCashMng : ");
		monthMng.cashMng(predate, refundLastDay,"Y");
	}
	
	/**
	 * 캐시부족 SMS
	 */
	private void runSmsMng(String predate, int refundLastDay) {
		MonthMng monthMng = new MonthMng();
		logger.info("---- runSmsMng : ");
		monthMng.cashMng(predate, refundLastDay,"N");
	}
	
	//수강관리 예약 처리
	private void runBookingMng(String pay_cd) {
		MonthMng monthMng = new MonthMng();
		logger.info("---- runBookingMng : ");
		monthMng.runBookingMng(pay_cd);
	}
	
	/**
	 * //출판사과금 처리 2017-08-21 최현일
	 */
	private void runOverCountCompany() {
		MonthMng monthMng = new MonthMng();
		logger.info("---- runOverCountCompany : ");
		monthMng.runOverCountCompany();
	}
	
	private long getNextRun(){ 
		DAS das = null;
		long rval = 30000;
		RecordSet rset = null ;
		try
		{
			das = new DAS(DAS_CONFIG);
			String sql = "SELECT TO_CHAR(ADD_MONTHS(SYSDATE,-1), 'YYYY-MM') PREDATE, TO_NUMBER(TO_CHAR(LAST_DAY(ADD_MONTHS(SYSDATE,-1)), 'DD')) REFUNDLASTDAY, TO_CHAR(SYSDATE, 'YYYY-MM-DD HH24:MI:SS') DAY,  " +
				"TO_CHAR(SYSDATE, 'HH24') HOUR, TO_CHAR(SYSDATE, 'YYYY-MM') CURRENTDATE, TO_NUMBER(TO_CHAR(SYSDATE, 'DD')) CURRENTDAY, TO_NUMBER(TO_CHAR(LAST_DAY(SYSDATE), 'DD')) LASTDAY,to_number(to_char(systimestamp, 'MI')) MINUTE,to_number(to_char(systimestamp, 'SS')) SECOND  FROM DUAL ";
			
			rset = das.select(sql, null);
			if (rset.next()){
				
				predate = rset.getString("PREDATE");
				refundLastDay = rset.getInt("REFUNDLASTDAY");
				todate = rset.getString("DAY");
				currentDate = rset.getString("CURRENTDATE");
				currentHour = rset.getString("HOUR");
				currentDay = rset.getInt("CURRENTDAY");
				lastDay = rset.getInt("LASTDAY");
				
				long minute = rset.getLong("MINUTE"); 
				long second = rset.getLong("SECOND");
				rval = ((60 - minute) * 60 - second + 2) * 1000;
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

