package kr.co.smenglish.couponmgr;

import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import kr.co.smenglish.classmgr.ClassMng;
import kr.co.smenglish.das.sql.DAS;
import kr.co.smenglish.das.sql.RecordSet;
import kr.co.smenglish.homeworkmgr.HomeworkMng;
import kr.co.smenglish.vitalinkmgr.VitaLinkMng;
import com.smenglish.core.util.CommonUtil;

import org.apache.log4j.Logger;
 
public class CouponServer {
	static Logger logger = Logger.getLogger(CouponServer.class);
	
	private static String lastRunDay;
	private static String nextRunDay;
	private static String beforeRunDay;
	private boolean canCheckAddCompany = false;
	private long currentDay  = -1;
	private long currentHour = -1;
	private ArrayList requireDateList = null;
	private String _organizationTypeCD="N";
	private String _couponType="N";
	private String _franchiseCD="N";
	
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws InterruptedException {
	
		CouponServer couponServer = new CouponServer();
		couponServer.Exectue();
	}
	
	public void Exectue() throws InterruptedException {
		//테스트
		//SMEnglish
		//nextRunDay = "20150113"; //차감되야 하는날짜
		
				
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		logger.info("---- START : " + sdf.format(Calendar.getInstance().getTime()));
		
		lastRunDay = "";

		while(true) { 
			long interval = getNextRun();
			logger.info(String.format("[%s][%s] - WAKE-UP : %s", lastRunDay, nextRunDay , sdf.format(Calendar.getInstance().getTime())));
			if (!lastRunDay.endsWith(nextRunDay)) {
				logger.info(String.format("--> Run : [%s] <--", sdf.format(Calendar.getInstance().getTime())));

				// ***************** 코인관련 ***************** //
				
				if(	currentHour < 3)	//오전 3시 이전에만 실행되므로 해당 내용 수정 필요
				{
					//SMEnglish
					this.RunCouponForSM();
	
					//영신
					this.RunCouponForYoungSin();
				}
				
				//VitaEnglish
				this.RunCouponForVitaEnglish();
				
				//KT 내신 
				this.RunCouponForKT();
				
				//sms : 비타와 내신만 이 sms를 사용함
				//this.RunRemainCheck();
				
				
				// ***************** 코인관련 ***************** //
				
				this.runClassManage();
				this.runHomeworkManage();
				lastRunDay = nextRunDay;
			}
			
			logger.info("SendSMSForSM START currentHour : "+ Long.toString(currentHour));
			if(currentHour == 9)
			{
				logger.info("SendSMSForSM START : " );
				this.SendSMSForSM();
			}
			
			Thread.sleep(interval);
		}
	}
	
	private void runClassManage() {
		ClassMng cm = new ClassMng();
		cm.AddBookToClass();
	}

	private void runHomeworkManage() {
		HomeworkMng hm = new HomeworkMng();
		hm.giveHomework();
	}
	
	private void runVitaDataManage() {
		VitaLinkMng vl = new VitaLinkMng();
		logger.info("---- runVitaDataManage : ");
		vl.olympiadLink();
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
				//5일부터 SMEnglish추가 출판사를 체크함
				if(toDay >= 5)
					this.canCheckAddCompany = true;
				else
					this.canCheckAddCompany = false;
				
				logger.info(String.format("[NEXT_RUN_DAY:%s] --> ORACLE TIME : %s시 %s분 %s 초 ",nextRunDay, hour, minute, second));
			}
			
		} catch (Exception e){
			logger.warn(e);
		} finally {
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
			if (das != null) try { das.close(); } catch(Exception ex) {}
		}
		
		return rval;
	}
	
	private Date getCurrentDate(DAS das){
		Date rval = Calendar.getInstance().getTime();
		RecordSet rset = null; 
		try
		{
			String sql = "select sysdate as TODAY from dual ";
			rset = das.select(sql, null);
			
			if (rset.next())
			{
				rval = rset.getTimeStamp("TODAY"); 
			}
			
		} catch (Exception e){
			logger.warn(e);
		} finally {
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
		}
		
		return rval;
	}
	
	private final String DAS_CONFIG = "EN";
	
	private ArrayList<AcademyCoupon> GetTargetAcademyRawIdNew(){
		DAS das = null;
		RecordSet rset = null;
		
		ArrayList<AcademyCoupon> alAcadmyCoupon = new ArrayList<AcademyCoupon>();
		try {
			das = new DAS(DAS_CONFIG);
			String sql="";
			if(this._franchiseCD.equals("VT"))
			{
				 sql = "SELECT academy_rawid, remain_coupon_count " +
				 "  FROM academy_coupon_mst " +
				 " WHERE academy_rawid in ( " +
				 "     SELECT rawid                            " +
				 "       FROM academy_mst                      " +
				 "      WHERE franchise_cd = ?              " +
				 "        AND del_yn = 'N'                     " +
				 "                         ) 				   " +
				 "  AND coupon_type = ? 					   " ;
			}
			else
			{
				 sql = "SELECT academy_rawid, remain_coupon_count " +
				 "  FROM academy_coupon_mst " +
				 " WHERE academy_rawid in ( " +
				 "     SELECT rawid                            " +
				 "       FROM academy_mst                      " +
				 "      WHERE franchise_cd <> ?           " + 
				 "        AND textbook_member_start <= sysdate " +
				 "        AND textbook_member_end + 1 > sysdate" +
				 "        AND del_yn = 'N'                     " +
				 "                         ) 				   " +
				 "  AND coupon_type = ? 					   " ;
			}
			
			LinkedHashMap keyList = new LinkedHashMap();
			//keyList.put("FRANCHISE_CD", this._franchiseCD);
			keyList.put("FRANCHISE_CD", "VT");
			keyList.put("COUPON_TYPE", this._couponType);
			rset = das.select(sql, keyList);
			
			while(rset.next()){
				AcademyCoupon ac = new AcademyCoupon();
				ac.setRawId(rset.getLong("academy_rawid"));
				ac.setRemainCouponCount(rset.getLong("remain_coupon_count"));
				
				alAcadmyCoupon.add(ac);
			}
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
			if (das != null) try { das.close(); } catch(Exception ex) {}
		}
		
		return alAcadmyCoupon;
	}
	
	/**
	 * 금일 (nextRunDay)에 쿠폰을 소모할 사용자를 가져온다.
	 * @param rawid
	 * @return
	 */
	private ArrayList<UserInfo> GetTargetUserByAcademyRawId(DAS dasx, long rawid){
		RecordSet rset = null;
		
		ArrayList<UserInfo> alUserInfo = new ArrayList<UserInfo>();
		try {
			String sql = "SELECT ACADEMY_RAWID, RAWID, cp_apply_yn " +
						 "  FROM user_mst " +
						 " WHERE cp_apply_yn IN ('Y')" + //, 'M')" +
						 "   AND to_char(cp_apply_dt, 'yyyymmdd') <= ? " +
						 "   AND academy_rawid = ? " +
						 "   AND user_status_cd in ('01', '03', '05') ";
			LinkedHashMap keyList = new LinkedHashMap();
			keyList.put("APPLY_DT", nextRunDay);
			keyList.put("ACADEMY_RAWID", rawid + "");
			
			rset = dasx.select(sql, keyList);
			
			while(rset.next()){
				UserInfo ui = new UserInfo();
				ui.setAcademyRawId(rset.getLong("academy_rawid"));
				ui.setRawId(rset.getLong("rawid"));
				ui.setCpapply(rset.getString("cp_apply_yn"));
				
				alUserInfo.add(ui);
			}
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
		}
		
		return alUserInfo;
	}
	
	private UserInfo getUserByRawId(DAS das, long rawId){
		RecordSet rset = null;
		UserInfo ui = null;
		try {
			String sql = "SELECT ACADEMY_RAWID, RAWID, cp_apply_yn, cp_apply_dt " +
						 "  FROM user_mst " +
						 " WHERE rawid = ? ";
			
			LinkedHashMap keyList = new LinkedHashMap();
			keyList.put("RAWID", rawId);
			
			rset = das.select(sql, keyList);
			while(rset.next()){
				ui = new UserInfo();
				ui.setAcademyRawId(rset.getLong("academy_rawid"));
				ui.setRawId(rset.getLong("rawid"));
				ui.setCpapply(rset.getString("cp_apply_yn"));
				ui.setCpApplyDate(rset.getDate("cp_apply_dt"));
			}
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
		}
		return ui;
	}
	
	
	private UserInfo getUserByRawIdNew(DAS das, long userRawId, String couponType){
		RecordSet rset = null;
		UserInfo ui = null;
		try {
			String sql = "SELECT ACADEMY_RAWID, RAWID, cp_apply_yn, cp_apply_dt " +
						 "  FROM user_coupon_mst " +
						 " WHERE user_rawid = ? " +
						 "   AND coupon_type = ? ";
			 
			LinkedHashMap keyList = new LinkedHashMap();
			keyList.put("USER_RAWID", userRawId);
			keyList.put("COUPON_TYPE", couponType);
			
			rset = das.select(sql, keyList);
			while(rset.next()){
				ui = new UserInfo();
				ui.setAcademyRawId(rset.getLong("academy_rawid"));
				ui.setRawId(rset.getLong("rawid"));
				ui.setCpapply(rset.getString("cp_apply_yn"));
				ui.setCpApplyDate(rset.getDate("cp_apply_dt"));
			}
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
		}
		
		return ui;
	}
	
	
	private long getRemainCouponCountNew(DAS das, long academyRawId, String couponType) throws Exception {
		long remainCouponCount = 0;
		
		RecordSet rset = null;
		try {
			String sql = "SELECT academy_rawid, remain_coupon_count" +
			            "   FROM academy_coupon_mst " +
			            "  WHERE academy_rawid = ? " +
			            "    AND coupon_type = ? ";
			LinkedHashMap keyList = new LinkedHashMap();
			keyList.put("ACADEMY_RAWID", academyRawId);
			keyList.put("COUPON_TYPE", couponType);
			rset = das.select(sql, keyList);
			
			if (rset.next())
				remainCouponCount = rset.getLong("remain_coupon_count");
			
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
		}
		
		return remainCouponCount;
	}
	
	
	
	private boolean existUserCouponTrxNew(DAS das, long academyRawId, long userRawId, String applyDay, String couponType){

		RecordSet rset = null;
		boolean result = false;
		try {
			String sql = "SELECT rawid, academy_rawid, user_rawid, " +
						 "       apply_type_cd, apply_day, create_dt" +
						 "  FROM user_coupon_trx " +
						 " WHERE academy_rawid = ? " +
						 "   AND user_rawid    = ? " +
						 "   AND apply_day     = ? " +
						 "   AND coupon_type   = ? ";
			LinkedHashMap keyList = new LinkedHashMap();
			keyList.put("ACADEMY_RAWID", academyRawId + "");
			keyList.put("USER_RAWID", userRawId + "");
			keyList.put("APPLY_DAY", applyDay);
			keyList.put("COUPON_TYPE", couponType);
			
			rset = das.select(sql, keyList);
			if (rset.next())
				result = true;
			else
				result = false;
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
		}
		
		return result;
	}

	
	private int updateUserCpApply(DAS das, long userRawId) throws Exception {
		LinkedHashMap columnMap = new LinkedHashMap();
		columnMap.put("CP_APPLY_YN+'M'", null);
		
		LinkedHashMap whereMap = new LinkedHashMap();
		whereMap.put("RAWID", userRawId + "");
		return das.update("USER_MST", columnMap, "where rawid = ?", whereMap);
	}
	
	
	
	
	private int insertUserCouponTrx(DAS das, Date hstDate, long academyRawId, long userRawId, String couponType)  throws Exception {
		
		LinkedHashMap columnMap = new LinkedHashMap();
		columnMap.put("RAWID+SEQ_USER_COUPON_TRX.nextval", null);
		columnMap.put("ACADEMY_RAWID", academyRawId);
		columnMap.put("USER_RAWID", userRawId);
		columnMap.put("APPLY_TYPE_CD", "S");
		columnMap.put("APPLY_DAY", nextRunDay);
		columnMap.put("CREATE_DT+SYSDATE", null);
		columnMap.put("HST_DT", hstDate);
		columnMap.put("COUPON_TYPE", couponType);
		
		return das.insert("USER_COUPON_TRX", columnMap);
	}

	private int insertUserCouponTrxNew(DAS das, Date hstDate, ApplyCouponData couponData, UserInfo userInfo)  throws Exception {
		
		int result = 0;
		if(couponData.getRenewType().equals("R"))
		{
			LinkedHashMap columnMap = new LinkedHashMap();
			columnMap.put("RAWID+SEQ_USER_COUPON_TRX.nextval", null);
			columnMap.put("ACADEMY_RAWID", userInfo.getAcademyRawId());
			columnMap.put("USER_RAWID", userInfo.getRawId());
			columnMap.put("APPLY_TYPE_CD", "S");
			columnMap.put("APPLY_DAY", nextRunDay);
			columnMap.put("CREATE_DT+SYSDATE", null);
			columnMap.put("HST_DT", hstDate);
			columnMap.put("COUPON_TYPE", couponData.getCouponType());
			
			result = das.insert("USER_COUPON_TRX", columnMap);
		}
		else
		{
			result = this.SetRequiredStart(das, hstDate, couponData, userInfo);
		}
		return result;
	}
	
	
	private int SetRequiredStart(DAS das, Date hstDate, ApplyCouponData couponData, UserInfo userInfo)throws Exception {
		int result = 0;
		String strDate = userInfo.getApplyStartDay();
			
		if(this.requireDateList != null)
		{
			for(int i=0; i < this.requireDateList.size(); i++)
			{
				LinkedHashMap columnMap = new LinkedHashMap();
				columnMap.put("RAWID+SEQ_USER_COUPON_TRX.nextval", null);
				columnMap.put("ACADEMY_RAWID", userInfo.getAcademyRawId());
				columnMap.put("USER_RAWID", userInfo.getRawId());
				columnMap.put("APPLY_TYPE_CD", "S");
				columnMap.put("CREATE_DT+SYSDATE", null);
				columnMap.put("HST_DT", hstDate);
				columnMap.put("COUPON_TYPE", couponData.getCouponType());

				if(userInfo.getReservationYN().equals("N"))
				{
					columnMap.put("APPLY_DAY", (String)this.requireDateList.get(i));
				}
				else
				{
					if(i > 0)
						strDate = GetAddedDate(strDate);
					
					columnMap.put("APPLY_DAY", strDate);
				}
				result+= das.insert("USER_COUPON_TRX", columnMap);
			}
			if(result == this.requireDateList.size())
				result = 1;
		}
		return result;
	}
	
	private int deleteUserCouponTrxNew(DAS das, Date hstDate, ApplyCouponData couponData, UserInfo userInfo)   {
		RecordSet rset = null;
		int result = 1;
		boolean existData = false;
		try
		{
			String sql =" SELECT * FROM USER_COUPON_TRX WHERE academy_rawid = ? and user_rawid = ? and apply_day >= ? and coupon_type=? ";

			LinkedHashMap whereMap = new LinkedHashMap();
			whereMap.put("ACADEMY_RAWID"  , userInfo.getAcademyRawId());
			whereMap.put("USER_RAWID" , userInfo.getRawId());
			whereMap.put("APPLY_DAY", userInfo.getApplyStartDay());
			whereMap.put("COUPON_TYPE", couponData.getCouponType());
			
			rset = das.select(sql, whereMap);
			
			while(rset.next())
			{
				existData = true;
			}
			
			
			if(existData)
			{
				result = das.delete("USER_COUPON_TRX", "WHERE academy_rawid = ? and user_rawid = ? and apply_day >= ? and coupon_type=? ", whereMap);
				if(result > 0)
				{
					logger.info("Delete Success USER_COUPON_TRX :WHERE academy_rawid = "+userInfo.getAcademyRawId()+" and user_rawid = "+userInfo.getRawId()+" and apply_day >= "+userInfo.getApplyStartDay()+" and coupon_type="+couponData.getCouponType());
					result =1;
				}
				else
				{
					logger.info("Delete Fail USER_COUPON_TRX :WHERE academy_rawid = "+userInfo.getAcademyRawId()+" and user_rawid = "+userInfo.getRawId()+" and apply_day >= "+userInfo.getApplyStartDay()+" and coupon_type="+couponData.getCouponType());
				}
			}
			
		} catch (Exception e) {
			logger.warn(e);
		} finally{
		}
		
		return result;
	}
	


	
	private int updateAcademyCouponMstNew(DAS das, long academyRawId, String couponType, String applyCouponCount) throws Exception {
		LinkedHashMap columnMap = new LinkedHashMap();
		columnMap.put("REMAIN_COUPON_COUNT+(REMAIN_COUPON_COUNT - "+applyCouponCount+")", null);
		
		LinkedHashMap whereMap = new LinkedHashMap();
		whereMap.put("ACADEMY_RAWID", academyRawId);
		whereMap.put("COUPON_TYPE", couponType);
		return das.update("ACADEMY_COUPON_MST", columnMap, "WHERE academy_rawid = ? AND coupon_type = ? ", whereMap);
	}
	
	
	private long getSequenceOfCouponHst(DAS das) throws Exception {
		return das.getSequence_Nextval("SEQ_COUPON_HST");
	}
	
	private int insertCouponHstNew(DAS das, CouponHstEntity entity)throws Exception {
		
		LinkedHashMap columnMap = new LinkedHashMap();
		columnMap.put("RAWID", entity.getRawId());
		columnMap.put("APPLY_DT", entity.getApplyDate());
		columnMap.put("ACADEMY_RAWID"   , entity.getAcademyRawId());
		columnMap.put("USE_TYPE_CD"     , entity.getUseTypeCode());
		columnMap.put("CREATE_TYPE_CD"	, entity.getCreateTypeCode());
		columnMap.put("APPLY_USER_CNT"	, entity.getApplyUserCnt());
		columnMap.put("NON_APPLY_USER_CNT"	, entity.getNonApplyUserCnt());
		columnMap.put("APPLY_COUPON_CNT"	, entity.getApplyCouponCnt());
		columnMap.put("REMAIN_COUPON_CNT"	, entity.getRemainCouponCnt());
		columnMap.put("COUPON_TYPE"	, entity.getCouponType());
		
		 return das.insert("COUPON_HST", columnMap);
	}

	/**
	 * 쿠폰 사용에 따른 HST 변경
	 * @param das
	 * @return
	 * @throws Exception
	 */

	private int updateCouponHstForUseCouponNew(DAS das, long hstRawId, long academyRawId, String couponType, String applyCouponCount, long applyUserCount, String freeYN) throws Exception  {
		int result = 0;
		try
		{
			LinkedHashMap columnMap = new LinkedHashMap();
			
			if(applyUserCount == -1)
				columnMap.put("APPLY_USER_CNT+(APPLY_USER_CNT + 1)", null);
			else
				columnMap.put("APPLY_USER_CNT", applyUserCount+"");
			
			columnMap.put("APPLY_COUPON_CNT+(APPLY_COUPON_CNT + "+applyCouponCount+")", null);
			columnMap.put("REMAIN_COUPON_CNT", this.getRemainCouponCountNew(das, academyRawId, couponType));
			
			if(freeYN.equals("N"))
			{
				columnMap.put("SPEND_STATUS_CD", "NU");
			}
			else
			{
				columnMap.put("SPEND_STATUS_CD", "FU");
			}
			
			LinkedHashMap whereMap = new LinkedHashMap();
			whereMap.put("RAWID"        , hstRawId);
			whereMap.put("ACADEMY_RAWID", academyRawId);
			whereMap.put("COUPON_TYPE"  , couponType);
			result =  das.update("COUPON_HST", columnMap, "WHERE rawid = ? AND academy_rawid = ? AND coupon_type = ? ", whereMap);
			
		} catch (Exception e) {
			logger.warn(e);
			
		} finally{
			
		}
		return result;
	}
	
	
	
	/**
	 * 쿠폰 부족에 의한 non_apply_user에 의한 로그 변경
	 * @param das
	 * @return
	 * @throws Exception
	 */

	private int updateCouponHstForNonUserNew(DAS das, long hstRawId, String couponType, String freeYN) throws Exception  {
		LinkedHashMap columnMap = new LinkedHashMap();
		columnMap.put("NON_APPLY_USER_CNT + (NON_APPLY_USER_CNT + 1)", null);
		
		
		if(freeYN.equals("N"))
		{
			columnMap.put("SPEND_STATUS_CD", "NU");
		}
		else
		{
			columnMap.put("SPEND_STATUS_CD", "FU");
		}
		LinkedHashMap whereMap = new LinkedHashMap();
		whereMap.put("RAWID", hstRawId);
		whereMap.put("COUPON_TYPE", couponType);
		return das.update("COUPON_HST", columnMap, "WHERE rawid = ? AND coupon_type = ?", whereMap);
	}
	
	

	
	private int insertUserCouponHstNew(DAS das, long academyRawId, long userRawId, String couponType) throws Exception {
		UserInfo ui = this.getUserByRawId(das, userRawId);
		
		LinkedHashMap columnMap = new LinkedHashMap();
		columnMap.put("RAWID+SEQ_USER_COUPON_HST.nextval", null);
		columnMap.put("ACADEMY_RAWID"   , academyRawId);
		columnMap.put("USER_RAWID"      , userRawId);
		columnMap.put("PRE_CP_APPLY_YN"	, ui.getCpapply());
		columnMap.put("PRE_CP_APPLY_DT"	, ui.getCpApplyDate());
		columnMap.put("CREATE_DT+SYSDATE",null);
		columnMap.put("CREATE_BY"		, academyRawId);
		columnMap.put("CREATE_TYPE_CD"	, "A");
		columnMap.put("COUPON_TYPE"	, couponType);
		
		return das.insert("USER_COUPON_HST", columnMap);
	}
	
	private int insertUserCouponHstForSM(DAS das, Date hstDate, ApplyCouponData couponData, UserInfo userInfo, String toApplyYN) throws Exception {
		UserInfo ui = this.getUserByRawIdNew(das, userInfo.getRawId(), couponData.getCouponType());
		
		LinkedHashMap columnMap = new LinkedHashMap();
		columnMap.put("RAWID+SEQ_USER_COUPON_HST.nextval", null);
		columnMap.put("ACADEMY_RAWID"   , userInfo.getAcademyRawId());
		columnMap.put("USER_RAWID"      , userInfo.getRawId());
		columnMap.put("PRE_CP_APPLY_YN"	, ui.getCpapply());
		columnMap.put("TO_CP_APPLY_YN"	, toApplyYN);
		columnMap.put("PRE_CP_APPLY_DT"	, ui.getCpApplyDate());
		columnMap.put("CREATE_DT"		, hstDate);
		columnMap.put("CREATE_BY"		, userInfo.getAcademyRawId());
		columnMap.put("CREATE_TYPE_CD"	, "S");
		columnMap.put("COUPON_TYPE"	, couponData.getCouponType());
		
		return das.insert("USER_COUPON_HST", columnMap);
	}
	
	private CouponHstEntity MakeEmptyCouponHstEntity(String useTypeCD) {
		CouponHstEntity cheHst = new CouponHstEntity();
		cheHst.setUseTypeCode(useTypeCD);
		cheHst.setCreateTypeCode("S");

		return cheHst;
	}
	
	
	
	
	/**
	 ******************************************************************** VitaCampus 쿠폰 Start ********************************************************************
	 */
	
	/**
	 * 1. VitaEnglish 쿠폰 적용
	 */
	private void RunCouponForVitaEnglish()
	{
		this.SetType(CoinType.VITAENGLISH);
		this.RunCoupon();
	}
	 
	/**
	 * 1. VitaEnglish 쿠폰 적용
	 */
	private void RunCouponForKT()
	{
		this.SetType(CoinType.KT);
		this.RunCoupon();
	}
	
	private enum CoinType
	{
		SMENGLISH,YOUNGSIN,VITAENGLISH,KT
	}
	
	private void SetType(CoinType coinType)
	{
		switch(coinType)
		{
			case SMENGLISH :
				this._organizationTypeCD="AC";
				this._franchiseCD ="N";
				this._couponType="NC";
				break;
			case YOUNGSIN :
				this._organizationTypeCD="SR";
				this._couponType="YC";
				break;
			case VITAENGLISH :
				this._franchiseCD ="VT";
				this._couponType="NS";
				break;
			case KT :
				this._franchiseCD ="NK";
				this._couponType="NK";
				break;
		}
	
	}
	
	/**
	 * 이용자에게 쿠폰 할당
	 * 1. 대상 학원(학교)를 가져온다.
	 * 2. 대상 이용자를 가져온다. (cp_apply_yn = 'Y' 인 학생만 가져온다.)
	 * 3. 대상 이용자를 사용가능하게 처리 (USER_COUPON_TRX에 추가)
	 * 3-1. 이용자가 USER_COUPON_TRX에 추가되었는지 확인
	 * 3-1-1. 추가되었으면 다음 이용자로 이동
	 * 3-2. 해당 학원에 남은 쿠폰수 있는지 조사
	 * 3-2-1. 없으면 이용자 상태 변경 (USER_MST에 CP_APPLY_YN = M 상태로 변경)
	 * 3-2-2. 없는 이용자 count 
	 * 3-3. 이용자를 USER_COUPON_TRX에 추가한다.
	 * 3-4. 사용 쿠폰수 count
	 * 3-5. 잔여 쿠폰수 빼기 
	 * 4. 쿠폰수 감소 (ACADEMY_COUPON_MST의 REMAIN_COUPON_COUNT 감소)
	 * 5. 쿠폰수 사용내역 저장
	 * 6. 다음 학원 처리
	 * 7. 종료
	 */
	private void RunCoupon(){
		DAS dasTransaction = null;
		ArrayList<AcademyCoupon> alAcadmyCoupon = this.GetTargetAcademyRawIdNew(); 
		
		try {
			dasTransaction = new DAS(DAS_CONFIG);
			String couponType = this._couponType;
			
			for (AcademyCoupon academyCoupon : alAcadmyCoupon) {
				logger.info(couponType+"Coupon ACADEMY_RAWID:" + academyCoupon.getRawId() + " - start");
				CouponHstEntity cheHst = this.MakeEmptyCouponHstEntity("D");
				
				ArrayList<UserInfo> alUserInfo = this.GetTargetUserByAcademyRawId(dasTransaction, academyCoupon.getRawId());
				for (UserInfo userInfo : alUserInfo) {
				
					if (this.existUserCouponTrxNew(dasTransaction, userInfo.getAcademyRawId(), userInfo.getRawId(), nextRunDay, couponType))
						continue; //3-1-1
					
					dasTransaction.beginTrans();
					long academyRawId = userInfo.getAcademyRawId();
					long userRawId = userInfo.getRawId();
					long hstRawId = cheHst.getRawId();
					if (hstRawId < 0){
						hstRawId = this.getSequenceOfCouponHst(dasTransaction);
						cheHst.setRawId(hstRawId);
						cheHst.setAcademyRawId(academyRawId);
						cheHst.setRemainCouponCnt(this.getRemainCouponCountNew(dasTransaction, academyRawId, couponType));
						cheHst.setCouponType(couponType);
						cheHst.setApplyDate(this.getCurrentDate(dasTransaction));
						this.insertCouponHstNew(dasTransaction, cheHst);
					}
					
					long remainCouponCnt = this.getRemainCouponCountNew(dasTransaction, academyRawId, couponType);
					int addUpCnt = 0;
					
//                  'M'인 상태 이용자는 로그(coupon_hst)의 카운트에서 제외.
//					이용자 가져올 때부터 가져오지 않는 것으로 처리
//					주석 처리됨. 추후 삭제
//					if (userInfo.getCpapply().equals("M")){ // 쿠폰부족으로 인한 정지상태
//						addUpCnt = this.updateCouponHstForNonUser(dasTransaction, hstRawId);
//						addUpCnt++;
//						addUpCnt++;
//					} else {
						if (remainCouponCnt <= 0){
							// 쿠폰수 부족으로 인한 처리
							addUpCnt += this.updateUserCpApply(dasTransaction, userRawId);
							addUpCnt += this.updateCouponHstForNonUserNew(dasTransaction, hstRawId, couponType, "N");
							addUpCnt += this.insertUserCouponHstNew(dasTransaction, academyRawId, userRawId, couponType);
						}else {
							addUpCnt += this.insertUserCouponTrx(dasTransaction, cheHst.getApplyDate(), academyRawId, userRawId, couponType);
							addUpCnt += this.updateAcademyCouponMstNew(dasTransaction, academyRawId, couponType, "1");
							addUpCnt += this.updateCouponHstForUseCouponNew(dasTransaction, hstRawId, academyRawId, couponType, "1", -1,"N");
						}
//					}
					if (addUpCnt == 3)
						dasTransaction.commit();
					else {
						dasTransaction.rollback();
						logger.error(String.format("USER 처리 실패! (academy_rawid:%d, user_rawid:%d)", academyRawId, userRawId)); 
					}
				}
			}
		} catch (Exception e) {
			logger.warn(e);
			if (dasTransaction != null) try { dasTransaction.rollback(); } catch(Exception ex) {}
		} finally{
			//if (dasTransaction != null) try { dasTransaction.commit(); } catch(Exception ex) {}
			if (dasTransaction != null) try { dasTransaction.close(); } catch(Exception ex) {}
		}
	}
	 

	
	private void RunRemainCheck()
	{ 
		DAS das = null;

		try
		{
			das = new DAS("EN");
			String sms_msg = "2일 사용 가능한 쿠폰이 남았습니다. 확인하시고 필요하시면 더 충전하여 주세요.";
			String sql = "select am.RAWID, am.NAME, am.CELL_NUMBER, cm.REMAIN_COUPON_COUNT from academy_mst am, (                        " +
			"select acm.ACADEMY_RAWID, acm.REMAIN_COUPON_COUNT from academy_coupon_mst acm   " +
			"where (acm.REMAIN_COUPON_COUNT   " +
			"between (select count(*) from user_mst um where um.CP_APPLY_YN = 'Y' and um.academy_rawid = acm.academy_rawid) * 1    " +
			"and (select count(*) from user_mst um where um.CP_APPLY_YN = 'Y' and um.academy_rawid = acm.academy_rawid) * 2)  " +
			"and acm.REMAIN_COUPON_COUNT > 0 " +
			"and acm.coupon_type='NS' ) cm  " +
			"where am.rawid = cm.academy_rawid and am.franchise_cd='VT' ";

	
			String sql2 = "SELECT AM.USER_ID, AM.RAWID, SM.REMAIN_COUNT, AM.CELL_NUMBER " +
						  "  FROM SMS_ACADEMY_MST SM, ACADEMY_MST AM " +
						  " WHERE SM.ACADEMY_RAWID = AM.RAWID AND AM.USER_ID = 'koryo'  ";
			RecordSet rset2 = das.select(sql2, null);
			
			if (rset2.next() && (rset2.getLong("REMAIN_COUNT") > 0)) {
				long remain_count = rset2.getLong("REMAIN_COUNT");
				 
				if (remain_count > 0) {
					RecordSet rset = das.select(sql, null);
					LinkedHashMap columnMap = new LinkedHashMap();
					
					das.beginTrans();
					long seq_em_tran_pr = -1; 
					while(rset.next())
					{
						if (seq_em_tran_pr < 0) seq_em_tran_pr = das.getSequence_Nextval("EM_TRAN_PR");
						
						if ((seq_em_tran_pr > 0))
						{
							String aName = rset.getString("NAME");
							if (aName.length() > 5)
								aName = aName.substring(0, 5);
							
							
							columnMap.clear();
							columnMap.put("TRAN_PR"				, seq_em_tran_pr);
							columnMap.put("TRAN_PHONE"		    , rset.getString("CELL_NUMBER"));
							columnMap.put("TRAN_CALLBACK"		,"02-583-4353");
							columnMap.put("TRAN_STATUS" 	    , "1");
							columnMap.put("TRAN_DATE+SYSDATE"	, null);
							columnMap.put("TRAN_MSG"		    , sms_msg);
							columnMap.put("TRAN_ETC1"			, aName);
							columnMap.put("TRAN_ETC2"			, "SMEnglish");
							columnMap.put("TRAN_ETC3"			, "M");
							columnMap.put("TRAN_TYPE"			, "4");
							das.insert("EM_TRAN"		, columnMap);
					
							//4.SMS_ACADEMY_MST update
							columnMap.clear();
							columnMap.put("REMAIN_COUNT+(REMAIN_COUNT - 1)", null);
						
							LinkedHashMap whereMap = new LinkedHashMap();
							whereMap.put("ACADEMY_RAWID"  ,  rset2.getLong("RAWID") + "");
							das.update("SMS_ACADEMY_MST", columnMap, "WHERE academy_rawid = ?  ", whereMap);
							
							das.commit();
						} else {
							das.rollback();
						}
					}
				}
			}
			
		} catch (Exception e)
		{
			logger.warn(e);
			das.rollback(); 
		} finally
		{
			if (das != null) try { das.close(); } catch(Exception ex) {}
		}
	}
	
	
//	private void RunRemainCheck()
//	{ 
//		DAS das = null;
//
//		try
//		{
//			das = new DAS("EN");
//			String sms_msg = "2일 사용 가능한 쿠폰이 남았습니다. 확인하시고 필요하시면 더 충전하여 주세요.";
//			String sql = "select am.RAWID, am.NAME, am.CELL_NUMBER, cm.REMAIN_COUPON_COUNT from academy_mst am, (                        " +
//			"select acm.ACADEMY_RAWID, acm.REMAIN_COUPON_COUNT from academy_coupon_mst acm   " +
//			"where (acm.REMAIN_COUPON_COUNT   " +
//			"between (select count(*) from user_mst um where um.CP_APPLY_YN = 'Y' and um.academy_rawid = acm.academy_rawid) * 1    " +
//			"and (select count(*) from user_mst um where um.CP_APPLY_YN = 'Y' and um.academy_rawid = acm.academy_rawid) * 2)  " +
//			"and acm.REMAIN_COUPON_COUNT > 0 " +
//			"and acm.coupon_type='NS' ) cm  " +
//			"where am.rawid = cm.academy_rawid and am.franchise_cd='VT' ";
//
////			String sms_msg_format = "학원의 수강권이 %s일 동안 사용가능합니다. 추가 구매 하세요.";
////			String sql = "" +
////			"SELECT am.rawid, am.name, am.cell_number, cm.remain_coupon_count, cm.ucnt " +
////			"  FROM academy_mst am,                          " +
////			"       (SELECT acm.academy_rawid, acm.remain_coupon_count, umc.ucnt  " +
////			"          FROM academy_coupon_mst acm, ( " +
////			"               (select academy_rawid, count(rawid) ucnt " +
////			"                  from user_mst " +
////			"                 where cp_apply_yn = 'y' " +
////			"                 group by academy_rawid ) umc    " +
////			"         WHERE acm.academy_rawid = umc.academy_rawid " +
////			"           AND acm.remain_coupon_count between umc.ucnt and umc.ucnt * 5   " +
////			"           AND acm.remain_coupon_count > 0) cm   " +
////			" WHERE am.rawid = cm.academy_rawid ";
//			
//			
//			String sql2 = "SELECT AM.USER_ID, AM.RAWID, SM.REMAIN_COUNT, AM.CELL_NUMBER " +
//						  "  FROM SMS_ACADEMY_MST SM, ACADEMY_MST AM " +
//						  " WHERE SM.ACADEMY_RAWID = AM.RAWID AND AM.USER_ID = 'koryo'  ";
//			RecordSet rset2 = das.select(sql2, null);
//			
//			if (rset2.next() && (rset2.getLong("REMAIN_COUNT") > 0)) {
//				long remain_count = rset2.getLong("REMAIN_COUNT");
//				 
//				if (remain_count > 0) {
//					RecordSet rset = das.select(sql, null);
//					int destCount = 0;
//					String destInfo = "";
//					LinkedHashMap columnMap = new LinkedHashMap();
//					
//					das.beginTrans();
//					long seq_sqm_academy_trx = -1; 
//					while(rset.next())
//					{
//						if (seq_sqm_academy_trx < 0) seq_sqm_academy_trx = das.getSequence_Nextval("SEQ_SMS_ACADEMY_TRX");
//						columnMap.clear();
//						columnMap.put("RAWID+SEQ_SMS_USER_TRX.nextval", null);
//						columnMap.put("SMS_ACADEMY_TRX_RAWID", rset2.getLong("RAWID"));
//						//columnMap.put("SMS_ACADEMY_TRX_RAWID", seq_sqm_academy_trx);
//						columnMap.put("USER_RAWID",rset.getLong("rawid"));
//						columnMap.put("CALL_NUMBER",rset.getString("CELL_NUMBER"));
//						columnMap.put("CREATE_DT+SYSDATE",null);
//						das.insert("SMS_USER_TRX", columnMap);
//						
//						destCount++;
//						
//						if (destInfo.length() > 0) destInfo += "|";
//						String aName = rset.getString("NAME");
//						if (aName.length() > 5)
//							aName = aName.substring(0, 5);
//						destInfo = destInfo + aName + "^" + rset.getString("CELL_NUMBER");
//					}
//					
//					if ((seq_sqm_academy_trx > 0) && (destCount < rset2.getLong("REMAIN_COUNT")))
//					{
//						columnMap.clear();
//						columnMap.put("RAWID", seq_sqm_academy_trx);
//						columnMap.put("ACADEMY_RAWID", rset2.getLong("RAWID"));
//						//columnMap.put("ACADEMY_RAWID", 1);
//						columnMap.put("CALL_COUNT",destCount);
//						columnMap.put("SEND_DT+SYSDATE","");
//						columnMap.put("SEND_MSG",sms_msg);
////						string msg = String.format(sms_msg_format, Math.round(a));
////						columnMap.put("SEND_MSG", msg);
//						columnMap.put("SENDER_RAWID", rset2.getLong("RAWID"));
//						//columnMap.put("SENDER_RAWID", "111");
//						columnMap.put("USER_TYPE","A");
//						columnMap.put("CREATE_DT+SYSDATE","");
//						das.insert("SMS_ACADEMY_TRX", columnMap);
//						
//						// ???????? ??? table?? ???
//						columnMap.clear();
//						columnMap.put("USER_ID", rset2.getString("USER_ID"));
//						//columnMap.put("USER_ID", "11");
//						columnMap.put("SCHEDULE_TYPE", "0");
//						columnMap.put("SUBJECT", "");
//						columnMap.put("NOW_DATE+TO_CHAR(SYSDATE, 'YYYYMMDDHH24MI')","");
//						columnMap.put("SEND_DATE+TO_CHAR(SYSDATE, 'YYYYMMDDHH24MI')","");
//						columnMap.put("CALLBACK", rset2.getString("CELL_NUMBER"));
//						//columnMap.put("CALLBACK", "11");
//						columnMap.put("DEST_COUNT", destCount);
//						columnMap.put("DEST_INFO", destInfo);
//						columnMap.put("SMS_MSG",sms_msg);
//						//columnMap.put("MSG_ID","1");
//						das.insert("SDK_SMS_SEND", columnMap);
//				
//						das.commit();
//					} else {
//						das.rollback();
//					}
//				}
//			}
//			
//		} catch (Exception e)
//		{
//			logger.warn(e);
//			das.rollback(); 
//		} finally
//		{
//			if (das != null) try { das.close(); } catch(Exception ex) {}
//		}
//	}
	
	
	/**
	 ******************************************************************** VitaCampus 쿠폰 End ********************************************************************
	 */
	
	
	/**
	 ******************************************************************** 영신 쿠폰 Start ********************************************************************
	 */
	
	
	/**
	 * 1. SMEnglish,내신 동시 사용한 학생 환불
	 * 2. SMEnglish쿠폰 적용
	 */
	private void RunCouponForYoungSin()
	{
		this.SetType(CoinType.YOUNGSIN);
		this.GetRequireDateList();
		this.ApplyCoin();
	}
	 
	/**
	 ******************************************************************** 영신 쿠폰 End ********************************************************************
	 */
	
	
	
	
	
	
	/**
	 ******************************************************************** SMEnglish 쿠폰 Start ********************************************************************
	 */
	
	
	
	/**
	 * 1. SMEnglish,내신 동시 사용한 학생 환불->삭제됨
	 * 2. SMEnglish코인 적용
	 *    .CoinType set
	 *    .필수기간을 처리할때 오늘부터 7일간의 날짜를 구함
	 *    .코인적용
	 */
	private void RunCouponForSM()
	{
		this.SetType(CoinType.SMENGLISH);
		//내신이 서비스되지 않으므로 삭제 2012.2.22
		//this.ApplyRefundCouponForSM();
		this.GetRequireDateList();
		this.ApplyCoinTmp(); // 12시에 가장 먼저 코인처리해야되는 학원을 임시로 추가함
		this.ApplyCoin();
	}
	 
	
	// *********************************************************** SMEnglish,내신 동시 사용한 학생 환불 Start ***********************************************************
	/**
	 * 전날 SMEnglish,내신 동시 사용한 학생들에게 가상쿠폰을 SM쿠폰으로 환산해서 환불
	 * 1. 동시에 사용한 학생 저장
	 * 2. 가상쿠폰을  SM쿠폰으로 환불
	 * 2-1. 학원에 남아있는 가상쿠폰 가져오기
	 * 2-2. 가상쿠폰, SM쿠폰쿠폰, 남는 가상쿠폰 계산
	 * 2-3. 학원별 각 쿠폰개수 저장
	 * 2-4. 남은 가상쿠폰 저장
	 * 2-5. 환산한 SM쿠폰 AcademyCouponTrx저장
	 * 2-6. 환산한 SM쿠폰 + 현재남은 쿠폰 저장
	 * 3. 종료
	 */
	private void ApplyRefundCouponForSM()
	{
		DAS das = null;
		ArrayList<AcademyCoupon> alAcadmyCoupon = this.GetTargetAcademyForRefund();
		
		
		try {
			das = new DAS(DAS_CONFIG);
			String couponType = "NC";
			String useTypeCD = "RF";
			
			for (AcademyCoupon academyCoupon : alAcadmyCoupon) {
				
				//1-1. ACADEMY_VIRTUAL_COUPON_TRX에 해당 학원이 있으면 제외
				if (this.existAcademyVirtualCouponTrx(das, academyCoupon.getRawId(), beforeRunDay))
					continue; 
				
				
				logger.info("Refund ACADEMY_RAWID:" + academyCoupon.getRawId() + " - start");
				CouponHstEntity cheHst = this.MakeEmptyCouponHstEntity(useTypeCD);

				das.beginTrans();
				long academyRawId       = academyCoupon.getRawId();
				
				//0. Coupon_Hst체크
				this.CheckCouponHstRawId(das, cheHst, academyRawId, couponType);
			
				//1. 동시에 사용한 학생 저장
				ArrayList<UserInfo> alUserInfo = this.GetTargetUserForRefund(academyRawId);
				int refundUserCount = 0;
				for (UserInfo userInfo : alUserInfo) 
				{
					
					//1-1. USER_VIRTUAL_COUPON_TRX에 해당 학생이 있으면 제외
					if (this.existUserVirtualCouponTrx(das, academyRawId, userInfo.getRawId(), beforeRunDay))
						continue; 
					
					//1-2. USER_VIRTUAL_COUPON_TRX 에 저장
					//logger.info("Refund USER_RAWID:" + userInfo.getRawId() + " - start");
					long userRawId = userInfo.getRawId();
					this.insertUserVirtualCouponTrx(das, cheHst, academyRawId, userRawId);
					refundUserCount++;
				}
				

				//2. 가상쿠폰을  SM쿠폰으로 환불
				//2-1. 학원에 남아있는 가상쿠폰 가져오기
				long acadmyRemainVirtualCount = this.GetRemainVirtualCoupon(academyRawId);
				long beforeVirtualCount = 0;
				boolean isExistRemainVirturalCoupon  = false;
				
				if(acadmyRemainVirtualCount >= 0){
					isExistRemainVirturalCoupon = true;
					beforeVirtualCount = acadmyRemainVirtualCount;
				}
				
				
								
				//2-2. 가상쿠폰, SM쿠폰쿠폰, 남는 가상쿠폰 계산
				if(refundUserCount > 0 )
				{
					long virtualCouponCount = beforeVirtualCount + refundUserCount * 4;
					long refundCouponCount = virtualCouponCount / 5;
					long remainVirtualCouponCount = virtualCouponCount % 5;
		
					
					//2-3. 학원별 가상 쿠폰개수 저장
					//ACADEMY_VIRTUAL_COUPON_TRX
					this.insertAcademyVirtualCouponTrx(das, cheHst, academyRawId, beforeVirtualCount, virtualCouponCount, refundCouponCount, remainVirtualCouponCount);
					
					
					//2-4. 남은 가상쿠폰 저장
					//ACADEMY_VIRTUAL_COUPON_MST
					if(remainVirtualCouponCount > 0)
					{
						if(isExistRemainVirturalCoupon)
							this.updateAcademyVirtualCouponMst(das, cheHst.getApplyDate(), academyRawId, remainVirtualCouponCount);
						else
							this.insertAcademyVirtualCouponMst(das, cheHst.getApplyDate(), academyRawId, remainVirtualCouponCount);
					}
					
					//2-5. 환산한 SM쿠폰 저장
					if(refundCouponCount > 0)
					{
						//2-5. 환산한 SM쿠폰 AcademyCouponTrx저장
						long academyCouponTrxRawId = this.getSequenceOfAcademyCouponTrx(das);
						this.insertAcademyCouponTrx(das, cheHst.getApplyDate(), academyCouponTrxRawId, academyRawId, refundCouponCount, "R", "0");
					
						//2-6. 환산한 SM쿠폰 + 현재남은 쿠폰 저장
						//ACADEMY_COUPON_MST
						long remainSMCouponCount = this.GetAcademyCouponMst(das, academyRawId);
						if(remainSMCouponCount > -1)
							this.updateAcademyCouponMst(das, academyRawId, remainSMCouponCount + refundCouponCount);
						else
						{
							this.insertAcademyCouponMst(das, academyRawId, refundCouponCount);
						}
						
						//COUPON_HST update
						int addUpCnt = this.updateCouponHstForUseCouponNew(das, cheHst.getRawId(), academyRawId, couponType, refundCouponCount+"", refundUserCount, "N");
						
					}
				}
				das.commit();
			}
		} catch (Exception e) {
			logger.warn(e);
			if (das != null) try { das.rollback(); } catch(Exception ex) {}
		} finally{
			if (das != null) try { das.close(); } catch(Exception ex) {}
		}
	}
	
	
	
	
	private boolean existAcademyVirtualCouponTrx(DAS das, long academyRawId, String applyDay){

		RecordSet rset = null;
		boolean result = false;
		try {
			String sql = "SELECT academy_rawid " +
						 "  FROM academy_virtual_coupon_trx " +
						 " WHERE academy_rawid = ? " +
						 "   AND apply_day     = ? " ;
						
			LinkedHashMap keyList = new LinkedHashMap();
			keyList.put("ACADEMY_RAWID", academyRawId + "");
			keyList.put("APPLY_DAY", applyDay);
		
			
			rset = das.select(sql, keyList);
			if (rset.next())
				result = true;
			else
				result = false;
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
		}
		
		return result;
	}

	
	
	
	
	private boolean existUserVirtualCouponTrx(DAS das, long academyRawId, long userRawId, String applyDay){

		RecordSet rset = null;
		boolean result = false;
		try {
			String sql = "SELECT user_rawid " +
						 "  FROM user_virtual_coupon_trx " +
						 " WHERE academy_rawid = ? " +
						 "   AND user_rawid    = ? " +
						 "   AND apply_day     = ? " ;
						
			LinkedHashMap keyList = new LinkedHashMap();
			keyList.put("ACADEMY_RAWID", academyRawId + "");
			keyList.put("USER_RAWID", userRawId + "");
			keyList.put("APPLY_DAY", applyDay);
		
			
			rset = das.select(sql, keyList);
			if (rset.next())
				result = true;
			else
				result = false;
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
		}
		
		return result;
	}

	
	
	/**
	 * 전날 SMEnglish,내신 동시 사용한 학원 가져오기
	 * @param 
	 * @return
	 */
	private ArrayList<AcademyCoupon> GetTargetAcademyForRefund(){
		DAS das = null;
		RecordSet rset = null;
		ArrayList<AcademyCoupon> alAcadmyCoupon = new ArrayList<AcademyCoupon>();
		
		
		try {
			das = new DAS(DAS_CONFIG);
			String sql =" SELECT sm.academy_rawid				     "+                                        
						" FROM                                       "+
						" ACADEMY_MST AM "+
						" INNER JOIN "+
						" (                                          "+                                        
						" 		SELECT academy_rawid, user_rawid     "+
						" 		 FROM user_coupon_trx                "+
						" 		WHERE apply_day = ?            		 "+
						" 		  AND coupon_type='NC'               "+
						" )sm ON AM.RAWID=SM.ACADEMY_RAWID           "+                                        
						" INNER JOIN                                 "+
						" (                                          "+                             
						" 		SELECT academy_rawid, user_rawid     "+
						" 		FROM user_coupon_trx                 "+
						" 		WHERE apply_day = ?             	 "+
						" 		  AND coupon_type='NS'               "+
						" )ns ON sm.user_rawid=ns.user_rawid         "+
						"  AND sm.academy_rawid=ns.academy_rawid     "+
						" WHERE AM.BILL_YN='Y' AND AM.FRANCHISE_CD='N' AND AM.ORGANIZATION_TYPE_CD='AC' "+
						"   AND AM.IS_USED='A' AND AM.SM_COUPON_YN='Y' AND AM.SM_COUPON_START_DT < SYSDATE "+
						"  GROUP BY sm.academy_rawid				 "; 

			LinkedHashMap keyList = new LinkedHashMap();
			keyList.put("APPLY_DAY1", beforeRunDay);
			keyList.put("APPLY_DAY2", beforeRunDay);
			rset = das.select(sql, keyList);
			
			while(rset.next()){
				AcademyCoupon ac = new AcademyCoupon();
				ac.setRawId(rset.getLong("academy_rawid"));
				alAcadmyCoupon.add(ac);
			}
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
			if (das != null) try { das.close(); } catch(Exception ex) {}
		}
		return alAcadmyCoupon;
	}
	
		
	/**
	 * 전날 SMEnglish,내신 동시 사용한 학생 가져오기
	 * @param 
	 * @return
	 */
	private ArrayList<UserInfo> GetTargetUserForRefund(long academyRawId){
		DAS das = null;
		RecordSet rset = null;
		ArrayList<UserInfo> alUserInfo = new ArrayList<UserInfo>();

		try {
			das = new DAS(DAS_CONFIG);
			String sql =" SELECT sm.academy_rawid, sm.user_rawid     "+                                        
						" FROM                                       "+                                        
						" (                                          "+                                        
						" 		SELECT academy_rawid, user_rawid     "+
						" 		 FROM user_coupon_trx                "+
						" 		WHERE apply_day = ?            		 "+
						" 		  AND academy_rawid = ?              "+
						" 		  AND coupon_type='NC'               "+
						" )sm                                        "+                                        
						" INNER JOIN                                 "+
						" (                                          "+                             
						" 		SELECT academy_rawid, user_rawid     "+
						" 		FROM user_coupon_trx                 "+
						" 		WHERE apply_day = ?             	 "+
						" 		  AND academy_rawid = ?              "+
						" 		  AND coupon_type='NS'               "+
						" )ns ON sm.user_rawid=ns.user_rawid         ";
						

			LinkedHashMap keyList = new LinkedHashMap();
			keyList.put("APPLY_DAY1", beforeRunDay);
			keyList.put("ACADEMY_RAWID1", academyRawId);
			keyList.put("APPLY_DAY2", beforeRunDay);
			keyList.put("ACADEMY_RAWID2", academyRawId);
			
			rset = das.select(sql, keyList);
			
			while(rset.next()){
				UserInfo ui = new UserInfo();
				ui.setAcademyRawId(rset.getLong("academy_rawid"));
				ui.setRawId(rset.getLong("user_rawid"));
							
				alUserInfo.add(ui);
			}
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
			if (das != null) try { das.close(); } catch(Exception ex) {}
		}
		return alUserInfo;
	}
	
	
	
	/**
	 * 학원별 남은 가상쿠폰 가져오기
	 */
	private long GetRemainVirtualCoupon(long academyRawId){
		DAS das = null;
		RecordSet rset = null;
		LinkedHashMap keyList = new LinkedHashMap();
		long remainVirtualCount = -1;
		
		try {
			das = new DAS(DAS_CONFIG);
			String sql =" SELECT academy_rawid, virtual_coupon_count "+
						"   FROM academy_virtual_coupon_mst          "+
						"  WHERE academy_rawid = ?			         ";
			
			keyList.put("ACADEMY_RAWID"     , academyRawId);
			rset = das.select(sql, keyList);
			
			while(rset.next()){
				remainVirtualCount = rset.getLong("virtual_coupon_count");
			}
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
			if (das != null) try { das.close(); } catch(Exception ex) {}
		}
		return remainVirtualCount;
	}
	
	
	/**
	 * 학생별 가상쿠폰 저장
	 */
	private int insertUserVirtualCouponTrx(DAS das, CouponHstEntity cheHst, long academyRawId, long userRawId) throws Exception {
		LinkedHashMap columnMap = new LinkedHashMap();
		columnMap.put("RAWID+SEQ_USER_VIRTUAL_COUPON_TRX.nextval", null);
		columnMap.put("ACADEMY_RAWID"     , academyRawId);
		columnMap.put("USER_RAWID"        , userRawId);
		columnMap.put("APPLY_DAY"	      , beforeRunDay);
		columnMap.put("CREATE_DT"         , cheHst.getApplyDate());
	
		return das.insert("USER_VIRTUAL_COUPON_TRX", columnMap);
	}
	
	
	/**
	 * 학원별 가상쿠폰 저장
	 */
	private int insertAcademyVirtualCouponTrx(DAS das, CouponHstEntity cheHst, long academyRawId, long beforeVirtualCount, long virtualCouponCount
			,long refundCouponCount, long remainCouponCount) throws Exception {
		
		LinkedHashMap columnMap = new LinkedHashMap();
		columnMap.put("RAWID+SEQ_ACADEMY_VIRTUAL_COUPON_TRX.nextval", null);
		columnMap.put("ACADEMY_RAWID"   		 , academyRawId);
		columnMap.put("BEFORE_VIRTUAL_COUNT"     , beforeVirtualCount);
		columnMap.put("VIRTUAL_COUPON_COUNT"     , virtualCouponCount);
		columnMap.put("REFUND_COUPON_COUNT"      , refundCouponCount);
		columnMap.put("REMAIN_COUPON_COUNT"      , remainCouponCount);
		columnMap.put("APPLY_DAY"				 , beforeRunDay);
		columnMap.put("CREATE_DT		"		 , cheHst.getApplyDate());
	
		return das.insert("ACADEMY_VIRTUAL_COUPON_TRX", columnMap);
	}
	
	
	/**
	 * 학원별 Remain 가상쿠폰 update
	 */
	private int updateAcademyVirtualCouponMst(DAS das, Date hstDate, long academyRawId, long remainCouponCount) throws Exception {
		
		LinkedHashMap columnMap = new LinkedHashMap();
		columnMap.put("VIRTUAL_COUPON_COUNT", remainCouponCount);
		
		LinkedHashMap whereMap = new LinkedHashMap();
		whereMap.put("ACADEMY_RAWID", academyRawId + "");
		return das.update("ACADEMY_VIRTUAL_COUPON_MST", columnMap, "where ACADEMY_RAWID = ?", whereMap);
	}
	
	/**
	 * 학원별 Remain 가상쿠폰 insert
	 */
	private int insertAcademyVirtualCouponMst(DAS das, Date hstDate, long academyRawId, long remainCouponCount) throws Exception {
		
		LinkedHashMap columnMap = new LinkedHashMap();
		columnMap.put("RAWID+SEQ_ACADEMY_VIRTUAL_COUPON_MST.nextval", null);
		columnMap.put("ACADEMY_RAWID"        , academyRawId);
		columnMap.put("VIRTUAL_COUPON_COUNT" , remainCouponCount);
		columnMap.put("CREATE_DT" , hstDate);
		
		return das.insert("ACADEMY_VIRTUAL_COUPON_MST", columnMap);
	}
	
	
	
	/**
	 * AcademyCouponTrx Sequence가져오기
	 */
	private long getSequenceOfAcademyCouponTrx(DAS das) throws Exception {
		return das.getSequence_Nextval("SEQ_ACADEMY_COUPON_TRX");
	}
	
	
	/**
	 * 학원별 SM쿠폰 insert
	 */
	private int insertAcademyCouponTrx(DAS das, Date hstDate, long academyCouponTrxRawId, long academyRawId, long couponCount, String payType, String lgdOID) throws Exception {
		
		LinkedHashMap columnMap = new LinkedHashMap();
		columnMap.put("RAWID"			 , academyCouponTrxRawId);
		columnMap.put("ACADEMY_RAWID"    , academyRawId);
		columnMap.put("CREATE_TYPE_CD"   , "A");
		columnMap.put("CREATE_BY"        , academyRawId);
		columnMap.put("PAY_COUPON_COUNT" , couponCount);
		columnMap.put("PAY"              , 0);
		columnMap.put("CREATE_DT"		 , hstDate);
		columnMap.put("LGD_OID"          , lgdOID);
		columnMap.put("PAY_YN"           , "Y");
		columnMap.put("PAY_DT+SYSDATE"   , null);
		columnMap.put("COUPON_TYPE"      , "NC");
		columnMap.put("PAY_TYPE"         , payType);
		
		return das.insert("ACADEMY_COUPON_TRX", columnMap);
	}
	
	
	/**
	 * 학원별 SM쿠폰 update
	 */
	private int updateAcademyCouponTrx(DAS das, long academyCouponTrxRawId, long academyRawId, long couponCount) throws Exception {
		
		LinkedHashMap columnMap = new LinkedHashMap();
		columnMap.put("PAY_COUPON_COUNT+(PAY_COUPON_COUNT + "+couponCount+")", null);
		
		LinkedHashMap whereMap = new LinkedHashMap();
		whereMap.put("RAWID", academyCouponTrxRawId + "");
		return das.update("ACADEMY_COUPON_TRX", columnMap, "WHERE RAWID = ?", whereMap);

	}
	
	
	/**
	 * 학원별 남은 SM쿠폰이 있는지 체크
	 */
	private long GetAcademyCouponMst(DAS das, long academyRawId){
		RecordSet rset = null;
		long result =-1;
		try {
			String sql =" SELECT academy_rawid, remain_coupon_count "+
						"   FROM academy_coupon_mst                 "+
						"  WHERE academy_rawid in                   "+
						"            (                              "+
						"                 SELECT rawid              "+
						"                 FROM academy_mst          "+
						"                 WHERE sm_coupon_yn = 'Y'  "+
						"                   AND del_yn = 'N'        "+
						"                   AND is_used = 'A'       "+  
						"             )                             "+
						"   AND coupon_type = ?                    "+
						"   AND academy_rawid = ?                     ";
						 
			LinkedHashMap keyList = new LinkedHashMap();
			keyList.put("coupon_type", this._couponType);
			keyList.put("ACADEMY_RAWID", academyRawId + "");
			rset = das.select(sql, keyList);
			if (rset.next())
			{
				result = rset.getLong("remain_coupon_count");
			}
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
		}
		return result;
	}
	
	
	/**
	 * 학원별 환불쿠폰+남은 SM쿠폰 update
	 */
	private int updateAcademyCouponMst(DAS das, long academyRawId, long CouponCount) throws Exception {
		
		LinkedHashMap columnMap = new LinkedHashMap();
		columnMap.put("REMAIN_COUPON_COUNT", CouponCount);
		
		LinkedHashMap whereMap = new LinkedHashMap();
		whereMap.put("ACADEMY_RAWID", academyRawId + "");
		whereMap.put("coupon_type", this._couponType);
		return das.update("ACADEMY_COUPON_MST", columnMap, "WHERE academy_rawid = ? AND coupon_type = ? ", whereMap);
	}
	
	
	
	/**
	 * 학원별 환불쿠폰+남은쿠폰 insert
	 */
	private int insertAcademyCouponMst(DAS das, long academyRawId, long CouponCount) throws Exception {
		
		LinkedHashMap columnMap = new LinkedHashMap();
		columnMap.put("RAWID+SEQ_ACADEMY_COUPON_MST.nextval", null);
		columnMap.put("ACADEMY_RAWID"       , academyRawId);
		columnMap.put("REMAIN_COUPON_COUNT" , CouponCount);
		columnMap.put("CREATE_DT+SYSDATE"   , null);
		columnMap.put("COUPON_TYPE"         , "NC");

		return das.insert("ACADEMY_COUPON_MST", columnMap);
	}
	
	// ************************************************************ SMEnglish,내신 동시 쓰는 학생 환불 End **************************************************************
	
	
	
	
	
	
	// **************************************************************** SMEnglish쿠폰 Start *************************************************************************
	
	private enum CodeType
	{
		SNC,NORMAL,NORMALOVER,REQUIREDOVER,STARTREQUIRE
	}
	
	private enum RenewType
	{
		NORMAL,TODAYSTART
	}
	
	//필수기간을 처리할때 오늘부터 7일간의 날짜를 구함
	private void GetRequireDateList()
	{
		ArrayList dateList = new ArrayList();
		try
		{
			SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
			Date date = format.parse(this.nextRunDay);
			Calendar cal = Calendar.getInstance();
			cal.setTime(date); 
			dateList.add(this.nextRunDay);
			for(int i=1; i < 7; i++)
			{
				cal.add(Calendar.DATE, 1);
				String strDate = format.format(cal.getTime());
				dateList.add(strDate);
			}
			
			this.requireDateList = dateList;
		} catch (Exception e)
		{
			logger.warn(e);
		} finally{

		}
	}
	
	
	private String GetAddedDate(String strDate)
	{
		try
		{
			SimpleDateFormat format = new SimpleDateFormat("yyyyMMdd");
			Date date = format.parse(strDate);
			Calendar cal = Calendar.getInstance();
			cal.setTime(date); 

			cal.add(Calendar.DATE, 1);
			strDate = format.format(cal.getTime());
		
		} catch (Exception e)
		{
			logger.warn(e);
		} finally{

		}
		return strDate;
	}
	
	
	/**
	 * 소속학급이 없는 학생은 사용정지한다. : couponWorkTypeCD='C'
	 * 필수사용기간이 아닌 학생중 출판사가 3개 이하인 학생차감 : couponWorkTypeCD='B'
	 * 필수사용기간이 아닌 학생중 출판사가 3개 초과인 학생차감 : couponWorkTypeCD='B'
	 * 필수사용기간인 학생중 출판사가 3개이상인 학생차감 : couponWorkTypeCD='RAC'
	 * 이용자에게 쿠폰 할당
	 * 1. 대상 학원(학교)를 가져온다.
	 * 2. 대상 이용자를 가져온다. (cp_apply_yn = 'Y' 인 학생만 가져온다.)
	 * 3. 대상 이용자를 사용가능하게 처리 (USER_COUPON_TRX에 추가)
	 * 3-1. 이용자가 USER_COUPON_TRX에 추가되었는지 확인
	 * 3-1-1. 추가되었으면 다음 이용자로 이동
	 * 3-2. 해당 학원에 남은 쿠폰수 있는지 조사
	 * 3-2-1. 없으면 이용자 상태 변경 (USER_MST에 CP_APPLY_YN = M 상태로 변경)
	 * 3-2-2. 없는 이용자 count 
	 * 3-3. 이용자를 USER_COUPON_TRX에 추가한다.
	 * 3-4. 사용 쿠폰수 count
	 * 3-5. 잔여 쿠폰수 빼기 
	 * 4. 쿠폰수 감소 (ACADEMY_COUPON_MST의 REMAIN_COUPON_COUNT 감소)
	 * 5. 쿠폰수 사용내역 저장7
	 * 6. 다음 학원 처리
	 * 7. 종료
	 */
	private void ApplyCoin()
	{
		ArrayList<AcademyCoupon> alAcadmyCoupon = this.GetTargetAcademyRawIdForSM();
		
		try {
			int stopStudentCount = 0;
			
			for (AcademyCoupon academyCoupon : alAcadmyCoupon) {
				logger.info(this._couponType + "Coupon ACADEMY_RAWID:" + academyCoupon.getRawId() + " - start");
				long academyRawId       = academyCoupon.getRawId();
				
				ApplyCouponData couponData = new ApplyCouponData();
				couponData.setCouponType(this._couponType);
				couponData.setFreeYN(academyCoupon.getFreeYN());
				couponData.setRenewType(academyCoupon.getRenewType());
				
				
				//0.MonthlyTotalCompany를 clear할지 체크. 5일 또는 쿠폰사용시작일일 경우 해당월 누적출판사 삭제 
				this.CheckClearMonthlyTotalCompany(academyRawId, academyCoupon.getRenewType());
				
				
				//1. 소속학급이 없는 학생은 사용정지한다.
				this.GetCode(CodeType.SNC, couponData);
				ArrayList<UserInfo> alUserInfoNotClass = this.GetUserForNoClass(academyRawId);
				this.SetStopByNotClassUser(alUserInfoNotClass, couponData);
				
				logger.info(this._couponType + "Coupon SetStopByNotClassUser:");
				
				//필수기간인 학생 차감
				//1.유료학원 차감
				//2.무료학원에서 유료학원인 학원 차감
				if(academyCoupon.getRenewType().equals("R"))
				{
					//2.필수사용기간이 아닌 학생  차감
					this.GetCode(CodeType.NORMAL, couponData);
					ArrayList<UserInfo> alNormal = this.GetUserCouponData(academyRawId, couponData, "NOTREQUIRE");
					this.SetCouponByBaseUser(alNormal, couponData);
					logger.info(this._couponType + "Coupon SetCouponByBaseUser NORMAL:");
					
					//4.필수사용기간인 학생중 출판사가 3개이상인 학생 출판사수*3 차감
					this.GetCode(CodeType.REQUIREDOVER, couponData);
					ArrayList<UserInfo> alRequireOver = this.GetUserCouponData(academyRawId, couponData, "REQUIRE");
					this.SetCouponByRequiredUser(alRequireOver, couponData);
					logger.info(this._couponType + "Coupon SetCouponByBaseUser REQUIRE:");

				}
				else
				{
					this.GetCode(CodeType.STARTREQUIRE, couponData);
					
					//필수기간인 학생, 나머지기간 삭제후, 7개 다시 insert(차감동시)
					ArrayList<UserInfo> alRequireNormal = this.GetUserCouponData(academyRawId, couponData, "STARTREQUIRE");
					this.SetCouponByBaseUser(alRequireNormal, couponData);
					logger.info(this._couponType + "Coupon SetCouponByBaseUser STARTREQUIRE:");
					
					//예약인 학생차감, 예약기간 삭제후, 7개 다시 insert(차감동시)
					ArrayList<UserInfo> alReservation = this.GetUserCouponData(academyRawId, couponData, "RESERVATION");
					this.SetCouponByBaseUser(alReservation, couponData);
					logger.info(this._couponType + "Coupon SetCouponByBaseUser RESERVATION:");
				}

				academyCoupon.setStopNoClassCount(couponData.getStopNoClassCount());
				academyCoupon.setStopNoCouponCount(couponData.getStopNoCouponCount());
				
			}
			
		} catch (Exception e) {
			logger.warn(e);
			//if (das != null) try { das.rollback(); } catch(Exception ex) {}
		} finally{
			//if (das != null) try { das.close(); } catch(Exception ex) {}
		}
	}
	
	
	
	/**
	 * 속도문제로 지정학원만 먼처 처리함
	 * 임시
	 */
	private void ApplyCoinTmp()
	{
		ArrayList<AcademyCoupon> alAcadmyCoupon = this.GetTargetAcademyRawIdForSMTmp();
		
		try {
			int stopStudentCount = 0;
			
			for (AcademyCoupon academyCoupon : alAcadmyCoupon) {
				logger.info(this._couponType + "Coupon ACADEMY_RAWID:" + academyCoupon.getRawId() + " - start");
				long academyRawId       = academyCoupon.getRawId();
				
				ApplyCouponData couponData = new ApplyCouponData();
				couponData.setCouponType(this._couponType);
				couponData.setFreeYN(academyCoupon.getFreeYN());
				couponData.setRenewType(academyCoupon.getRenewType());
				
				
				//0.MonthlyTotalCompany를 clear할지 체크. 5일 또는 쿠폰사용시작일일 경우 해당월 누적출판사 삭제 
				this.CheckClearMonthlyTotalCompany(academyRawId, academyCoupon.getRenewType());
				
				
				//1. 소속학급이 없는 학생은 사용정지한다.
				this.GetCode(CodeType.SNC, couponData);
				ArrayList<UserInfo> alUserInfoNotClass = this.GetUserForNoClass(academyRawId);
				this.SetStopByNotClassUser(alUserInfoNotClass, couponData);
				
				logger.info(this._couponType + "Coupon SetStopByNotClassUser:");
				
				//필수기간인 학생 차감
				//1.유료학원 차감
				//2.무료학원에서 유료학원인 학원 차감
				if(academyCoupon.getRenewType().equals("R"))
				{
					//2.필수사용기간이 아닌 학생  차감
					this.GetCode(CodeType.NORMAL, couponData);
					ArrayList<UserInfo> alNormal = this.GetUserCouponData(academyRawId, couponData, "NOTREQUIRE");
					this.SetCouponByBaseUser(alNormal, couponData);
					logger.info(this._couponType + "Coupon SetCouponByBaseUser NORMAL:");
					
					//4.필수사용기간인 학생중 출판사가 3개이상인 학생 출판사수*3 차감
					this.GetCode(CodeType.REQUIREDOVER, couponData);
					ArrayList<UserInfo> alRequireOver = this.GetUserCouponData(academyRawId, couponData, "REQUIRE");
					this.SetCouponByRequiredUser(alRequireOver, couponData);
					logger.info(this._couponType + "Coupon SetCouponByBaseUser REQUIRE:");

				}
				else
				{
					this.GetCode(CodeType.STARTREQUIRE, couponData);
					
					//필수기간인 학생, 나머지기간 삭제후, 7개 다시 insert(차감동시)
					ArrayList<UserInfo> alRequireNormal = this.GetUserCouponData(academyRawId, couponData, "STARTREQUIRE");
					this.SetCouponByBaseUser(alRequireNormal, couponData);
					logger.info(this._couponType + "Coupon SetCouponByBaseUser STARTREQUIRE:");
					
					//예약인 학생차감, 예약기간 삭제후, 7개 다시 insert(차감동시)
					ArrayList<UserInfo> alReservation = this.GetUserCouponData(academyRawId, couponData, "RESERVATION");
					this.SetCouponByBaseUser(alReservation, couponData);
					logger.info(this._couponType + "Coupon SetCouponByBaseUser RESERVATION:");
				}

				academyCoupon.setStopNoClassCount(couponData.getStopNoClassCount());
				academyCoupon.setStopNoCouponCount(couponData.getStopNoCouponCount());
				
			}
			
		} catch (Exception e) {
			logger.warn(e);
			//if (das != null) try { das.rollback(); } catch(Exception ex) {}
		} finally{
			//if (das != null) try { das.close(); } catch(Exception ex) {}
		}
	}
	
	
	private void GetCode(CodeType codeType, ApplyCouponData applyCouponData)
	{
		switch(codeType)
		{
			case SNC :
				applyCouponData.setCouponWorkTypeCD("SNC");//No Class Stop
				applyCouponData.setCheckOverCompany("N");
				applyCouponData.setStopApplyYN("C");
				applyCouponData.setUseTypeCD("SNC");
				break;
			case NORMAL :
				applyCouponData.setCouponWorkTypeCD("B");
				applyCouponData.setCheckOverCompany("N");
				applyCouponData.setStopApplyYN("M");
				applyCouponData.setUseTypeCD("D");
				break;
			case NORMALOVER : // 필수기간이 아닌 추가 출판사
				applyCouponData.setCouponWorkTypeCD("B");
				applyCouponData.setCheckOverCompany("Y");
				applyCouponData.setStopApplyYN("M");
				applyCouponData.setUseTypeCD("D");
				break;
			case REQUIREDOVER : //필수기간중 추가 출판사
				applyCouponData.setCouponWorkTypeCD("RAC");//Required Add Company
				applyCouponData.setCheckOverCompany("Y");
				applyCouponData.setStopApplyYN("M");
				applyCouponData.setUseTypeCD("RAC");
				break;
			case STARTREQUIRE :
				applyCouponData.setCouponWorkTypeCD("B");//Required Add Company
				applyCouponData.setCheckOverCompany("");
				applyCouponData.setStopApplyYN("M");
				applyCouponData.setUseTypeCD("D");
				break;
		}
	}
	
	
	
	
	
	private void CheckCouponHstRawId(DAS das, CouponHstEntity cheHst, long academyRawId, String couponType)
	{
		try
		{
			long hstRawId     = cheHst.getRawId();
	
			if (hstRawId < 0){
				hstRawId = this.getSequenceOfCouponHst(das);
				cheHst.setRawId(hstRawId);
				cheHst.setAcademyRawId(academyRawId);
				cheHst.setRemainCouponCnt(this.getRemainCouponCountNew(das, academyRawId, couponType));
				cheHst.setApplyDate(this.getCurrentDate(das));
				cheHst.setCouponType(couponType);
				this.insertCouponHstNew(das, cheHst);
			}
		} catch (Exception e) {
			logger.warn(e);
		} finally{
		}
	}
	
	
	/**
	 * 소속학급이 없는 학생은 사용정지한다.
	 * 1. Coupon_Hst체크
	 * 2. USER_COUPON_MST update
	 * 3. COUPON_HST update
	 * 4. USER_COUPON_HST insert
	 * 5. COUPON_USE_TRX insert
	 */
	private void SetStopByNotClassUser(ArrayList<UserInfo> alUserInfo, ApplyCouponData couponData )
	{
		DAS das = null;
		try 
		{
			//class없는 학생수 set
			couponData.setStopNoClassCount(alUserInfo.size());
			
			String couponType = couponData.getCouponType();
			String couponWorkTypeCD = couponData.getCouponWorkTypeCD();
			String useTypeCD = couponData.getUseTypeCD();
			String freeYN = couponData.getFreeYN();
			
			das = new DAS(DAS_CONFIG);
			CouponHstEntity cheHst = this.MakeEmptyCouponHstEntity(useTypeCD);
			
			for (UserInfo userInfo : alUserInfo) {
				
				das.beginTrans();
				userInfo.setCouponWorkTypeCD(couponWorkTypeCD);
				
				
				long academyRawId = userInfo.getAcademyRawId();
				long userRawId    = userInfo.getRawId();

				//1. Coupon_Hst체크
				this.CheckCouponHstRawId(das, cheHst, academyRawId, couponType);
				long hstRawId     = cheHst.getRawId();
				int addUpCnt = 0;

			
				//소속학급이 없어 사용정지함
				//2. USER_COUPON_MST update
				//3. COUPON_HST update
				//4. USER_COUPON_HST insert
				//5. COUPON_USE_TRX insert
				addUpCnt += this.updateUserCouponMst(das, cheHst.getApplyDate(), couponData, userInfo, couponData.getStopApplyYN());
				addUpCnt += this.updateCouponHstForNonUserNew(das, hstRawId, couponType, freeYN);
				addUpCnt += this.insertUserCouponHstForSM(das, cheHst.getApplyDate(), couponData, userInfo, couponData.getStopApplyYN());
				
				if (addUpCnt == 3)
					das.commit();
				else {
					das.rollback();
					logger.error(String.format("USER 처리 실패! (academy_rawid:%d, user_rawid:%d)", academyRawId, userRawId)); 
				}
		
			}
		} catch (Exception e) {
			logger.warn(e);
			if (das != null) try { das.rollback(); } catch(Exception ex) {}
		} finally{
			if (das != null) try { das.close(); } catch(Exception ex) {}
		}
	}
	
	
	
	/**
	 * 필수사용기간이 아닌 학생 차감
	 * 1. USER_COUPON_TRX에 해당 학생이 있으면 제외
	 * 2. Coupon_Hst체크
	 * 3. 남은쿠폰, 필요쿠폰, 차감된 쿠폰 개수 가져오기
	 * 4. 차감
	 * 4-1. 쿠폰이 부족하면 학생을 사용정지시킴
	 * 4-1-1. USER_COUPON_MST update
	 * 4-1-2. COUPON_HST update
	 * 4-1-3. USER_COUPON_HST insert
	 * 4-2. 쿠폰이 부족하지 않으면 차감
	 * 4-2-1. 누적출판사, 적용후 누적출판사 체크후 출판사 변경사항이 있으면 COUPON_MONTHLY_TOTAL_TRX insert 
	 * 4-2-2. 이전사용출판사, 현재사용출판사 가져오기
	 * 4-2-3. USER_COUPON_TRX insert
	 * 4-2-4. ACADEMY_COUPON_MST update
	 * 4-2-5. COUPON_HST update
	 * 4-2-6. COUPON_USE_TRX insert
	 * 5. 종료 
	 */
	private void SetCouponByBaseUser(ArrayList<UserInfo> alUserInfo, ApplyCouponData couponData)
	{
		DAS das = null;
		try 
		{
			String couponType = couponData.getCouponType();
			String couponWorkTypeCD = couponData.getCouponWorkTypeCD();
			String useTypeCD = couponData.getUseTypeCD();
			String freeYN = couponData.getFreeYN();
			String renewType = couponData.getRenewType();
			
			das = new DAS(DAS_CONFIG);
			CouponHstEntity cheHst = this.MakeEmptyCouponHstEntity(useTypeCD);
			
			for (UserInfo userInfo : alUserInfo) {

				das.beginTrans();
				userInfo.setCouponWorkTypeCD(couponWorkTypeCD);
				long academyRawId = userInfo.getAcademyRawId();
				long userRawId    = userInfo.getRawId();

				//2. Coupon_Hst체크
				this.CheckCouponHstRawId(das, cheHst, academyRawId, couponType);
				long hstRawId     = cheHst.getRawId();
				
				
				//3. 남은쿠폰, 필요쿠폰, 차감된 쿠폰 개수 가져오기
				long currRemainCount = this.getRemainCouponCountNew(das, academyRawId, couponType);
				long requiredCount = userInfo.getTotalCouponCount();
				long remainCount   = currRemainCount - requiredCount;
				userInfo.setPrevRemainCouponCount(currRemainCount);

				
				//4. 차감
				//4-1. 쿠폰이 부족하면 학생을 사용정지시킴
				//4-2. 쿠폰이 부족하지 않으면 차감
				int addUpCnt = 0;
				if (remainCount < 0){
				
					//4-1-1. USER_COUPON_MST update
					//4-1-2. COUPON_HST update
					//4-1-3. USER_COUPON_HST insert
					addUpCnt += this.updateUserCouponMst(das, cheHst.getApplyDate(), couponData, userInfo, couponData.getStopApplyYN());
					addUpCnt += this.updateCouponHstForNonUserNew(das, hstRawId, couponType, freeYN);
					addUpCnt += this.insertUserCouponHstForSM(das, cheHst.getApplyDate(), couponData, userInfo, couponData.getStopApplyYN());
					
					if(couponData.getRenewType().equals("S"))
					{
						addUpCnt += this.deleteUserCouponTrxNew(das, cheHst.getApplyDate(), couponData, userInfo);
					}
					
					if ((couponData.getRenewType().equals("R")&& addUpCnt == 3) || (couponData.getRenewType().equals("S")&& addUpCnt == 4))
					{
						couponData.setStopNoCouponCount(1);
						das.commit();
					}
					else {
						das.rollback();
						logger.error(String.format("USER 처리 실패! (academy_rawid:%d, user_rawid:%d)", academyRawId, userRawId)); 
					}
				}else {
						
					//4-2-1. 누적출판사, 적용후 누적출판사 체크후 출판사 변경사항이 있으면 COUPON_MONTHLY_TOTAL_TRX insert 
					this.GetMonthlyTotalCompany(userInfo, das, cheHst.getApplyDate());
			
					//4-2-2. 이전사용출판사, 현재사용출판사 가져오기
					this.getCurrentCompanyByUserRawId(das, userInfo);
					

					if(couponData.getRenewType().equals("R"))
					{
						//4-2-3. USER_COUPON_TRX insert
						//4-2-4. ACADEMY_COUPON_MST update
						//4-2-5. COUPON_HST update
						//4-2-6. COUPON_USE_TRX insert
						addUpCnt += this.insertUserCouponTrxNew(das, cheHst.getApplyDate(), couponData, userInfo);
						
						if(freeYN.equals("N"))
						{
							addUpCnt += this.updateAcademyCouponMstNew(das, academyRawId, couponType, requiredCount+"");
						}
						addUpCnt += this.updateCouponHstForUseCouponNew(das, hstRawId, academyRawId, couponType, requiredCount+"", -1, freeYN);
						addUpCnt += this.insertCouponUseTrx(das, cheHst.getApplyDate(), userInfo);
						
						if (freeYN.equals("N") && addUpCnt == 4)
							das.commit();
						else if(freeYN.equals("Y") && addUpCnt == 3)
							das.commit();
						else {
							das.rollback();
							logger.error(String.format("USER 처리 실패! (academy_rawid:%d, user_rawid:%d)", academyRawId, userRawId)); 
						}
					}
					else
					{
						//4-2-3. USER_COUPON_TRX insert
						//4-2-4. ACADEMY_COUPON_MST update
						//4-2-5. COUPON_HST update
						//4-2-6. COUPON_USE_TRX insert
						
						addUpCnt += this.updateUserCouponMst(das, cheHst.getApplyDate(), couponData, userInfo, "Y");
						addUpCnt += this.insertUserCouponHstForSM(das, cheHst.getApplyDate(), couponData, userInfo, "Y");
						addUpCnt += this.deleteUserCouponTrxNew(das, cheHst.getApplyDate(), couponData, userInfo);
						addUpCnt += this.insertUserCouponTrxNew(das, cheHst.getApplyDate(), couponData, userInfo);
						
						if(freeYN.equals("N"))
						{
							addUpCnt += this.updateAcademyCouponMstNew(das, academyRawId, couponType, requiredCount+"");
						}
						addUpCnt += this.updateCouponHstForUseCouponNew(das, hstRawId, academyRawId, couponType, requiredCount+"", -1, freeYN);
						addUpCnt += this.insertCouponUseTrx(das, cheHst.getApplyDate(), userInfo);
						
						
						if (freeYN.equals("N") && addUpCnt == 7)
							das.commit();
						else if(freeYN.equals("Y") && addUpCnt == 6)
							das.commit();
						else {
							das.rollback();
							logger.error(String.format("USER 처리 실패! (academy_rawid:%d, user_rawid:%d)", academyRawId, userRawId)); 
						}
					}
				}
			}
		} catch (Exception e) {
			logger.warn(e);
			if (das != null) try { das.rollback(); } catch(Exception ex) {}
		} finally{
			if (das != null) try { das.close(); } catch(Exception ex) {}
		}
	}
	
	
	
	
	
	
	/**
	 * 필수사용기간인 학생중 출판사가 3개이상인 학생 차감
	 * 1. Coupon_Hst체크
	 * 2. 남은쿠폰, 필요쿠폰, 차감된 쿠폰 개수 가져오기
	 * 3. 누적출판사, 적용후 누적출판사 체크후 출판사 변경사항이 있으면 COUPON_MONTHLY_TOTAL_TRX insert 
	 * 4. 이전사용출판사, 현재사용출판사 가져오기
	 * 5. 쿠폰이 부족하면 ACADEMY_COUPON_TRX 마이너스쿠폰을 저장한다.
	 * 5-1. academyCouponTrxRawId < 0 이면 insert
	 * 5-2. academyCouponTrxRawId >=0 이면 update
	 * 6. ACADEMY_COUPON_MST update
	 * 7. COUPON_HST update
	 * 8. COUPON_USE_TRX insert
	 * 9. 종료 
	 */
	private void SetCouponByRequiredUser(ArrayList<UserInfo> alUserInfo, ApplyCouponData applyCouponData)
	{
		DAS das = null;
		try 
		{
			String couponType = applyCouponData.getCouponType();
			String couponWorkTypeCD = applyCouponData.getCouponWorkTypeCD();
			String useTypeCD = applyCouponData.getUseTypeCD();
			String freeYN = applyCouponData.getFreeYN();
			
			das = new DAS(DAS_CONFIG);
			long academyCouponTrxRawId = -1;
			CouponHstEntity cheHst = this.MakeEmptyCouponHstEntity(useTypeCD);
			
			for (UserInfo userInfo : alUserInfo) {
				
				if(userInfo.getDifferYN().equals("N"))
					continue;
				
				das.beginTrans();
				userInfo.setCouponWorkTypeCD(couponWorkTypeCD);
				long academyRawId = userInfo.getAcademyRawId();
				long userRawId    = userInfo.getRawId();

				//1. Coupon_Hst체크
				this.CheckCouponHstRawId(das, cheHst, academyRawId, couponType);
				long hstRawId     = cheHst.getRawId();
				
				
				//2. 누적출판사, 적용후 누적출판사 체크후 출판사 변경사항이 있으면 COUPON_MONTHLY_TOTAL_TRX insert 
				this.GetMonthlyTotalCompany(userInfo, das, cheHst.getApplyDate());
				
				if( userInfo.getOverCompanyCount()> 0)
				{
					//3. 남은쿠폰, 필요쿠폰, 차감된 쿠폰 개수 가져오기
					long currRemainCount = this.getRemainCouponCountNew(das, academyRawId, couponType);
					long requiredCount = userInfo.getTotalCouponCount();
					long remainCount   = currRemainCount - requiredCount;
					long minusCouponCount    = 0;
					userInfo.setPrevRemainCouponCount(currRemainCount);
					
					//4. 이전사용출판사, 현재사용출판사 가져오기
					this.getCurrentCompanyByUserRawId(das, userInfo);
					
	
					//5. 쿠폰이 부족하면 ACADEMY_COUPON_TRX 마이너스쿠폰을 저장한다.
					int addUpCnt = 0;
					if (remainCount < 0)
					{
						userInfo.setMinusCouponCount(remainCount);
						userInfo.setMinusYN("Y");
						minusCouponCount += remainCount;
						
						//5-1. academyCouponTrxRawId < 0 이면 insert
						//5-2. academyCouponTrxRawId >= 0 이면 update
						if(academyCouponTrxRawId < 0)
						{
							academyCouponTrxRawId = this.getSequenceOfAcademyCouponTrx(das);
							addUpCnt += this.insertAcademyCouponTrx(das, cheHst.getApplyDate(), academyCouponTrxRawId, academyRawId, minusCouponCount, "M", "1");
						}	
						else
						{
							addUpCnt += this.updateAcademyCouponTrx(das, academyCouponTrxRawId, academyRawId, minusCouponCount);
						}
					}
					//6. ACADEMY_COUPON_MST update
					//7. COUPON_HST update
					//8. COUPON_USE_TRX insert
					if(freeYN.equals("N"))
					{
						addUpCnt += this.updateAcademyCouponMstNew(das, academyRawId, couponType, requiredCount+"");
					}
					addUpCnt += this.updateCouponHstForUseCouponNew(das, hstRawId, academyRawId, couponType, requiredCount+"", -1, freeYN);
					addUpCnt += this.insertCouponUseTrx(das, cheHst.getApplyDate(), userInfo);
					
					
					if (freeYN.equals("N") && remainCount < 0 && addUpCnt == 4)
						das.commit();
					if (freeYN.equals("N") && remainCount >= 0 && addUpCnt == 3)
						das.commit();
					else if(freeYN.equals("Y") && remainCount < 0 && addUpCnt == 3)
						das.commit();
					else if(freeYN.equals("Y") && remainCount >= 0 && addUpCnt == 2)
						das.commit();
					else {
						das.rollback();
						logger.error(String.format("USER 처리 실패! (academy_rawid:%d, user_rawid:%d)", academyRawId, userRawId)); 
					}
				}
				else
				{
					das.commit();
				}
			
			}
		} catch (Exception e) {
			logger.warn(e);
			if (das != null) try { das.rollback(); } catch(Exception ex) {}
		} finally{
			if (das != null) try { das.close(); } catch(Exception ex) {}
		}
	}
	
	
	
	private void CheckClearMonthlyTotalCompany(long academyRawId, String renewType)
	{
		DAS das = null;
		try
		{
			das = new DAS(DAS_CONFIG);
			
			if(this.currentDay == 5)
			{
				if(getMonthlyTotalCompanyForToDay(das, academyRawId) == false)
				{
					//COUPON_MONTHLY_TOTAL_TRX CLEAR
					this.ClearMonthlyTotalCompany(das, academyRawId);
					das.commit();
				}
			}
			else if(renewType.equals("S"))
			{
				//COUPON_MONTHLY_TOTAL_TRX CLEAR
				this.ClearMonthlyTotalCompany(das, academyRawId);
				das.commit();
			}
		} catch (Exception e) {
			logger.warn(e);
			if (das != null) try { das.rollback(); } catch(Exception ex) {}
		} finally{
			if (das != null) try { das.close(); } catch(Exception ex) {}
		}
	}
	
	
	
	
	
	/**
	 * 학원의 남아있는 쿠폰을 가져온다.
	 * @param 
	 * @return
	 */
	private ArrayList<AcademyCoupon> GetTargetAcademyRawIdForSM(){
		DAS das = null;
		RecordSet rset = null;
		ArrayList<AcademyCoupon> alAcadmyCoupon = new ArrayList<AcademyCoupon>();
		
		try {
			das = new DAS(DAS_CONFIG);
			String sql =" SELECT am.rawid academy_rawid "+
						"		 ,CASE WHEN am.sm_coupon_start_dt <= SYSDATE THEN 'N' ELSE 'Y' END free_yn "+
						"   	 ,DECODE(TO_CHAR(sm_coupon_start_dt,'yyyymmdd'), TO_CHAR(SYSDATE,'yyyymmdd'),'S','R') renewtype"+
						" FROM academy_mst am "+
						" WHERE am.franchise_cd = 'N' AND am.organization_type_cd = ? "+
						"   AND am.is_used = 'A' AND am.sm_coupon_yn = 'Y'  and am.sm_coupon_start_dt is not null and am.rawid <> 3497 ";
					

			LinkedHashMap keyList = new LinkedHashMap();
			keyList.put("organizationTypeCD"     , this._organizationTypeCD);
			rset = das.select(sql, keyList);
			
			while(rset.next()){
				AcademyCoupon ac = new AcademyCoupon();
				ac.setRawId(rset.getLong("academy_rawid"));
				ac.setFreeYN(rset.getString("free_yn"));
				ac.setRenewType(rset.getString("renewtype"));
				alAcadmyCoupon.add(ac);
			}
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
			if (das != null) try { das.close(); } catch(Exception ex) {}
		}
		return alAcadmyCoupon;
	}
	
	
	/**
	 * 속도문제로 지정학원만 먼처 처리함
	 * 임시
	 * @param 
	 * @return
	 */
	private ArrayList<AcademyCoupon> GetTargetAcademyRawIdForSMTmp(){
		DAS das = null;
		RecordSet rset = null;
		ArrayList<AcademyCoupon> alAcadmyCoupon = new ArrayList<AcademyCoupon>();
		
		try {
			das = new DAS(DAS_CONFIG);
			String sql =" SELECT am.rawid academy_rawid "+
						"		 ,CASE WHEN am.sm_coupon_start_dt <= SYSDATE THEN 'N' ELSE 'Y' END free_yn "+
						"   	 ,DECODE(TO_CHAR(sm_coupon_start_dt,'yyyymmdd'), TO_CHAR(SYSDATE,'yyyymmdd'),'S','R') renewtype"+
						" FROM academy_mst am "+
						" WHERE am.franchise_cd = 'N' AND am.organization_type_cd = ? "+
			"   AND am.is_used = 'A' AND am.sm_coupon_yn = 'Y'  and am.sm_coupon_start_dt is not null  and am.rawid = 3497 ";  //문제있는 학원 rawid로 변경할것.
			/*"   AND am.is_used = 'A' AND am.sm_coupon_yn = 'Y'  and am.sm_coupon_start_dt is not null  "+
			" AND AM.RAWID NOT IN (select ACADEMY_RAWID from coupon_hst "+
			"		 WHERE TO_CHAR(APPLY_DT,'YYYY-MM-DD') = '2015-01-13' AND CREATE_TYPE_CD = 'S' AND COUPON_TYPE = 'NC') ";*/
					

			LinkedHashMap keyList = new LinkedHashMap();
			keyList.put("organizationTypeCD"     , this._organizationTypeCD);
			rset = das.select(sql, keyList);
			
			while(rset.next()){
				AcademyCoupon ac = new AcademyCoupon();
				ac.setRawId(rset.getLong("academy_rawid"));
				ac.setFreeYN(rset.getString("free_yn"));
				ac.setRenewType(rset.getString("renewtype"));
				alAcadmyCoupon.add(ac);
			}
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
			if (das != null) try { das.close(); } catch(Exception ex) {}
		}
		return alAcadmyCoupon;
	}
	
	/**
	 * 소속학급이 없는 학생 가져오기
	 * @param academyRawId
	 * @return
	 */
	private ArrayList<UserInfo> GetUserForNoClass(long academyRawId){
		RecordSet rset = null;
		DAS das = null;
		ArrayList<UserInfo> alUserInfo = new ArrayList<UserInfo>();
		
		try {
			das = new DAS(DAS_CONFIG);
			String sql = "  SELECT ucm.user_rawid, um.academy_rawid										                                                                      "+
						"     FROM user_coupon_mst ucm                                                                                                                          "+
						"    INNER JOIN user_mst um ON um.rawid = ucm.user_rawid  AND um.user_status_cd = '01'                                                                "+
						"     LEFT OUTER JOIN class_user_link_mst clm ON clm.user_rawid = um.rawid "+
						"				  AND clm.class_kind_cd = 'NC'  "+ 
	                    //"          AND clm.class_rawid IN(SELECT rawid FROM class_mst WHERE academy_rawid = ? AND class_kind_cd = 'NC'      ) "+
						"     LEFT OUTER JOIN user_coupon_trx uct ON uct.user_rawid = um.rawid                                                                                  "+
						"		   AND uct.apply_day = ? AND uct.coupon_type=? AND uct.academy_rawid = ?                                                                                 "+
						"    WHERE ucm.cp_apply_yn = 'Y'                                                                                                                     "+
						"      AND TO_CHAR(ucm.cp_apply_dt, 'yyyymmdd') <= ?                                                                                                    "+
						" 	   AND ucm.cp_apply_dt < to_date(?)-6       "+
						"      AND ucm.academy_rawid = ?                                                                                                                        "+
						"      AND ucm.coupon_type   = ?                                                                                                                     "+
						"      AND DECODE(uct.rawid,NULL,'N','Y') = 'N'										                                                                                      "+
					    "      AND clm.class_rawid is null ";
			
			LinkedHashMap keyList = new LinkedHashMap();
			keyList.put("APPLY_DT1"     , nextRunDay);
			keyList.put("coupon_type", this._couponType);
			keyList.put("ACADEMY_RAWID2", academyRawId + "");
			keyList.put("APPLY_DT2"     , nextRunDay);
			keyList.put("APPLY_DT3"     , nextRunDay);
			keyList.put("ACADEMY_RAWID3", academyRawId + "");
			keyList.put("coupon_type2",  this._couponType);
			rset = das.select(sql, keyList);
			
			while(rset.next()){
				UserInfo ui = new UserInfo();
				ui.setAcademyRawId(rset.getLong("academy_rawid"));
				ui.setRawId(rset.getLong("user_rawid"));

				alUserInfo.add(ui);
			}
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
			if (das != null) try { das.close(); } catch(Exception ex) {}
		}
		return alUserInfo;
	}
	
	
	
	//예약인 학생
	private String GetReservationUser(LinkedHashMap keyList, long academyRawId)
	{
		String sql ="   SELECT ucm.academy_rawid, ucm.user_rawid, ucm.cp_apply_yn , TO_CHAR(ucm.cp_apply_dt, 'yyyymmdd') apply_start_day , 'Y' reservationyn, um.tutor_yn    "+
		"     FROM user_coupon_mst ucm                                                                                                                          "+
		"    INNER JOIN USER_MST um ON um.rawid = ucm.user_rawid AND um.academy_rawid = ? AND um.user_status_cd ='01'                                           "+
		"    INNER JOIN class_user_link_mst clm ON clm.user_rawid = um.rawid AND clm.class_kind_cd = 'NC' "+
		"     LEFT OUTER JOIN user_coupon_trx uct ON uct.user_rawid = um.rawid AND uct.apply_day > ? AND uct.coupon_type=? AND uct.academy_rawid = ?     "+
		"    WHERE ucm.cp_apply_yn = 'Y'                                                                                                                     "+
		"      AND TO_CHAR(ucm.cp_apply_dt, 'yyyymmdd') > ?                                                                                                   "+
		" 	   AND ucm.cp_apply_dt  > to_date(?)-6       "+
		"      AND ucm.academy_rawid = ?                                                                                                                        "+
		"      AND ucm.coupon_type   = ?                                                                                                                     "+
		"      AND DECODE(uct.rawid,NULL,'N','Y') = 'Y'										                                                                                      "+
		"    GROUP BY  ucm.academy_rawid, ucm.user_rawid, ucm.cp_apply_yn , ucm.cp_apply_dt, um.tutor_yn ";
		
		keyList.put("ACADEMY_RAWID_V1" , academyRawId + "");
		keyList.put("APPLY_DT_V1"     , nextRunDay);
		keyList.put("COUPONTYPE_V1" , this._couponType);
		keyList.put("ACADEMY_RAWID_V2" , academyRawId + "");
		keyList.put("APPLY_DT_V2"      , nextRunDay);
		keyList.put("APPLY_DT_V3"      , nextRunDay);
		keyList.put("ACADEMY_RAWID_V3", academyRawId + "");
		keyList.put("COUPONTYPE_V2" , this._couponType);
		
		
		return sql;
	}
	
	//필수기간인 학생
	private String GetRequireUser(LinkedHashMap keyList, long academyRawId)
	{
		String sql ="   SELECT ucm.academy_rawid, ucm.user_rawid, ucm.cp_apply_yn, TO_CHAR(sysdate, 'yyyymmdd') apply_start_day	, 'N' reservationyn, um.tutor_yn									                                                                      "+
		"     FROM user_coupon_mst ucm                                                                                                                          "+
		"    INNER JOIN USER_MST um ON um.rawid = ucm.user_rawid AND um.academy_rawid = ? AND um.user_status_cd ='01'                         "+
		"    INNER JOIN class_user_link_mst clm ON clm.user_rawid = um.rawid AND clm.class_kind_cd = 'NC' "+
		"    INNER JOIN user_coupon_trx uct ON uct.user_rawid = um.rawid AND uct.apply_day = ? AND uct.coupon_type=? AND uct.academy_rawid = ?                                        "+
		"    LEFT OUTER JOIN coupon_use_trx cut ON cut.user_rawid = uct.user_rawid  AND TO_CHAR(cut.create_dt, 'yyyymmdd')= ? and coupon_work_type_cd='RAC'  AND cut.academy_rawid =?   "+
		"    WHERE ucm.cp_apply_yn = 'Y'                                                                                                                     "+
		"      AND TO_CHAR(ucm.cp_apply_dt, 'yyyymmdd') <= ?                                                                                                    "+
		" 	   AND ucm.cp_apply_dt  >= to_date(?)-6       "+
		"      AND ucm.academy_rawid = ?                                                                                                                        "+
		"      AND ucm.coupon_type   = ?                                                                                                                     "+
		"      AND DECODE(cut.rawid,NULL,'N','Y') = 'N'										                                                                                      "+
		"    GROUP BY  ucm.academy_rawid, ucm.user_rawid, ucm.cp_apply_yn , um.tutor_yn ";
		
		
		keyList.put("ACADEMY_RAWID_R1" , academyRawId + "");
		keyList.put("APPLY_DT_R1"     , nextRunDay);
		keyList.put("COUPONTYPE_R1" , this._couponType);
		
		keyList.put("ACADEMY_RAWID_R2" , academyRawId + "");
		keyList.put("APPLY_DT_R2"      , nextRunDay);
		keyList.put("ACADEMY_RAWID_R3", academyRawId + "");
		
		keyList.put("APPLY_DT_R3"      , nextRunDay);
		keyList.put("APPLY_DT_R4"      , nextRunDay);
		keyList.put("ACADEMY_RAWID_R4", academyRawId + "");
		keyList.put("COUPONTYPE_R2" , this._couponType);

		return sql;
	}
	
	//필수기간이 아닌 학생
	private String GetNotRequireUser(LinkedHashMap keyList, long academyRawId)
	{
		String sql ="   SELECT ucm.academy_rawid, ucm.user_rawid, ucm.cp_apply_yn , TO_CHAR(sysdate, 'yyyymmdd') apply_start_day , 'N' reservationyn, um.tutor_yn											                                                                      "+
		"     FROM user_coupon_mst ucm                                                                                                                          "+
		"    INNER JOIN USER_MST um ON um.rawid = ucm.user_rawid AND um.academy_rawid = ? AND um.user_status_cd ='01'                                                                                                  "+
		"    INNER JOIN class_user_link_mst clm ON clm.user_rawid = um.rawid AND clm.class_kind_cd = 'NC' "+
		"     LEFT OUTER JOIN user_coupon_trx uct ON uct.user_rawid = um.rawid AND uct.apply_day = ? AND uct.coupon_type=? AND uct.academy_rawid = ?                                        "+
		"    WHERE ucm.cp_apply_yn = 'Y'                                                                                                                     "+
		"      AND TO_CHAR(ucm.cp_apply_dt, 'yyyymmdd') <= ?                                                                                                    "+
		" 	   AND ucm.cp_apply_dt < to_date(?)-6       "+
		"      AND ucm.academy_rawid = ?                                                                                                                        "+
		"      AND ucm.coupon_type   = ?                                                                                                                    "+
		"      AND DECODE(uct.rawid,NULL,'N','Y') = 'N'										                                                                                      "+
		"    GROUP BY  ucm.academy_rawid, ucm.user_rawid, ucm.cp_apply_yn , um.tutor_yn ";
		
		
		keyList.put("ACADEMY_RAWID_N1" , academyRawId + "");
		keyList.put("APPLY_DT_N1"     , nextRunDay);
		keyList.put("COUPONTYPE_N1" , this._couponType);
		
		keyList.put("ACADEMY_RAWID_N2" , academyRawId + "");
		keyList.put("APPLY_DT_N2"      , nextRunDay);
		keyList.put("APPLY_DT_N3"      , nextRunDay);
		
		keyList.put("ACADEMY_RAWID_N3", academyRawId + "");
		keyList.put("COUPONTYPE_N2" , this._couponType);
		
		return sql;
	}
	
	//오늘부터 COUPON사용시작인 학생
	private String GetTodayStartUser(LinkedHashMap keyList, long academyRawId)
	{
		String sql = " select academy_rawid,user_rawid ,cp_apply_yn ,apply_start_day ,reservationyn ,tutor_yn  from ( "+ this.GetNotRequireUser(keyList, academyRawId) + " union " + this.GetRequireUser(keyList, academyRawId) +" ) group by academy_rawid,user_rawid ,cp_apply_yn ,apply_start_day ,reservationyn ,tutor_yn  ";
		
//		String sql ="   SELECT ucm.academy_rawid, ucm.user_rawid, ucm.cp_apply_yn, TO_CHAR(sysdate, 'yyyymmdd') apply_start_day	, 'N' reservationyn, um.tutor_yn									                                                                      "+
//		"     FROM user_coupon_mst ucm                                                                                                                          "+
//		"    INNER JOIN USER_MST um ON um.rawid = ucm.user_rawid AND um.academy_rawid = ? AND um.user_status_cd ='01'                                    "+
//		"    INNER JOIN class_user_link_mst clm ON clm.user_rawid = um.rawid AND clm.class_kind_cd = 'NC' "+
//		"    INNER JOIN user_coupon_trx uct ON uct.user_rawid = um.rawid AND uct.apply_day = ? AND uct.coupon_type=? AND uct.academy_rawid = ?                                        "+
//		"    LEFT OUTER JOIN coupon_use_trx cut ON cut.user_rawid = uct.user_rawid  AND TO_CHAR(cut.create_dt, 'yyyymmdd')= ? and coupon_work_type_cd='RAC'  AND cut.academy_rawid =?   "+
//		"    WHERE ucm.cp_apply_yn = 'Y'                                                                                                                     "+
//		"      AND TO_CHAR(ucm.cp_apply_dt, 'yyyymmdd') <= ?                                                                                                    "+
//		//" 	   AND ucm.cp_apply_dt  >= to_date(?)-6       "+
//		"      AND ucm.academy_rawid = ?                                                                                                                        "+
//		"      AND ucm.coupon_type   = ?                                                                                                                     "+
//		"      AND DECODE(cut.rawid,NULL,'N','Y') = 'N'										                                                                                      "+
//		"    GROUP BY  ucm.academy_rawid, ucm.user_rawid, ucm.cp_apply_yn , um.tutor_yn ";
		return sql;
	}
	
	
	/**
	 * 필수사용기간이 아닌 학생 가져오기
	 * @param 
	 * @return
	 */
	private ArrayList<UserInfo> GetUserCouponData(long academyRawId, ApplyCouponData couponData, String searchType){
		RecordSet rset = null;
		DAS das = null;
		ArrayList<UserInfo> alUserInfo = new ArrayList<UserInfo>();
		
		try {
			LinkedHashMap keyList = new LinkedHashMap();
			
			das = new DAS(DAS_CONFIG);
			String sql =" SELECT academy_rawid, user_rawid, cp_apply_yn , decode(sign(overcompanycount),-1,0,overcompanycount)overcompanycount "+
				        "       ,differyn, apply_start_day, reservationyn, tutor_yn        "+
						" FROM ( "+
						" SELECT um.academy_rawid, um.user_rawid, um.cp_apply_yn                                                  "+
						"		  , CASE WHEN NVL(mon.count,0) >= 3 THEN NVL(curr.count,0) ELSE NVL(curr.count,0) + NVL(mon.count,0) - 3 END  overcompanycount "+          
						" 	      , CASE WHEN  nvl(curr.count,0) = 0 THEN 'N' ELSE 'Y'END differyn, um.apply_start_day,reservationyn,um.tutor_yn               "+
						" FROM                                                                                                                                                  "+
						" (                                                                                                                                                     ";
					

			if(searchType.equals("RESERVATION"))
			{
				sql += this.GetReservationUser(keyList,academyRawId);
			}
			else if(searchType.equals("REQUIRE"))
			{
				sql += this.GetRequireUser(keyList,academyRawId);
			}
			else if(searchType.equals("STARTREQUIRE"))
			{
				sql += this.GetTodayStartUser(keyList,academyRawId);
			}
			else if(searchType.equals("NOTREQUIRE"))
			{
				sql += this.GetNotRequireUser(keyList,academyRawId);
			}
			
		
			sql +="    )um 													                                                                                                                        "+
						" LEFT OUTER JOIN                                                                                                                                       "+
						" (																																																																									    "+
						"    SELECT user_rawid,count(company_cd)count                                                                                                           "+
						"      FROM coupon_monthly_total_trx                                                                                                                    "+
						"     WHERE check_month = to_char(sysdate,'yyyymm') AND academy_rawid = ?                                                                               "+
						"  GROUP BY user_rawid                                                                                                                                  "+
						" )MON ON um.user_rawid = mon.user_rawid                                                                                                                "+
						" LEFT OUTER JOIN                                                                                                                                       "+
						" (                                                                                                                                                     "+
						"     SELECT user_rawid, count(company_cd)count                                                                                                         "+
						"     FROM                                                                                                                                              "+
						"     (                                                                                                                                                 "+
						"         SELECT um.rawid user_rawid,  sm.company_cd                                                                                                    "+
						"           FROM user_mst um                                                                                                                            "+
						"           INNER JOIN academy_mst am ON am.rawid = um.academy_rawid  AND am.rawid = ? AND am.use_acaman_yn = 'Y'                               "+
						"           INNER JOIN class_user_link_mst clm ON clm.user_rawid = um.rawid                                                                             "+
						"           INNER JOIN class_mst cm ON cm.rawid = clm.class_rawid AND cm.del_yn = 'N'                                                                   "+
						"           INNER JOIN class_book_link_mst cbm ON cbm.class_rawid = cm.rawid  AND to_char(sysdate, 'yyyymmdd') BETWEEN cbm.start_dt AND cbm.end_dt     "+
						"           INNER JOIN book_mst bm ON bm.rawid = cbm.book_rawid  AND bm.del_yn = 'N'                                                                    "+
						"           INNER JOIN series_mst sm ON sm.series_cd = bm.series_cd  AND sm.del_yn = 'N'                                                                "+
						"           INNER JOIN partner_mst pm ON sm.company_cd = pm.company_cd  AND pm.del_yn = 'N' AND pm.select_yn = 'Y'                                      "+
						"           INNER JOIN product_group_mst pg ON  pg.product_group_cd = am.product_group_cd   AND pg.del_yn = 'N'                                         "+
						"           INNER JOIN product_mst pdm ON  pdm.rawid = pg.product_rawid AND pdm.del_yn = 'N'                                                            "+
						"           INNER JOIN product_link_mst pl ON pl.series_rawid = sm.rawid AND pl.product_rawid = pdm.rawid                           "+
						"           INNER JOIN (SELECT * FROM code_mst WHERE category = 'STUDY_CD' AND use_yn = 'Y') csd ON csd.code = sm.study_cd                              "+
						"           INNER JOIN (SELECT * FROM code_mst WHERE category = 'COMPANY_CD' AND use_yn = 'Y') cpd ON cpd.code = sm.company_cd                          ";
			
			if(_organizationTypeCD.equals("SR"))
			{
				sql += " INNER JOIN PARTNER_CHARGE_MST PCM ON PM.RAWID = PCM.PARTNER_RAWID AND PCM.ORGANIZATION_TYPE_CD='SR' AND PCM.CHARGE_YN='Y'  ";
			}

			sql += "                GROUP BY um.rawid, sm.company_cd, pm.partner_type_cd                                                                                        "+
						"           MINUS                                                                                                                                       "+
						"           SELECT user_rawid,company_cd FROM coupon_monthly_total_trx WHERE check_month = to_char(sysdate,'yyyymm') AND academy_rawid = ?              "+
						"   )                                                                                                                                                   "+
						"   GROUP BY user_rawid                                                                                                                                 "+
						" )CURR ON um.user_rawid = curr.user_rawid                                                                                                              "+
						" )  ";
			
			if(searchType.equals("REQUIRE"))
			{
				//sql += " WHERE overcompanycount > 0 ";
			}
			sql += " ORDER BY overcompanycount ";   
			
			
		
//			keyList.put("ACADEMY_RAWID1" , academyRawId + "");
//			keyList.put("APPLY_DT1"     , nextRunDay);
//			keyList.put("COUPONTYPE1" , this._couponType);
//			keyList.put("ACADEMY_RAWID2" , academyRawId + "");
//			
//			if(searchType.equals("REQUIRE") || searchType.equals("STARTREQUIRE"))
//			{
//				keyList.put("APPLY_DT4"     , nextRunDay);
//				keyList.put("ACADEMY_RAWID7" , academyRawId + "");
//			}
//			
//			
//			keyList.put("APPLY_DT2"      , nextRunDay);
//			
//			if(!searchType.equals("STARTREQUIRE"))
//				keyList.put("APPLY_DT3"      , nextRunDay);
//			
//			keyList.put("ACADEMY_RAWID3", academyRawId + "");
//			keyList.put("COUPONTYPE2" , this._couponType);
			keyList.put("ACADEMY_RAWID11", academyRawId + "");
			keyList.put("ACADEMY_RAWID22", academyRawId + "");
			keyList.put("ACADEMY_RAWID33", academyRawId + "");
			rset = das.select(sql, keyList);
			
			int renewCount = 1;
			int overCount = 3;
			
			//일반학원일 경우
			if(couponData.getRenewType().equals("R"))
			{
				if(searchType.equals("NOTREQUIRE"))
				{
					renewCount = 1;
				}
				else if(searchType.equals("REQUIRE"))
				{
					renewCount = 0;
				}
			}
			else
			{
				renewCount = 7;
			}
			
			while(rset.next()){
				UserInfo ui = new UserInfo();
				ui.setAcademyRawId(rset.getLong("academy_rawid"));
				ui.setRawId(rset.getLong("user_rawid"));
				ui.setCpapply(rset.getString("cp_apply_yn"));
				ui.setDifferYN(rset.getString("differyn"));
				ui.setApplyStartDay(rset.getString("apply_start_day"));
				ui.setReservationYN(rset.getString("reservationyn"));
				
				
				
				if(couponData.getFreeYN().equals("N") && rset.getString("tutor_yn").equals("N")){
					if(this.canCheckAddCompany)
						ui.setOverCompanyCount(rset.getLong("overcompanycount"));
					
					ui.setAddCompanyCouponCount(ui.getOverCompanyCount()*overCount);
					ui.setBaseCouponCount(renewCount);
				}

				ui.setTotalCouponCount(ui.getBaseCouponCount() + ui.getAddCompanyCouponCount());
				alUserInfo.add(ui);
			}
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
			if (das != null) try { das.close(); } catch(Exception ex) {}
			
		}
		return alUserInfo;
	}
	
	
	
	private String getMonthlyCompanyByUserRawId(DAS dasx, long rawid, long userRawId)
	{
		String companyList = "";
		RecordSet rset = null;
		ArrayList<UserInfo> alUserInfo = new ArrayList<UserInfo>();
		
		try {
			String sql =" SELECT user_rawid,cm.name companyname "+
						"   FROM coupon_monthly_total_trx mt "+
						"  INNER JOIN code_mst cm ON cm.code = mt.company_cd AND category = 'COMPANY_CD' AND use_yn = 'Y' "+
						"  WHERE check_month = TO_CHAR(SYSDATE,'yyyymm') AND academy_rawid=? AND user_rawid=? ";

			LinkedHashMap keyList = new LinkedHashMap();
			keyList.put("ACADEMY_RAWID", rawid + "");
			keyList.put("USER_RAWID"   , userRawId + "");
			rset = dasx.select(sql, keyList);
			
			while(rset.next())
			{
				companyList += "," + rset.getString("COMPANYNAME");
			}
			if(companyList.length()>0)
				companyList  = companyList.substring(1, companyList.length());
			
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
		}
		return companyList;
	}
	
	private void InsertMonthlyTotalCompany(DAS dasx, Date hstDate, long academyRawId, long userRawId){
		RecordSet rset = null;
		
		try {
			String sql ="  SELECT user_rawid, company_cd, TO_CHAR(SYSDATE,'yyyymm') check_month                               "+
			            "   FROM( 								"+
						"         SELECT um.rawid user_rawid,  sm.company_cd                                                                                                "+
						"           FROM user_mst um                                                                                                                            "+
						"           INNER JOIN academy_mst am ON am.rawid=um.academy_rawid  AND um.academy_rawid=? AND am.use_acaman_yn = 'Y'                                   "+
						"           INNER JOIN class_user_link_mst clm ON clm.user_rawid=um.rawid                                                                               "+
						"           INNER JOIN class_mst cm ON cm.rawid=clm.class_rawid AND cm.del_yn = 'N'                                                                     "+
						"           INNER JOIN class_book_link_mst cbm ON cbm.class_rawid=cm.rawid  AND to_char (sysdate, 'yyyymmdd') BETWEEN cbm.start_dt AND cbm.end_dt       "+
						"           INNER JOIN book_mst bm ON bm.rawid=cbm.book_rawid  AND bm.del_yn = 'N'                                                                      "+
						"           INNER JOIN series_mst sm ON sm.series_cd = bm.series_cd  AND sm.del_yn = 'N'                                                                "+
						"           INNER JOIN partner_mst pm ON sm.company_cd = pm.company_cd  AND pm.del_yn = 'N' AND pm.select_yn = 'Y'                                      "+
						"           INNER JOIN product_group_mst pg ON  pg.product_group_cd = am.product_group_cd   AND pg.del_yn = 'N'                                         "+
						"           INNER JOIN product_mst pdm ON  pdm.rawid = pg.product_rawid AND pdm.del_yn = 'N'                                                            "+
						"           INNER JOIN product_link_mst pl ON pl.series_rawid = sm.rawid AND pl.product_rawid = pdm.rawid                           "+
						"           INNER JOIN (SELECT * FROM code_mst WHERE category = 'STUDY_CD' AND use_yn = 'Y') csd ON csd.code = sm.study_cd                              "+
						"           INNER JOIN (SELECT * FROM code_mst WHERE category = 'COMPANY_CD' AND use_yn = 'Y') cpd ON cpd.code = sm.company_cd                          ";
			
			if(_organizationTypeCD.equals("SR"))
			{
				sql += " INNER JOIN PARTNER_CHARGE_MST PCM ON PM.RAWID = PCM.PARTNER_RAWID AND PCM.ORGANIZATION_TYPE_CD='SR' AND PCM.CHARGE_YN='Y'  ";
			}

			sql +="           WHERE um.rawid = ?                                                                                                                            "+
						"           GROUP BY um.rawid, sm.company_cd, pm.partner_type_cd                                                                                        "+
						"           MINUS                                                                                                                                       "+
						"           SELECT user_rawid,company_cd FROM coupon_monthly_total_trx WHERE check_month= TO_CHAR(SYSDATE,'yyyymm') AND academy_rawid=? AND user_rawid=? "+
						"       )";
						
			LinkedHashMap keyList = new LinkedHashMap();
			keyList.put("ACADEMY_RAWID1", academyRawId + "");
			keyList.put("USER_RAWID1"   , userRawId + "");
			keyList.put("ACADEMY_RAWID2", academyRawId + "");
			keyList.put("USER_RAWID2"   , userRawId + "");
			rset = dasx.select(sql, keyList);
			
			while(rset.next()){
				LinkedHashMap columnMap = new LinkedHashMap();
				columnMap.put("RAWID+SEQ_COUPON_MONTHLY_TOTAL_TRX.nextval", null);
				columnMap.put("ACADEMY_RAWID"    , academyRawId);
				columnMap.put("USER_RAWID"       , userRawId);
				columnMap.put("COMPANY_CD"       , rset.getString("COMPANY_CD"));
				columnMap.put("CHECK_MONTH"	     , rset.getString("CHECK_MONTH"));
				columnMap.put("CREATE_DT"		 , hstDate);
				
				dasx.insert("COUPON_MONTHLY_TOTAL_TRX", columnMap);
			}
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
		}
	}
	
	
	
	private void ClearMonthlyTotalCompany(DAS dasx, long academyRawId){
	
		try
		{
			LinkedHashMap whereMap = new LinkedHashMap();
			whereMap.put("ACADEMY_RAWID"  , academyRawId + "");
			
			dasx.delete("COUPON_MONTHLY_TOTAL_TRX", "WHERE academy_rawid = ? and check_month=to_char(sysdate,'yyyymm')", whereMap);
			
		} catch (Exception e) {
			logger.warn(e);
		} finally{
		}
	}
	
	private boolean getMonthlyTotalCompanyForToDay(DAS dasx, long academyRawId)
	{
		RecordSet rset = null;
		boolean resultValue = false;
		try {
			String sql =" SELECT * FROM coupon_monthly_total_trx WHERE academy_rawid = ? AND check_month = TO_CHAR(SYSDATE,'yyyymm') AND TO_CHAR(create_dt,'yyyymmdd') = ? AND ROWNUM = 1 ";

			LinkedHashMap keyList = new LinkedHashMap();
			keyList.put("ACADEMY_RAWID", academyRawId + "");
			keyList.put("CREATE_DT"   , nextRunDay);
			rset = dasx.select(sql, keyList);
			
			while(rset.next())
			{
				resultValue = true;
			}
			
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
		}
		return resultValue;
	}
	
	
	private void getCurrentCompanyByUserRawId(DAS dasx, UserInfo userInfo)
	{
		RecordSet rset = null;
		
		try {
			String sql =" SELECT PREV_USE, NOW_USE                                                                                                                                   "+
						" FROM                                                                                                                                                      "+
						" (                                                                                                                                                          "+
						"     SELECT USER_RAWID, SUBSTR(MAX(SYS_CONNECT_BY_PATH(COMPANY, ',')), 2) NOW_USE                                                                     "+
						"     FROM                                                                                                                                                   "+
						"     (                                                                                                                                                      "+
						"             SELECT USER_RAWID,COMPANY,  ROW_NUMBER() OVER (PARTITION BY USER_RAWID ORDER BY  COMPANY)RNUM                                                  "+
						"             FROM                                                                                                                                           "+
						"             (                                                                                                                                              "+
						"              SELECT UM.RAWID USER_RAWID ,CPD.NAME COMPANY                                                                                                 "+
						"                FROM USER_MST UM                                                                                                                            "+                
						"                INNER JOIN ACADEMY_MST AM ON AM.RAWID=UM.ACADEMY_RAWID  AND UM.ACADEMY_RAWID=? AND AM.USE_ACAMAN_YN = 'Y'                                 "+                  
						"                INNER JOIN CLASS_USER_LINK_MST CLM ON CLM.USER_RAWID=UM.RAWID                                                                               "+                
						"                INNER JOIN CLASS_MST CM ON CM.RAWID=CLM.CLASS_RAWID AND CM.DEL_YN = 'N'                                                                     "+                
						"                INNER JOIN CLASS_BOOK_LINK_MST CBM ON CBM.CLASS_RAWID=CM.RAWID  AND TO_CHAR (SYSDATE, 'YYYYMMDD') BETWEEN CBM.START_DT AND CBM.END_DT       "+
						"                INNER JOIN BOOK_MST BM ON BM.RAWID=CBM.BOOK_RAWID  AND BM.DEL_YN = 'N'                                                                      "+
						"                INNER JOIN SERIES_MST SM ON SM.SERIES_CD = BM.SERIES_CD  AND SM.DEL_YN = 'N'                                                                "+
						"                INNER JOIN PARTNER_MST PM ON SM.COMPANY_CD = PM.COMPANY_CD  AND PM.DEL_YN = 'N' AND PM.SELECT_YN = 'Y'                                      "+
						"                INNER JOIN PRODUCT_GROUP_MST PG ON  PG.PRODUCT_GROUP_CD = AM.PRODUCT_GROUP_CD   AND PG.DEL_YN = 'N'                                         "+
						"                INNER JOIN PRODUCT_MST PDM ON  PDM.RAWID = PG.PRODUCT_RAWID AND PDM.DEL_YN = 'N'                                                            "+
						"                INNER JOIN PRODUCT_LINK_MST PL ON PL.SERIES_RAWID = SM.RAWID AND PL.PRODUCT_RAWID = PDM.RAWID                           "+
						"                INNER JOIN (SELECT * FROM CODE_MST WHERE CATEGORY = 'STUDY_CD' AND USE_YN = 'Y') CSD ON CSD.CODE = SM.STUDY_CD                              "+
						"                INNER JOIN (SELECT * FROM CODE_MST WHERE CATEGORY = 'COMPANY_CD' AND USE_YN = 'Y') CPD ON CPD.CODE = SM.COMPANY_CD                          ";
		
			if(_organizationTypeCD.equals("SR"))
			{
				sql += " INNER JOIN PARTNER_CHARGE_MST PCM ON PM.RAWID = PCM.PARTNER_RAWID AND PCM.ORGANIZATION_TYPE_CD='SR' AND PCM.CHARGE_YN='Y'  ";
			}
			
			sql += "                WHERE UM.RAWID=?                                                                                                                            "+
						"                GROUP BY UM.RAWID, CPD.NAME                                                                                                                 "+
						"             )                                                                                                                                              "+
						"      )                                                                                                                                                     "+
						"      START WITH rnum = 1                                                                                                                                   "+
						"      CONNECT BY PRIOR rnum = rnum - 1 AND PRIOR USER_RAWID = USER_RAWID                                                                                    "+
						"      GROUP BY USER_RAWID                                                                                                                                   "+
						"  )CURR                                                                                                                                                     "+
						"  LEFT OUTER JOIN                                                                                                                               "+
						"  (                                                                                                                                                         " +
						"   SELECT USER_RAWID, NOW_USE PREV_USE" +
						"   FROM                                                                                                                                                   "+
						"   (                                                                                                                                                      "+
						"     	SELECT USER_RAWID, NOW_USE, ROW_NUMBER() OVER (ORDER BY CREATE_DT DESC)RNUM FROM COUPON_USE_TRX WHERE ACADEMY_RAWID=? AND USER_RAWID=?               "+
						"		   AND TO_CHAR(create_dt,'yyyy-mm') = TO_CHAR(SYSDATE,'yyyy-mm') "+
						"    )WHERE RNUM=1                                                                                                                                          "+
						"  )PREV ON PREV.USER_RAWID=CURR.USER_RAWID                                                                                                                    ";

			LinkedHashMap keyList = new LinkedHashMap();
			keyList.put("ACADEMY_RAWID1" , userInfo.getAcademyRawId() + "");
			keyList.put("USER_RAWID1"    , userInfo.getRawId() + "");
			keyList.put("ACADEMY_RAWID2" , userInfo.getAcademyRawId() + "");
			keyList.put("USER_RAWID2"    , userInfo.getRawId() + "");
			rset = dasx.select(sql, keyList);
			
			while(rset.next())
			{
				userInfo.setPrevUse(rset.getString("PREV_USE"));
				userInfo.setNowUse(rset.getString("NOW_USE"));
			}
			
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
		}
	}
	
	
	//4-2-1. 누적출판사, 적용후 누적출판사 체크후 출판사 변경사항이 있으면 COUPON_MONTHLY_TOTAL_TRX insert
	private void GetMonthlyTotalCompany(UserInfo userInfo, DAS das, Date hstDate)
	{
		String beforeCompany = this.getMonthlyCompanyByUserRawId(das, userInfo.getAcademyRawId(), userInfo.getRawId());
		String afterCompany = beforeCompany;
		if(userInfo.getDifferYN().equals("Y"))
		{
			this.InsertMonthlyTotalCompany(das, hstDate, userInfo.getAcademyRawId(), userInfo.getRawId());
			afterCompany = this.getMonthlyCompanyByUserRawId(das, userInfo.getAcademyRawId(), userInfo.getRawId());
		}
		userInfo.setMonthlyCompanyBefore(beforeCompany);
		userInfo.setMonthlyCompanyAfter(afterCompany);
	}
	
	
	private int insertCouponUseTrx(DAS dasx, Date hstDate, UserInfo userInfo)throws Exception {
		int rInt = 0;
		
		LinkedHashMap columnMap = new LinkedHashMap();
		LinkedHashMap culmMap = new LinkedHashMap();
		LinkedHashMap whereMap = new LinkedHashMap();
		
		columnMap.put("RAWID+SEQ_COUPON_USE_TRX.nextval", null);
		columnMap.put("ACADEMY_RAWID"    		, userInfo.getAcademyRawId());
		columnMap.put("USER_RAWID"       		, userInfo.getRawId());
		columnMap.put("PREV_TOTAL_USE"   		, userInfo.getMonthlyCompanyBefore());
		columnMap.put("NOW_TOTAL_USE"    		, userInfo.getMonthlyCompanyAfter());
		if(userInfo.getPrevUse()==null){
			columnMap.put("PREV_USE"         		, "");
		}else{
			columnMap.put("PREV_USE"         		, userInfo.getPrevUse());
		}
		columnMap.put("NOW_USE"          		, userInfo.getNowUse());
		columnMap.put("WORKED_BY"        		, "S");
		columnMap.put("CREATE_TYPE_CD"   		, "S");
		columnMap.put("COUPON_WORK_TYPE_CD"     , userInfo.getCouponWorkTypeCD());
		columnMap.put("PREV_REMAIN_COUPON_COUNT", userInfo.getPrevRemainCouponCount());
		columnMap.put("BASE_COUPON_COUNT"		, userInfo.getBaseCouponCount());
		columnMap.put("COMPANY_COUPON_COUNT"    , userInfo.getAddCompanyCouponCount());
		columnMap.put("CREATE_DT"				, hstDate);
		columnMap.put("USE_COUPON_COUNT"        , userInfo.getTotalCouponCount());
		
		if(userInfo.getMinusYN().equals("Y"))
		{
			columnMap.put("USE_COUPON_COUNT"   , userInfo.getPrevRemainCouponCount());
			columnMap.put("MINUS_COUPON_COUNT" , userInfo.getMinusCouponCount());
			columnMap.put("MINUS_YN"           , "Y");
		}
		
		//삭제조건
		whereMap.put("USER_RAWID"       		, userInfo.getRawId());
		
		//COUPON_USE_LAST_MST
		culmMap.put("RAWID+SEQ_COUPON_USE_LAST_MST.NEXTVAL", null);
		culmMap.put("CREATE_DT+SYSDATE", null);
		culmMap.put("ACADEMY_RAWID", userInfo.getAcademyRawId());
		culmMap.put("USER_RAWID", userInfo.getRawId());
		culmMap.put("NOW_TOTAL_USE", userInfo.getMonthlyCompanyAfter());
		culmMap.put("NOW_USE", userInfo.getNowUse());
		
		try{
			dasx.delete("COUPON_USE_LAST_MST", "WHERE user_rawid = ?  ", whereMap);
			dasx.insert("COUPON_USE_LAST_MST", culmMap);
			rInt = dasx.insert("COUPON_USE_TRX", columnMap);
		}catch (Exception e) {
			rInt = 0;
		} 
		
		return rInt;
	}
	
	
	private int updateUserCouponMst(DAS das, Date applyDate, ApplyCouponData couponData, UserInfo userInfo, String ApplyYN) throws Exception {


		LinkedHashMap columnMap = new LinkedHashMap();
		//columnMap.put("CP_APPLY_YN + '" + couponData.getStopApplyYN() + "'", null);
		columnMap.put("CP_APPLY_YN", ApplyYN );
		
		if(ApplyYN.equals("Y")){
			columnMap.put("CP_APPLY_DT"   , userInfo.getApplyStartDay());
		}
		else
		{
			columnMap.put("CP_APPLY_DT"   , applyDate);
		}
		
		LinkedHashMap whereMap = new LinkedHashMap();
		whereMap.put("USER_RAWID"  , userInfo.getRawId() + "");
		whereMap.put("COUPON_TYPE" , couponData.getCouponType() + "");
		
		return das.update("USER_COUPON_MST", columnMap, "WHERE user_rawid = ? AND coupon_type=? ", whereMap);
	}
	
	
	
	
	
	/**
	 * 학원별 문자보내기
	 * 1. 1일,4일,5일 각 해당 메세지와, 정지학생 정보 문자보내기
	 * 2. 3일치를 계산해서 미리 충전하라는 문자보내기
	 */
	private void SendSMSForSM()
	{
		this.SendSMSForBaseInform();
		this.SendSMSForRequiredCouponInform();
	}
	
	/**
	 * 학원별 문자보내기
	 * 1일,4일,5일 각 해당 메세지와, 정지학생 정보 문자보내기 
	 */
	private void SendSMSForBaseInform()
	{ 
		DAS das = null;
		RecordSet rset = null;
		
		try {
			das = new DAS(DAS_CONFIG);
	
			String sms_base = "";
			String sms_msg1 = "매월 1일에서 4일까지는 출판사변경기간입니다."; //1일
			String sms_msg2 = "5일부터 기본3개외 추가출판사 코인차감이 됩니다.필요하시면 더 충전해주세요."; //4일
			String sms_msg3 = "오늘부터 추가출판사에 대한 코인차감이 됩니다.필요하시면 더 충전해주세요."; //5일
			
			if(currentDay == 1)
				sms_base = sms_msg1;
			else if(currentDay == 4)
				sms_base = sms_msg2;
			else if(currentDay == 5)
				sms_base = sms_msg3;
			
			
			String sql ="SELECT rawid, user_id, name, cell_number, stopnoclasscount, stopnocouponcount, coupon_type "+
				" FROM "+
				" ( "+
				" 	SELECT am.rawid, am.user_id, am.name, am.cell_number, snc.non_apply_user_cnt stopnoclasscount, sd.non_apply_user_cnt stopnocouponcount ,acm.coupon_type "+
				" 	FROM academy_mst am "+
				" 	LEFT OUTER JOIN (SELECT * FROM coupon_hst WHERE create_type_cd='S' AND TO_DATE(apply_dt) = ? AND use_type_cd = 'SNC' AND coupon_type in('NC','YC') AND non_apply_user_cnt > 0) snc ON snc.academy_rawid = am.rawid "+
				" 	LEFT OUTER JOIN (SELECT * FROM coupon_hst WHERE create_type_cd='S' AND TO_DATE(apply_dt) = ? AND use_type_cd = 'D' AND coupon_type in('NC','YC') AND non_apply_user_cnt > 0) sd ON sd.academy_rawid = am.rawid "+
				" 	INNER JOIN academy_coupon_mst acm ON acm.academy_rawid = am.rawid AND acm.coupon_type in('NC','YC') "+
				" 	INNER JOIN sms_academy_mst sam ON sam.academy_rawid = am.rawid AND sam.remain_count > 0 "+
				" 	WHERE am.coupon_sms_yn='Y'  "+
				" ) ";
				
			
			LinkedHashMap keyList = new LinkedHashMap();
			keyList.put("APPLY_DAY"     , nextRunDay);
			//keyList.put("coupon_type1"     , "NS");
			keyList.put("APPLY_DT"      , nextRunDay);
			//keyList.put("coupon_type2"     , "NS");
			//keyList.put("coupon_type3"     , "NS");
			rset = das.select(sql, keyList);
			
			while(rset.next())
			{
				String msgAcademy="";
				String stopNoClassCount = rset.getString("STOPNOCLASSCOUNT") == null ? "" : rset.getString("STOPNOCLASSCOUNT");
				String stopNoCouponCount = rset.getString("STOPNOCOUPONCOUNT") == null ? "" : rset.getString("STOPNOCOUPONCOUNT"); 
				long academyRawId = rset.getLong("RAWID");
				
				if(stopNoClassCount.length() > 0)
					msgAcademy = "*무소속학급정지:" + stopNoClassCount + "명";
				if(stopNoCouponCount.length() > 0)
					msgAcademy = "*쿠폰부족정지:" + stopNoCouponCount + "명";
				
			
				
				String msg =  sms_base + msgAcademy;
				if(msg.length() > 0)
				{
					if(rset.getString("coupon_type").equals("YC"))
						msg = "[영신]"+msg;
					else if(rset.getString("coupon_type").equals("NC"))
						msg = "[SM]"+msg;
					
					LinkedHashMap alInform = new LinkedHashMap();
					alInform.put("ACADEMY_RAWID" 	, rset.getString("RAWID"));
					alInform.put("USER_ID"			, rset.getString("USER_ID"));
					alInform.put("NAME"				, rset.getString("NAME"));
					alInform.put("CELL_NUMBER" 		, rset.getString("CELL_NUMBER") == null ? "" : rset.getString("CELL_NUMBER"));
					alInform.put("MESSAGE" 			, msg);
					this.SendSMS(alInform, false);
				}
			}
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
			if (das != null) try { das.close(); } catch(Exception ex) {}
		}
	}
	
	
	/**
	 * 학원별 문자보내기
	 * 3일치를 계산해서 미리 충전하라는 문자보내기
	 */
	private void SendSMSForRequiredCouponInform()
	{ 
		DAS das = null;
		RecordSet rset = null;
	
		try {
			das = new DAS(DAS_CONFIG);
			String sms_base="";
						
			String sql =" SELECT am.rawid, am.user_id, am.name, am.cell_number, cm.remain_coupon_count, remainday, cm.coupon_type  "+
			" FROM academy_mst am "+
			" INNER JOIN "+
			" (                         "+
			"     SELECT acm.academy_rawid, acm.remain_coupon_count, trunc(acm.remain_coupon_count /usercount) remainday, acm.coupon_type "+
			"     FROM academy_coupon_mst acm    "+
			" 	  INNER JOIN   "+
		    "     (    "+
		    "       SELECT academy_rawid, coupon_type, count(*) usercount "+
		    "         FROM user_coupon_mst um "+
		    "        WHERE um.cp_apply_yn = ? AND um.coupon_type in('NC') AND TO_CHAR(um.cp_apply_dt, 'yyyymmdd') <= ? AND um.cp_apply_dt <= to_date(?)-6 "+
		    "        GROUP BY academy_rawid,coupon_type   "+
		    "     )um ON acm.academy_rawid = um.academy_rawid AND acm.coupon_type = um.coupon_type   "+
			"     WHERE acm.remain_coupon_count  <= usercount * 3 "+
			"       AND acm.coupon_type in('NC') "+
			" ) cm ON am.rawid = cm.academy_rawid "+  
			//" INNER JOIN sms_academy_mst sam ON sam.academy_rawid = am.rawid AND sam.remain_count > 0 "+
			" WHERE am.coupon_sms_yn = ?  and am.pay_cd != 'M'  ";
	
			LinkedHashMap keyList = new LinkedHashMap();
			keyList.put("cp_apply_yn"     , "Y");
			//keyList.put("coupon_type1"     , "NC");
			keyList.put("apply_dt1"     , nextRunDay);
			keyList.put("apply_dt2"     , nextRunDay);
			//keyList.put("coupon_type2"     , "NC");
			keyList.put("coupon_sms_yn"     , "Y");
			rset = das.select(sql, keyList);
			
			String smsSendAcademyList = "";
			
			while(rset.next())
			{
				sms_base = "일 사용 가능한 코인이 남았습니다.필요하시면 더 충전해주세요.";
				long academyRawId = rset.getLong("RAWID");
				String remainday = rset.getString("remainday");

				if(rset.getString("coupon_type").equals("YC"))
					sms_base = "[영신] "+remainday+sms_base;
				else if(rset.getString("coupon_type").equals("NC"))
					sms_base = "[SM] "+remainday+sms_base;
				
				LinkedHashMap alInform = new LinkedHashMap();
				alInform.put("ACADEMY_RAWID" 	, rset.getString("RAWID"));
				alInform.put("USER_ID"			, rset.getString("USER_ID"));
				alInform.put("NAME"				, rset.getString("NAME"));
				alInform.put("CELL_NUMBER" 		, rset.getString("CELL_NUMBER") == null ? "" : rset.getString("CELL_NUMBER"));
				alInform.put("MESSAGE" 			, sms_base);
				
				smsSendAcademyList += "{" + alInform.get("ACADEMY_RAWID");
				smsSendAcademyList += "," + alInform.get("USER_ID");
				smsSendAcademyList += "," + alInform.get("NAME");
				smsSendAcademyList += "," + alInform.get("CELL_NUMBER");
				smsSendAcademyList += "," + alInform.get("MESSAGE") + "}";
				
				//부족한 경우 무조건 보냄.. 
				//0일인 경우로 표시 되서 문자 원하지 않으면 문자사용 안함으로 변경하도록 응대할것.. 실시간으로 조건 추가 했다가 다시 실시간 요청으로 조건 원복 시킴.
				//if(!remainday.equals("0")){
					this.SendSMS(alInform, true);
				//}

			}
			
			//logger.info("[ " + nextRunDay + " ] " + smsSendAcademyList);
		} catch (Exception e) {
			logger.warn(e);
		} finally{
			if (rset != null) try { rset.close(); } catch(Exception ex) {}
			if (das != null) try { das.close(); } catch(Exception ex) {}
		}
	}
	
	
	/**
	 * 문자보내기
	 * 1.SMS_USER_TRX insert
	 * 2.SMS_ACADEMY_TRX insert
	 * 3.SDK_SMS_SEND insert
	 * 4.SMS_ACADEMY_MST update
	 */
	
	private void SendSMS(LinkedHashMap alSMSInform, boolean isFree)
	{
		
		DAS das = null;
		try
		{
			das = new DAS("EN");
			
			String cell_number = ""; 
			String academy_name = "";
			String user_id = "";
			String message = "";
			long academyRawId  = -1;
			
			if(alSMSInform.containsKey("ACADEMY_RAWID"))
				academyRawId = Long.parseLong(alSMSInform.get("ACADEMY_RAWID").toString());
			
			if(alSMSInform.containsKey("CELL_NUMBER"))
				cell_number = alSMSInform.get("CELL_NUMBER").toString();
			
			if(alSMSInform.containsKey("NAME"))
				academy_name = alSMSInform.get("NAME").toString();
			
				user_id = alSMSInform.get("USER_ID").toString();
			
			if(alSMSInform.containsKey("MESSAGE"))
				message = alSMSInform.get("MESSAGE").toString();
			
//			academyRawId=450;
//			cell_number = "01088394581"; 
//			logger.info("user_id:" + user_id + " - start");
			
			if(message.length() > 0 && cell_number.length() > 0)
			{
				das.beginTrans();
				
				//1.SMS_USER_TRX insert
				String destInfo = "";
				LinkedHashMap columnMap = new LinkedHashMap();
				long seq_em_tran_pr = -1; 
				
				if (seq_em_tran_pr < 0) seq_em_tran_pr = das.getSequence_Nextval("EM_TRAN_PR");
				

				if ((seq_em_tran_pr > 0))
				{
					//2.SMS_ACADEMY_TRX insert
					columnMap.clear();
					columnMap.put("TRAN_PR"				, seq_em_tran_pr);
					columnMap.put("TRAN_PHONE"		    , cell_number);
					columnMap.put("TRAN_CALLBACK"		,"02-581-4353");
					columnMap.put("TRAN_STATUS" 	    , "1");
					columnMap.put("TRAN_DATE+SYSDATE"	, null);
					columnMap.put("TRAN_MSG"		    , message);
					columnMap.put("TRAN_ETC1"			, academy_name);
					columnMap.put("TRAN_ETC2"			, "SMEnglish");
					columnMap.put("TRAN_ETC3"			, "M");
					columnMap.put("TRAN_TYPE"			, "4");
					das.insert("EM_TRAN"		, columnMap);
			
					if(!isFree)
					{
						//4.SMS_ACADEMY_MST update
						columnMap.clear();
						columnMap.put("REMAIN_COUNT+(REMAIN_COUNT - 1)", null);
					
						LinkedHashMap whereMap = new LinkedHashMap();
						whereMap.put("ACADEMY_RAWID"  , academyRawId + "");
						das.update("SMS_ACADEMY_MST", columnMap, "WHERE academy_rawid = ?  ", whereMap);
					}
					das.commit();
				} else {
					das.rollback();
				}
			}
		} catch (Exception e)
		{
			logger.warn(e);
			das.rollback(); 
		} finally
		{
			if (das != null) try { das.close(); } catch(Exception ex) {}
		}
	}
	
	
	// ***************************************************************** SMEnglish쿠폰 End **************************************************************************
		
	
}

