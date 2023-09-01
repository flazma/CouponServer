package kr.co.smenglish.monthmgr;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import kr.co.smenglish.das.sql.DAS3;
import kr.co.smenglish.das.sql.Function;
import kr.co.smenglish.servermgr.Constants;

import org.apache.log4j.Logger;

import com.smenglish.bizwork.json.coupon.sql.SQLCouponCheck;
import com.smenglish.core.config.AppConfig;
import com.smenglish.core.util.CommonUtil;
import com.smenglish.core.util.ServerTime;

public class SqlList {
	static Logger logger = Logger.getLogger(MonthMng.class);
	private DAS3 das3 = new DAS3(Constants.DAS_CONFIG);
	private Connection conn = null;
	
	//코인제 > 수강신청 
	public int startCourses(String academy_rawid, String user_type, String user_rawid, String user_list, String start_dt, String coupon_type) {
		int iResult = 0;
		ServerTime st = new ServerTime();
		Connection conn = null;
		String now_dt = st.getCurrentPatternDate("yyyy-MM-dd");
		String sql = "";
		SQLCouponCheck couponChk = new SQLCouponCheck();
		
		try {
			LinkedList<String> dataList = new LinkedList<String>();
			LinkedList<Object> rowset = null;
			LinkedList<LinkedHashMap<Object, Object>> cmttList = new LinkedList<LinkedHashMap<Object,Object>>();
			LinkedList<LinkedHashMap<Object, Object>> cutList = new LinkedList<LinkedHashMap<Object,Object>>();
			LinkedList<LinkedHashMap<Object, Object>> uctInsertList = new LinkedList<LinkedHashMap<Object,Object>>();
			LinkedList<LinkedHashMap<Object, Object>> ucmInsertList = new LinkedList<LinkedHashMap<Object,Object>>();
			LinkedList<LinkedHashMap<Object, Object>> uchInsertList = new LinkedList<LinkedHashMap<Object,Object>>();
			LinkedList<LinkedHashMap<Object, Object>> ucmUpdateList = new LinkedList<LinkedHashMap<Object,Object>>();
			LinkedList<LinkedList<Object>> ucmUpdateDataList = new LinkedList<LinkedList<Object>>();
			LinkedList<LinkedList<String>> uctDeleteDataList = new LinkedList<LinkedList<String>>();
			LinkedList<LinkedHashMap<Object, Object>> culmInsertList = new LinkedList<LinkedHashMap<Object,Object>>();
			LinkedList<LinkedList<Object>> culmUpdateDataList = new LinkedList<LinkedList<Object>>();	
			
			conn = das3.beginTrans();
			String arrUserList[] = user_list.split(",");
			int totalUseCoupon = 0;
			String work_type_cd = "";
			
			if(CommonUtil.compareDate(now_dt, start_dt) == 0) 
				work_type_cd = "S";
			else 
				work_type_cd = "RV";
			
			/*
			 * CREATE_DT를 키값으로 통일 시키기 위한 값
			 */
			String create_dt = 
				((LinkedHashMap<Object, Object>) das3.select(null, "SELECT TO_CHAR(SYSDATE, 'YYYY-MM-DD HH24:MI:SS') CREATE_DT FROM DUAL", null).get(0)).get("create_dt").toString();
			
			Function funcCreateDt = new Function();
			funcCreateDt.columnName = "CREATE_DT";
			funcCreateDt.sentence = "TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS')";
			funcCreateDt.funcList = new LinkedList<Object>();
			funcCreateDt.funcList.add(create_dt);
			
			Function funcHstDt = new Function();
			funcHstDt.columnName = "HST_DT";
			funcHstDt.sentence = "TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS')";
			funcHstDt.funcList = new LinkedList<Object>();
			funcHstDt.funcList.add(create_dt);
			
			/*
			 * 현재 남은 쿠폰 수량
			 */
			int remainCount = couponChk.getRemainCouponCount(academy_rawid);
			
			for(int i = 0; i < arrUserList.length; i++) {
				String prev_total_use_list = "", prev_now_use_list = "";
				
				/*
				 * USER_BOOK_LINK_MST에서 학생키에 세팅된 도서들 중
				 * 사용 시작된 도서들의 출판사 정리
				 * 현재 달에 이미 사용을 한 도서는 제외
				 */
				sql = "SELECT PM.COMPANY_CD, PM.NAME " +
				"FROM USER_BOOK_LINK_MST UB " +
				"INNER JOIN BOOK_MST BM ON BM.RAWID = UB.BOOK_RAWID AND " +
				"UB.START_DT <= TO_CHAR(SYSDATE, 'YYYY-MM-DD') AND " +
				"UB.END_DT >= TO_CHAR(SYSDATE, 'YYYY-MM-DD') " +
				"INNER JOIN PARTNER_MST PM ON PM.COMPANY_CD = BM.COMPANY_CD " +
				"AND PM.SELECT_YN = ? " +
				"WHERE UB.ACADEMY_RAWID = ? AND UB.USER_RAWID = ? " +
				"AND BM.COMPANY_CD NOT IN ( " +
				"    SELECT COMPANY_CD FROM COUPON_MONTHLY_TOTAL_TRX " +
				"    WHERE USER_RAWID = ? AND CHECK_MONTH = TO_CHAR(SYSDATE, 'YYYYMM') " +
				") " +
				"GROUP BY PM.COMPANY_CD, PM.NAME " +
				"ORDER BY PM.NAME ";
				dataList.clear();
				dataList.add("Y");
				dataList.add(academy_rawid);
				dataList.add(arrUserList[i]);
				dataList.add(arrUserList[i]);
				rowset = das3.select(null, sql, dataList);
				
				if(work_type_cd.equals("S")) {
					for(int j = 0; j < rowset.size(); j++) {
						/*
						 * 새로 시작되는 출판사 COUPON_MONTHLY_TOTAL_TRX INSERT
						 */
						LinkedHashMap<Object, Object> temp = (LinkedHashMap<Object, Object>) rowset.get(j);
						LinkedHashMap<Object, Object> cmttMap = new LinkedHashMap<Object, Object>();
						Function f_check_month = new Function();
						f_check_month.columnName = "CHECK_MONTH";
						f_check_month.sentence = "TO_CHAR(SYSDATE, 'YYYYMM')";
						
						cmttMap.put("RAWID+SEQ_COUPON_MONTHLY_TOTAL_TRX.NEXTVAL", null);
						cmttMap.put("ACADEMY_RAWID", academy_rawid);
						cmttMap.put("USER_RAWID", arrUserList[i]);
						cmttMap.put(f_check_month, null);
						cmttMap.put("COMPANY_CD", temp.get("company_cd"));
						cmttMap.put(funcCreateDt, null);
						cmttList.add(cmttMap);
						// 새로 시작되는 출판사 COUPON_MONTHLY_TOTAL_TRX INSERT
					}
				}
				// 사용 시작 출판사 처리
				
				// 이전 사용 내역 처리
				dataList.clear();
//				sql = "SELECT MAX(CREATE_DT), NOW_TOTAL_USE, NOW_USE  " +
//				"FROM COUPON_USE_TRX " +
//				"WHERE ACADEMY_RAWID = ? AND USER_RAWID = ? AND CREATE_DT  >= TO_CHAR(SYSDATE, 'YYYY-MM')||'-01' " +
//				"GROUP BY NOW_TOTAL_USE, NOW_USE ";
				
				sql = "SELECT NOW_TOTAL_USE, NOW_USE FROM COUPON_USE_LAST_MST " +
						"WHERE USER_RAWID = ? AND ACADEMY_RAWID = ? ";
				dataList.add(arrUserList[i]);
				dataList.add(academy_rawid);
				
				rowset = das3.select(null, sql, dataList);
				
				if(rowset.size() > 0) {
					LinkedHashMap<Object, Object> temp = (LinkedHashMap<Object, Object>) rowset.get(0);
					prev_now_use_list = temp.get("now_use").toString();
					prev_total_use_list = temp.get("now_total_use").toString();
				}
				// 이전 사용 내역 처리
				
				sql = "SELECT RAWID, CP_APPLY_YN, TO_CHAR(CP_APPLY_DT, 'YYYY-MM-DD') CP_APPLY_DT FROM USER_COUPON_MST " +
				"WHERE ACADEMY_RAWID = ? AND USER_RAWID = ?";
				dataList.clear();
				dataList.add(academy_rawid);
				dataList.add(arrUserList[i]);
				rowset = das3.select(null, sql, dataList);
				
				LinkedHashMap<Object, Object> ucmMap = new LinkedHashMap<Object, Object>();
				LinkedHashMap<Object, Object> uchMap = new LinkedHashMap<Object, Object>();
				String pre_apply_dt = "";
				String pre_apply_yn = "";
				
				if(rowset.size() > 0) {
					
					pre_apply_yn = ((LinkedHashMap<Object, Object>) rowset.get(0)).get("cp_apply_yn").toString();
					pre_apply_dt = ((LinkedHashMap<Object, Object>) rowset.get(0)).get("cp_apply_dt").toString();
					
					//사용중지(쿠폰부족)일 때  USER_COUPON_TRX 에 오늘 날짜 (APPLY_DAY)이후 DELETE				
					if(pre_apply_yn.equals("M")){
						LinkedList<String> uctDeleteList = new LinkedList<String>();				
						uctDeleteList.add(arrUserList[i]);						
						uctDeleteList.add(coupon_type);
						uctDeleteDataList.add(uctDeleteList);				
					}					
					
					// USER_COUPON_MST UPDATE
					LinkedList<Object> updateDataList = new LinkedList<Object>();
					ucmMap.put("CP_APPLY_YN", "Y");
					ucmMap.put("CP_APPLY_DT", start_dt);
					ucmMap.put(funcCreateDt, null);
					updateDataList.add(((LinkedHashMap<Object, Object>) rowset.get(0)).get("rawid"));
					ucmUpdateList.add(ucmMap);
					ucmUpdateDataList.add(updateDataList);					
				} else {
					// USER_COUPON_MST INSERT
					ucmMap.put("RAWID+SEQ_USER_COUPON_MST.NEXTVAL", null);
					ucmMap.put("ACADEMY_RAWID", academy_rawid);
					ucmMap.put("COUPON_TYPE", coupon_type);
					ucmMap.put("USER_RAWID", arrUserList[i]);
					ucmMap.put("CP_APPLY_YN", "Y");
					ucmMap.put("CP_APPLY_DT", start_dt);
					ucmMap.put("CREATE_DT+SYSDATE", null);
					ucmMap.put("CREATE_BY", user_rawid);
					ucmMap.put("CREATE_TYPE_CD", user_type);
					ucmInsertList.add(ucmMap);
				}

				// USER_COUPON_HST INSERT
				uchMap.put("RAWID+SEQ_USER_COUPON_HST.NEXTVAL", null);
				uchMap.put("ACADEMY_RAWID", academy_rawid);
				uchMap.put("USER_RAWID", arrUserList[i]);
				uchMap.put("PRE_CP_APPLY_YN", pre_apply_yn);
				uchMap.put("PRE_CP_APPLY_DT", pre_apply_dt);
				uchMap.put(funcCreateDt, null);
				uchMap.put("CREATE_BY", user_rawid);
				uchMap.put("CREATE_TYPE_CD", user_type);
				uchMap.put("COUPON_TYPE", coupon_type);
				uchMap.put("TO_CP_APPLY_YN", "Y");
				uchInsertList.add(uchMap);
				
				/*
				 * USER_COUPON_TRX INSERT
				 * 필수 사용기간 7일 동안의 데이터를 미리 입력
				 */
				for(int k = 0; k < 7; k++) {
					LinkedHashMap<Object, Object> uctMap = new LinkedHashMap<Object, Object>();
					Function funcApplyDt = new Function();
					funcApplyDt.columnName = "APPLY_DAY";
					funcApplyDt.sentence = "TO_CHAR(TO_DATE(?) + ?, 'YYYYMMDD')";
					funcApplyDt.funcList = new LinkedList<Object>();
					funcApplyDt.funcList.add(start_dt);
					funcApplyDt.funcList.add(k);
					
					uctMap.put("RAWID+SEQ_USER_COUPON_TRX.NEXTVAL", null);
					uctMap.put("ACADEMY_RAWID", academy_rawid);
					uctMap.put("USER_RAWID", arrUserList[i]);
					uctMap.put("APPLY_TYPE_CD", "W");
					uctMap.put(funcApplyDt, null);
					uctMap.put("CREATE_DT+SYSDATE", null);
					uctMap.put(funcHstDt, null);
					uctMap.put("COUPON_TYPE", coupon_type);
					uctInsertList.add(uctMap);
					// USER_COUPON_TRX INSERT
				}
				
				/*
				 * COUPON_USE_TRX INSERT
				 * 1. 무료 사용 기간에는 모든 사용 쿠폰이 0으로 처리 되도록
				 * 2. 매월 1~4일엔 추가 과금은 발생하지 않음
				 */
				LinkedHashMap<Object, Object> cutMap = new LinkedHashMap<Object, Object>();
				LinkedHashMap<Object, Object> culmMap = new LinkedHashMap<Object, Object>();
				int use_coupon_count = 0, overCount = 0, baseCount = 0;
				
				if(couponChk.checkAcademyBillYn(academy_rawid) && !couponChk.checkUserTutorYn(arrUserList[i])) {
					// 무료 사용 중이거나 무료 사용 학생이 아니면 기본 수량은 반드시 차감
					baseCount = 7;
				} 
//				if(couponChk.checkAcademyBillYn(academy_rawid) && !couponChk.checkUserTutorYn(arrUserList[i]) && 
//						st.getCurrentDate() > 4){
//					// 무료 사용기간도 아니고 커리 조정 기간도 아니며 무료 사용 학생도 아니면
//					String temp_overCount = jsonReq != null ? ((JSONObject) jsonReq.get(arrUserList[i])).get("overCount").toString() : req.getParameter(arrUserList[i]+"[overCount]");
//					overCount = (Integer.parseInt(temp_overCount) * 3);
//				}
				use_coupon_count = baseCount + overCount;
				
				String now_total_use = getUserAccumulateCompanyList(academy_rawid, arrUserList[i], "", "");	
				String now_use_list = getUserNowUseCompanyList(arrUserList[i], academy_rawid);
				
				cutMap.put("RAWID+SEQ_COUPON_USE_TRX.NEXTVAL", null);
				cutMap.put(funcCreateDt, null);
				cutMap.put("ACADEMY_RAWID", academy_rawid);
				cutMap.put("USER_RAWID", arrUserList[i]);
				cutMap.put("PREV_TOTAL_USE", prev_total_use_list);
				cutMap.put("NOW_TOTAL_USE", !work_type_cd.equals("S") ? "" : now_total_use);
				cutMap.put("PREV_USE", prev_now_use_list);
				cutMap.put("NOW_USE", !work_type_cd.equals("S") ? "" : now_use_list);
				cutMap.put("WORKED_BY", "W");
				cutMap.put("CREATE_TYPE_CD", user_type);
				cutMap.put("COUPON_WORK_TYPE_CD", work_type_cd);
				cutMap.put("BASE_COUPON_COUNT", baseCount);
				cutMap.put("COMPANY_COUPON_COUNT", overCount);
				cutMap.put("USE_COUPON_COUNT", use_coupon_count);
				cutMap.put("MINUS_COUPON_COUNT+0", null);
				cutMap.put("CREATE_BY", user_rawid);
				cutMap.put("MINUS_YN+'N'", null);
				cutMap.put("PREV_REMAIN_COUPON_COUNT", remainCount);
				cutList.add(cutMap);
				
				totalUseCoupon += use_coupon_count;
				
				//COUPON_USE_LAST_MST
				LinkedList<Object> updateDataList = new LinkedList<Object>();
				culmMap.put("RAWID+SEQ_COUPON_USE_LAST_MST.NEXTVAL", null);
				culmMap.put("CREATE_DT+SYSDATE", null);
				culmMap.put("ACADEMY_RAWID", academy_rawid);
				culmMap.put("USER_RAWID", arrUserList[i]);
				culmMap.put("NOW_TOTAL_USE", now_total_use);
				culmMap.put("NOW_USE", now_use_list);
				culmInsertList.add(culmMap);
				updateDataList.add(arrUserList[i]); //조건절
				culmUpdateDataList.add(updateDataList);				
				
			}
			
			if(remainCount < totalUseCoupon) {
				// 잔여 쿠폰 부족
				das3.rollback(conn);
				return -2;
			}
			
			// 사용 내역 관련 INSERT 및 UPDATE
			das3.batchInsert(conn, "COUPON_MONTHLY_TOTAL_TRX", cmttList);
			das3.batchInsert(conn, "COUPON_USE_TRX", cutList);
			das3.batchDelete(conn, "COUPON_USE_LAST_MST", "WHERE USER_RAWID = ?", culmUpdateDataList);
			das3.batchInsert(conn, "COUPON_USE_LAST_MST", culmInsertList);
			das3.batchDelete(conn, "USER_COUPON_TRX", "WHERE USER_RAWID = ? AND TO_DATE(APPLY_DAY,'YYYY-MM-DD') >= TO_CHAR(SYSDATE,'YYYY-MM-DD') AND COUPON_TYPE = ?", uctDeleteDataList);
			das3.batchInsert(conn, "USER_COUPON_TRX", uctInsertList);
			das3.batchInsert(conn, "USER_COUPON_MST", ucmInsertList);
			das3.batchUpdate(conn, "USER_COUPON_MST", ucmUpdateList, "WHERE RAWID = ?", ucmUpdateDataList);
			das3.batchInsert(conn, "USER_COUPON_HST", uchInsertList);
			// 사용 내역 관련 INSERT 및 UPDATE
			
			/*
			 * COUPON_HST INSERT
			 */
			LinkedHashMap<Object, Object> chMap = new LinkedHashMap<Object, Object>();
			Function funcApplyDt = new Function();
			funcApplyDt.columnName = "APPLY_DT";
			funcApplyDt.sentence = "TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS')";
			funcApplyDt.funcList = new LinkedList<String>();
			funcApplyDt.funcList.add(create_dt);
			
			String spend_status = "";
			
			if(st.getCurrentDate() > 4 && couponChk.checkAcademyBillYn(academy_rawid))
				spend_status = "NU";
			else if(st.getCurrentDate() <= 4 && couponChk.checkAcademyBillYn(academy_rawid))
				spend_status = "CM";
			else if(!couponChk.checkAcademyBillYn(academy_rawid))
				spend_status = "FU";
			
			chMap.put("RAWID+SEQ_COUPON_HST.NEXTVAL", null);
			chMap.put(funcApplyDt, null);
			chMap.put("ACADEMY_RAWID", academy_rawid);
			chMap.put("USE_TYPE_CD", "D");
			chMap.put("CREATE_TYPE_CD", "W");
			chMap.put("APPLY_USER_CNT", arrUserList.length);
			chMap.put("NON_APPLY_USER_CNT", "");
			chMap.put("APPLY_COUPON_CNT", totalUseCoupon);
			chMap.put("REMAIN_COUPON_CNT", remainCount - totalUseCoupon);
			chMap.put("COUPON_TYPE", AppConfig.getInstance().getProperty("coupon.coupontypecd"));
			chMap.put("SPEND_STATUS_CD", spend_status);
			if(totalUseCoupon > 0)			// 쿠폰 소비가 발생할 경우 저장
				das3.insert(conn, "COUPON_HST", chMap);
			// COUPON_HST INSERT
			
			// 사용 쿠폰 차감
			LinkedList<Object> acmUpdateDataList = new LinkedList<Object>();
			LinkedHashMap<Object, Object> acmUpdateList = new LinkedHashMap<Object, Object>();
			Function funcAcmUpdate = new Function();
			funcAcmUpdate.columnName = "REMAIN_COUPON_COUNT";
			funcAcmUpdate.sentence = "REMAIN_COUPON_COUNT - ?";
			funcAcmUpdate.funcList = new LinkedList<Object>();
			funcAcmUpdate.funcList.add(totalUseCoupon);
			acmUpdateDataList.add(academy_rawid);
			acmUpdateDataList.add(AppConfig.getInstance().getProperty("coupon.coupontypecd"));
			acmUpdateList.put(funcAcmUpdate, null);
			das3.update(conn, "ACADEMY_COUPON_MST", acmUpdateList, "WHERE ACADEMY_RAWID = ? AND COUPON_TYPE = ?", acmUpdateDataList);
			// 사용 쿠폰 차감
			
			das3.commit(conn);
			iResult = 1;
		} catch (Exception e) {
			iResult = 0;
			das3.rollback(conn);
			logger.error(e);
		}
		
		return iResult;
	}
	
	public String getUserAccumulateCompanyList(String academy_rawid, String user_rawid, String book_list, String period_list) {
		// 학생 이번달 월 누적 사용 리스트
		LinkedList<String> dataList = new LinkedList<String>();
		LinkedList<Object> rowset = new LinkedList<Object>();
		String sql = "";
		String returnVal = "";
		
		try {
			String new_book_list = "0";
			
			if(!book_list.equals("")) {
				String arrPeriodList[] = period_list.split(",");
				String arrBookList[] = book_list.split(",");
				ServerTime st = new ServerTime();
				String now_dt = st.getCurrentPatternDate("yyyy-MM-dd");
				
				for(int i = 0; i < arrPeriodList.length; i++) {
					if(CommonUtil.compareDate(now_dt, arrPeriodList[i].split("~")[0]) == 0) {
						new_book_list += "," + arrBookList[i];
					}
				}
			}
				
			sql = "SELECT PM.NAME " +
			"    FROM COUPON_MONTHLY_TOTAL_TRX CM " +
			"    INNER JOIN PARTNER_MST PM ON PM.COMPANY_CD = CM.COMPANY_CD AND PM.SELECT_YN = ? " +
			"    WHERE CM.ACADEMY_RAWID = ? AND CM.USER_RAWID = ? AND CM.CHECK_MONTH = TO_CHAR(SYSDATE, 'YYYYMM') ";
			dataList.add("Y");
			dataList.add(academy_rawid);
			dataList.add(user_rawid);
			
			if(!book_list.equals("")) {
				sql = sql.concat(
				"    UNION " +
				"    SELECT PM.NAME " +
				"    FROM BOOK_MST BM " +
				"    INNER JOIN PARTNER_MST PM ON PM.COMPANY_CD = BM.COMPANY_CD AND PM.SELECT_YN = ? " +
				"    WHERE BM.RAWID IN (" + new_book_list + ") "
				);
				dataList.add("Y");
			} else {
				sql = sql.concat(
				"    UNION " +
				"    SELECT PM.NAME " +
				"    FROM USER_BOOK_LINK_MST UB " +
				"	 INNER JOIN BOOK_MST BM ON BM.RAWID = UB.BOOK_RAWID " +
				"    INNER JOIN PARTNER_MST PM ON PM.COMPANY_CD = BM.COMPANY_CD AND PM.SELECT_YN = ? " +
				"    WHERE UB.ACADEMY_RAWID = ? AND UB.USER_RAWID = ? AND UB.START_DT <= TO_CHAR(SYSDATE, 'YYYY-MM-DD') AND " +
				"	 UB.END_DT >= TO_CHAR(SYSDATE, 'YYYY-MM-DD') "
				);
				dataList.add("Y");
				dataList.add(academy_rawid);
				dataList.add(user_rawid);
			}
			rowset = das3.select(null, sql, dataList);
			
			for(int i = 0; i < rowset.size(); i++) {
				LinkedHashMap<Object, Object> temp = (LinkedHashMap<Object, Object>) rowset.get(i);
				
				returnVal += "," + temp.get("name");
			}
			
			if(!returnVal.equals("")) returnVal = returnVal.substring(1);
		} catch (Exception e) {
			logger.error(e);
		}
		
		return returnVal;
	}
	
	public String getUserNowUseCompanyList(String user_rawid, String academy_rawid) {
		// 학생이 현재 사용중인 출판사 리스트
		String returnVal = "";
		LinkedList<String> dataList = new LinkedList<String>();
		LinkedList<Object> rowset = null;
		String sql = "";
		
		try {
			sql = "SELECT PM.NAME FROM USER_BOOK_LINK_MST UB " +
			"INNER JOIN BOOK_MST BM ON BM.RAWID = UB.BOOK_RAWID " +
			"INNER JOIN PARTNER_MST PM ON PM.COMPANY_CD = BM.COMPANY_CD AND PM.SELECT_YN = ? " +
			"WHERE UB.ACADEMY_RAWID = ? AND UB.USER_RAWID = ? AND UB.START_DT <= TO_CHAR(SYSDATE, 'YYYY-MM-DD') AND " +
			"UB.END_DT >= TO_CHAR(SYSDATE, 'YYYY-MM-DD') " +
			"GROUP BY PM.NAME " +
			"ORDER BY PM.NAME ";
			dataList.add("Y");
			dataList.add(academy_rawid);
			dataList.add(user_rawid);
			rowset = das3.select(null, sql, dataList);
			
			for(int i = 0; i < rowset.size(); i++) {
				LinkedHashMap<Object, Object> temp = (LinkedHashMap<Object, Object>) rowset.get(i);
				returnVal += "," + temp.get("name");
			}
			
			if(!returnVal.equals("")) returnVal = returnVal.substring(1);
		} catch (Exception e) {
			logger.error(e);
		}
		
		return returnVal;
	}
	
	public int stopCourses(String academy_rawid, String user_type, String user_list, String apply_dt, String coupon_type) {
		/*
		 * 수강 중지
		 * 1. 사용 대기 중인 경우 쿠폰 반환 필요
		 */
		int iResult = 0;
		String arrUserList[] = user_list.split(",");
		ServerTime st = new ServerTime();
		Connection conn = null;
		LinkedList<String> dataList = new LinkedList<String>();
		LinkedList<Object> rowset = null;
		String sql = "";
		String now_dt = st.getCurrentPatternDate("yyyy-MM-dd");
		SQLCouponCheck couponChk = new SQLCouponCheck();
		
		try {
			conn = das3.beginTrans();
			
			LinkedList<LinkedHashMap<Object, Object>> ucmUpdateList = new LinkedList<LinkedHashMap<Object,Object>>();
			LinkedList<LinkedList<String>> ucmUpdateDataList = new LinkedList<LinkedList<String>>();
			LinkedList<LinkedList<String>> uctDeleteDataList = new LinkedList<LinkedList<String>>();
			LinkedList<LinkedHashMap<Object, Object>> uchInsertList = new LinkedList<LinkedHashMap<Object,Object>>();
			LinkedList<LinkedList<Object>> cutDeleteDataList = new LinkedList<LinkedList<Object>>();
			
			int refundCount = 0;
			
			/*
			 * CREATE_DT functuon
			 */
			String create_dt = ((LinkedHashMap<Object, Object>) das3.select(null, "SELECT TO_CHAR(SYSDATE, 'YYYY-MM-DD HH24:MI:SS') NOW_DT FROM DUAL", null).get(0)).get("now_dt").toString();
			Function fn_now_dt = new Function();
			fn_now_dt.columnName = "CREATE_DT";
			fn_now_dt.sentence = "TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS')";
			fn_now_dt.funcList = new LinkedList<Object>();
			fn_now_dt.funcList.add(create_dt);
			
			for(String tempUser : arrUserList) {
				dataList.clear();
				sql = "SELECT RAWID, TO_CHAR(CP_APPLY_DT, 'YYYY-MM-DD') CP_APPLY_DT, CP_APPLY_YN, " +
				"TO_CHAR(CREATE_DT, 'YYYY-MM-DD HH24:MI:SS') CREATE_DT, " +
				"CASE " +
				"	WHEN CP_APPLY_DT > TO_CHAR(SYSDATE, 'YYYY-MM-DD') THEN 'Y' " +
				"	ELSE 'N' " +
				"END RESERVE_YN " +
				"FROM USER_COUPON_MST " +
				"WHERE ACADEMY_RAWID = ? AND USER_RAWID = ? AND COUPON_TYPE = ?";
				dataList.add(academy_rawid);
				dataList.add(tempUser);
				dataList.add(coupon_type);
				rowset = das3.select(null, sql, dataList);
				
				for(int i = 0; i < rowset.size(); i++) {
					LinkedHashMap<Object, Object> temp = (LinkedHashMap<Object, Object>) rowset.get(i);
					
					if(temp.get("cp_apply_yn").equals("Y")) {
						LinkedHashMap<Object, Object> ucmUpdateMap = new LinkedHashMap<Object, Object>();
						LinkedList<String> ucmList = new LinkedList<String>();
						LinkedHashMap<Object, Object> uchInsertMap = new LinkedHashMap<Object, Object>();
						
						// USER_COUPON_MST UPDATE RAWID
						ucmList.add(temp.get("rawid").toString());
						ucmUpdateDataList.add(ucmList);
						
						// USER_COUPON_HST INSERT
						uchInsertMap.put("RAWID+SEQ_USER_COUPON_HST.NEXTVAL", null);
						uchInsertMap.put("ACADEMY_RAWID", academy_rawid);
						uchInsertMap.put("USER_RAWID", tempUser);
						uchInsertMap.put("PRE_CP_APPLY_YN", "Y");
						uchInsertMap.put("PRE_CP_APPLY_DT", temp.get("cp_apply_dt"));
						uchInsertMap.put(fn_now_dt, null);
						uchInsertMap.put("CREATE_BY", academy_rawid);
						uchInsertMap.put("CREATE_TYPE_CD", user_type);
						uchInsertMap.put("COUPON_TYPE", coupon_type);
						uchInsertMap.put("TO_CP_APPLY_YN", "N");
						uchInsertList.add(uchInsertMap);
						
						if(temp.get("reserve_yn").equals("Y")) {
							/*
							 * 사용대기 중. 7장 되돌려 줌
							 * USER_COUPON_TRX 삭제
							 */
							LinkedList<String> uctDeleteList = new LinkedList<String>();
							LinkedList<Object> cutDeleteList = new LinkedList<Object>();
							uctDeleteList.add(tempUser);
							uctDeleteList.add(temp.get("create_dt").toString());
							uctDeleteList.add(coupon_type);
							uctDeleteDataList.add(uctDeleteList);
							
							if(couponChk.checkAcademyBillYn(academy_rawid) && !couponChk.checkUserTutorYn(tempUser))
								// 무료사용 기간이 아니면서 무료사용 학생이 아닌 경우 환불 대상
								refundCount++;
							
							// USER_COUPON_MST UPDATE
							ucmUpdateMap.put("CP_APPLY_YN", "N");
							ucmUpdateMap.put("CP_APPLY_DT", "");
							ucmUpdateMap.put("CREATE_DT", "");
							ucmUpdateMap.put("WORKED_BY", "S");
							ucmUpdateList.add(ucmUpdateMap);
							
							// COUPONT_USE_TRX DELETE DATA LIST
							cutDeleteList.add(tempUser);
							cutDeleteList.add(temp.get("create_dt"));
							cutDeleteDataList.add(cutDeleteList);
						} else {
							// USER_COUPON_MST UPDATE
							ucmUpdateMap.put("CP_APPLY_YN", "N");
							ucmUpdateMap.put("CP_APPLY_DT", apply_dt);
							//ucmUpdateMap.put(fn_now_dt, null);
							ucmUpdateMap.put("CREATE_DT", "");
							ucmUpdateMap.put("WORKED_BY", "S");
							ucmUpdateList.add(ucmUpdateMap);
						}
					}
				}
			}
			
			das3.batchUpdate(conn, "USER_COUPON_MST", ucmUpdateList, "WHERE RAWID = ?", ucmUpdateDataList);
			das3.batchDelete(conn, "USER_COUPON_TRX", "WHERE USER_RAWID = ? AND HST_DT = TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS') AND COUPON_TYPE = ?", uctDeleteDataList);
			das3.batchInsert(conn, "USER_COUPON_HST", uchInsertList);
			das3.batchDelete(conn, "COUPON_USE_TRX", "WHERE USER_RAWID = ? AND CREATE_DT = TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS') ", cutDeleteDataList);
			
			if(refundCount > 0) {
				LinkedHashMap<Object, Object> chInsertMap = new LinkedHashMap<Object, Object>();
				LinkedHashMap<Object, Object> acmUpdateMap = new LinkedHashMap<Object, Object>();
				LinkedList<Object> acmUpdateDataList = new LinkedList<Object>();
				
				// COUPON_HST INSERT
				/*
				 * COUPON_HST 에는 APPLY_DT = CREATE_DT 이므로 CREATE_DT 컬럼명을 APPLY_DT로 수정
				 */
				fn_now_dt.columnName = "APPLY_DT";
				chInsertMap.put("RAWID+SEQ_COUPON_HST.NEXTVAL", null);
				chInsertMap.put(fn_now_dt, null);
				chInsertMap.put("ACADEMY_RAWID", academy_rawid);
				chInsertMap.put("USE_TYPE_CD", "R");
				chInsertMap.put("CREATE_TYPE_CD", "W");
				chInsertMap.put("APPLY_USER_CNT", refundCount);
				chInsertMap.put("NON_APPLY_USER_CNT", "0");
				chInsertMap.put("APPLY_COUPON_CNT", refundCount * 7);
				chInsertMap.put("REMAIN_COUPON_CNT", couponChk.getRemainCouponCount(academy_rawid) + (refundCount * 7));
				chInsertMap.put("COUPON_TYPE", coupon_type);
				das3.insert(conn, "COUPON_HST", chInsertMap);
				
				// ACADEMY_COUPON_MST UPDATE
				Function funcRemainCount = new Function();
				funcRemainCount.columnName = "REMAIN_COUPON_COUNT";
				funcRemainCount.sentence = "REMAIN_COUPON_COUNT + ? * 7";
				funcRemainCount.funcList = new LinkedList<Object>();
				funcRemainCount.funcList.add(refundCount);
				
				acmUpdateMap.put(funcRemainCount, null);
				acmUpdateDataList.add(academy_rawid);
				acmUpdateDataList.add(coupon_type);
				das3.update(conn, "ACADEMY_COUPON_MST", acmUpdateMap, "WHERE ACADEMY_RAWID = ? AND COUPON_TYPE = ?", acmUpdateDataList);
			}
			
			das3.commit(conn);
			iResult = 1;
		} catch (Exception e) {
			iResult = 0;
			das3.rollback(conn);
			logger.error(e);
		}
		
		return iResult;
	}
	
	public boolean checkUserTutorYn(String user_rawid) {
		boolean returnVal = false;
		LinkedList dataList = new LinkedList();
		try {
			das3 = new DAS3(AppConfig.getInstance().getDBName());

			String sql = "SELECT TUTOR_YN FROM USER_MST WHERE RAWID = ?";
			dataList.add(user_rawid);
			LinkedList rowset = das3.select(null, sql, dataList);

			if (rowset.size() > 0) {
				LinkedHashMap temp = (LinkedHashMap) rowset.get(0);
				if (temp.get("tutor_yn").equals("Y"))
					returnVal = true;
			}
		} catch (Exception e) {
			this.logger.error(e);
		}

		return returnVal;
	}
	
	public int startCoursesCash(String academy_rawid, String user_type, String user_list, String start_dt) {
		int iResult = 0,addCompanyCash = 0;
		ServerTime st = new ServerTime();
		Connection conn = null;
		String now_dt = st.getCurrentPatternDate("yyyy-MM-dd");
		String sql = "",cash_use_type_cd = "MCU";
		 
		
		SQLCouponCheck cashChk = new SQLCouponCheck();
		LinkedHashMap<String, String> userInfo = new LinkedHashMap<String, String>();
		
		String book_unit_price = "";
		String addCompany = "";
		
		HashMap<String, String> hm = new HashMap<String, String>();
		try {
			String deduction_unit_price = selectKey("tot_price","SELECT NVL(TOT_PRICE,0) TOT_PRICE FROM ACADEMY_CASH_PRODUCT_MST WHERE ACADEMY_RAWID = '"+academy_rawid+"' AND USE_YN='Y' AND CASH_TYPE_CD='MC'"); //학원월가격
			LinkedList<String> dataList = new LinkedList<String>();
			LinkedList<Object> rowset = null;
			LinkedList<LinkedHashMap<Object, Object>> cmttList = new LinkedList<LinkedHashMap<Object,Object>>();
			LinkedList<LinkedHashMap<Object, Object>> cutList = new LinkedList<LinkedHashMap<Object,Object>>();			 
			LinkedList<LinkedHashMap<Object, Object>> chList = new LinkedList<LinkedHashMap<Object, Object>>();
			LinkedList<LinkedHashMap<Object, Object>> stList = new LinkedList<LinkedHashMap<Object, Object>>();
			LinkedList<LinkedHashMap<Object, Object>> ummList = new LinkedList<LinkedHashMap<Object, Object>>();
			LinkedList<LinkedHashMap<Object, Object>> ummUpdateDataList = new LinkedList<LinkedHashMap<Object, Object>>();
			LinkedList<LinkedList<Object>> delList = new LinkedList<LinkedList<Object>>();			
			LinkedList<LinkedList<String>> UpdateList = new LinkedList<LinkedList<String>>();
			
			
			conn = das3.beginTrans();
			String arrUserList[] = user_list.split(",");
			ArrayList<String> insertCheck = new ArrayList<String>();
			String work_type_cd = "";
			String academy_cash_hst_sequnce = this.getTableSequence(dataList,"seq_academy_cash_hst");
			
			if(CommonUtil.compareDate(now_dt,userInfo.get("start_dt") ) == 0) 
				work_type_cd = "S";
			else 
				work_type_cd = "RV";
			
			// CREATE_DT를 키값으로 통일 시키기 위한 값
			String create_dt = 
				((LinkedHashMap<Object, Object>) das3.select(null, "SELECT TO_CHAR(SYSDATE, 'YYYY-MM-DD HH24:MI:SS') CREATE_DT FROM DUAL", null).get(0)).get("create_dt").toString();
			
			Function funcCreateDt = new Function();
			funcCreateDt.columnName = "CREATE_DT";
			funcCreateDt.sentence = "TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS')";
			funcCreateDt.funcList = new LinkedList<Object>();
			funcCreateDt.funcList.add(create_dt);
			
			Function funcHstDt = new Function();
			funcHstDt.columnName = "HST_DT";
			funcHstDt.sentence = "TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS')";
			funcHstDt.funcList = new LinkedList<Object>();
			funcHstDt.funcList.add(create_dt);
			
			String last_day = 
				((LinkedHashMap<Object, Object>) das3.select(null, "SELECT TO_CHAR(last_day(sysdate),'YYYY-MM-DD HH24:MI:SS') last_day from dual", null).get(0)).get("last_day").toString();
			 			
			Function funcEndDt = new Function();
			funcEndDt.columnName = "END_DT";
			funcEndDt.sentence = "TO_DATE(?, 'YYYY-MM-DD HH24:MI:SS')";
			funcEndDt.funcList = new LinkedList<Object>();
			funcEndDt.funcList.add(last_day);

			//잔여캐시 확인
			int remainCount = cashChk.getRemainCashCount(academy_rawid,"remain");
			int freeCash = cashChk.getRemainCashCount(academy_rawid,"freecash");
			int calRemainSum = remainCount + freeCash; //유료,무료 합계
			
			int deductionRemainCash = 0,userFreeCashAmount = 0,
			haveFreeCash = freeCash ,tempFreeCash = freeCash,
			totalDeductionCash = Integer.parseInt(userInfo.get("deduction_total_price"),10);
			
			boolean user_month_mst_insert_check = false; 
			dataList.clear();
			
			int academyFreeCash = 0; //학원 마일리지캐시 차감 합계
			int academyCash = 0; //유료캐시 차감 합계
			
			for(int i = 0; i < arrUserList.length; i++) {
				int calApplyCash = 0; //수강신청 실제 차감금액(마일리지캐시 확인후 계산된 금액)
				int applyCash = Integer.parseInt(deduction_unit_price,10); //수강신청 차감금액
				int  overCount = 0, company_over_count = 0;
				boolean checkCompanyDeduct = false;
				
//				if(st.getCurrentDate() > 4 || (!cashChk.checkAcademyCashBillYn(academy_rawid) && AppConfig.getInstance().getProperty("custom.organizationtypecd").equals("SR"))){
//					// 무료 사용기간도 아니고 커리 조정 기간도 아니며 무료 사용 학생도 아니면
//					String temp_overCount = jsonReq != null ? ((JSONObject) jsonReq.get(arrUserList[i])).get("overCount").toString() : req.getParameter(arrUserList[i]+"[overCount]");
//					company_over_count = Integer.parseInt(temp_overCount);
//					overCount = company_over_count  * Integer.parseInt(userInfo.get("book_unit_price"));
//					addCompanyCash+=overCount;
//				}
				
				int tot_price = applyCash + overCount; //수강신청+추가출판사 차감합계
				//마일리지 캐시 확인
				if(freeCash>0){
					if(freeCash >= tot_price){
						//마일리지 캐시 사용
						calApplyCash = 0;
						freeCash = freeCash-tot_price;  //학원 마일리지 차감 잔액
						academyFreeCash = academyFreeCash + tot_price;
					}else{
						//마일리지 + 유료캐시 사용
						calApplyCash = tot_price-freeCash;
						academyCash = academyCash + calApplyCash; 
						academyFreeCash = academyFreeCash + freeCash;
						remainCount = remainCount-calApplyCash;
						freeCash = 0;  //학원 마일리지 차감 잔액
					}
				}else{
					//유료캐시 사용
					calApplyCash = tot_price;
					remainCount = remainCount-tot_price;  //차감잔액
					academyCash = academyCash + tot_price; //차감캐시
				}
				
				String prev_total_use_list = "", prev_now_use_list = "";
				String user_month_trx_sequnce = this.getTableSequence(dataList,"seq_user_month_trx");
				
				int tempApplyCash = 0;
				
				//==================================================
				// USER_MONTH_MST 테이블 이전 기록 유무 체크
				// 리턴 값이 TRUE UPDATE ,FLASE INSERT 함	
				//==================================================
				user_month_mst_insert_check = this.userMonthMstInsertCheck(dataList,arrUserList[i]);
				LinkedHashMap<Object, Object> ummMap = new LinkedHashMap<Object, Object>();
				if(user_month_mst_insert_check){
					ummMap.put("RAWID+SEQ_USER_MONTH_MST.NEXTVAL", null);
					ummMap.put("ACADEMY_RAWID", userInfo.get("academy_rawid"));
					ummMap.put("user_rawid", arrUserList[i]);
					ummMap.put("user_month_trx_rawid", user_month_trx_sequnce);
					ummMap.put("create_dt", userInfo.get("now_dt"));
					ummMap.put("modify_user_type",userInfo.get("user_type"));
					ummMap.put("modify_user_rawid",userInfo.get("user_rawid"));
					ummMap.put("worked_by","S");
					ummList.add(ummMap);
					insertCheck.add("Y");
				}else{																				
					LinkedList<String> ummUpdateMap = new LinkedList<String>();
					ummMap.put("USER_MONTH_TRX_RAWID", user_month_trx_sequnce); 
					ummMap.put("MODIFY_USER_TYPE", userInfo.get("user_type"));
					ummMap.put("MODIFY_USER_RAWID", userInfo.get("user_rawid"));
					ummMap.put("CREATE_DT", userInfo.get("now_dt"));
					ummUpdateDataList.add(ummMap);
					
					ummUpdateMap.add(arrUserList[i] );
					UpdateList.add(ummUpdateMap);
					insertCheck.add("N");
					
				}
				
				//1. 학생 수강변경 이력 USER_MONTH_TRX
				//마일리지캐시차감 금액은 0원 처리함
				LinkedHashMap<Object, Object> stMap = new LinkedHashMap<Object, Object>();
				String user_status = this.getCashUserSatus(dataList, arrUserList[i]);
				stMap.put("RAWID", user_month_trx_sequnce);
				stMap.put("ACADEMY_RAWID", academy_rawid);
				stMap.put("USER_RAWID", arrUserList[i]);
				stMap.put("FROM_USE_STATUS_CD", user_status);
				stMap.put("TO_USE_STATUS_CD", "01");
				stMap.put("CREATE_DT+SYSDATE", "");
				stMap.put("MODIFY_USER_TYPE", user_type);
				stMap.put("MODIFY_USER_RAWID", academy_rawid);
				stMap.put("APPLY_DT", start_dt);
				stMap.put("APPLY_CASH", calApplyCash);				
				stMap.put("STOP_DT",null);  				 
				stList.add(stMap);
				
				/*
				 * USER_BOOK_LINK_MST에서 학생키에 세팅된 도서들 중
				 * 사용 시작된 도서들의 출판사 정리
				 * 현재 달에 이미 사용을 한 도서는 제외
				 */
				rowset = this.getUserLinkBook(userInfo,arrUserList,i);
				
				if(work_type_cd.equals("S")) {
					Function f_check_month = new Function();
					f_check_month.columnName = "CHECK_MONTH";
					f_check_month.sentence = "TO_CHAR(SYSDATE, 'YYYYMM')";
					
					for(int j = 0; j < rowset.size(); j++) {
						/*
						 * 새로 시작되는 출판사 COUPON_MONTHLY_TOTAL_TRX INSERT
						 */
						LinkedHashMap<Object, Object> temp = (LinkedHashMap<Object, Object>) rowset.get(j);
						LinkedHashMap<Object, Object> cmttMap = new LinkedHashMap<Object, Object>();
						
						
						cmttMap.put("RAWID+SEQ_COUPON_MONTHLY_TOTAL_TRX.NEXTVAL", null);
						cmttMap.put("ACADEMY_RAWID", userInfo.get("academy_rawid"));
						cmttMap.put("USER_RAWID", arrUserList[i]);
						cmttMap.put(f_check_month, null);
						cmttMap.put("COMPANY_CD", temp.get("company_cd"));
						cmttMap.put(funcCreateDt, null);
						cmttList.add(cmttMap);
						// 새로 시작되는 출판사 COUPON_MONTHLY_TOTAL_TRX INSERT
					}
				}
				
				// 이전 사용 내역 처리
				dataList.clear();
				sql = " SELECT USER_rawid, NOW_TOTAL_USE, NOW_USE FROM ( \n" +
				" 			SELECT user_rawid, NOW_TOTAL_USE, NOW_USE  \n" +
				" 				FROM USER_MONTH_CASH_HST WHERE CREATE_DT >=  TO_CHAR(SYSDATE,'YYYY-MM-')||'01' \n" +
				" 				AND ACADEMY_RAWID = ? AND END_DT > SYSDATE AND USER_RAWID = ? \n" +
				" 				ORDER BY CREATE_DT DESC  \n" +				
				" 				) WHERE ROWNUM = 1  \n" ;
				dataList.add(userInfo.get("academy_rawid"));
				dataList.add(arrUserList[i]);
				rowset = das3.select(null, sql, dataList);
				
				if(rowset.size() > 0) {
					LinkedHashMap<Object, Object> temp = (LinkedHashMap<Object, Object>) rowset.get(0);
					prev_now_use_list = temp.get("now_use").toString();
					prev_total_use_list = temp.get("now_total_use").toString();
				}
				 
				//2. 학생월캐시이력 USER_MONTH_CASH_HST
				LinkedHashMap<Object, Object> cutMap = new LinkedHashMap<Object, Object>();
				cutMap.put("RAWID+SEQ_USER_MONTH_CASH_HST.NEXTVAL", null);
				cutMap.put("ACADEMY_CASH_HST_RAWID", academy_cash_hst_sequnce);				 
				cutMap.put("ACADEMY_RAWID", academy_rawid);
				cutMap.put("USER_RAWID", arrUserList[i]);
				cutMap.put("PREV_TOTAL_USE", prev_total_use_list);
				cutMap.put("NOW_TOTAL_USE", !work_type_cd.equals("S") ? "" : getUserAccumulateCompanyList(academy_rawid, arrUserList[i], "", ""));
				cutMap.put("PREV_USE", prev_now_use_list);
				cutMap.put("NOW_USE", !work_type_cd.equals("S") ? "" : getUserNowUseCompanyList(arrUserList[i], academy_rawid));
				cutMap.put("WORKED_BY", "S");
				cutMap.put("APPLY_CASH",applyCash) ;
				cutMap.put("COMPANY_COUPON_COUNT",company_over_count) ;				 
				cutMap.put("REFUND_CASH", 0);
				cutMap.put("START_DT",start_dt); 
				cutMap.put(funcEndDt,null);				 
				cutMap.put("REFUND_RATE", 0);
				cutMap.put("CREATE_BY", academy_rawid);
				cutMap.put("CREATE_DT+SYSDATE", null);		
				cutMap.put("ADD_USE", hm.get(arrUserList[i]));
				cutList.add(cutMap);
			}//end for - user
			
			//3. 학원캐시이력 ACADEMY_CASH_HST
			if(academyCash>0&&academyFreeCash>0){
				cash_use_type_cd = "MMR";	//유료+마일리지
			}else if(academyFreeCash>0){
				cash_use_type_cd = "MFR";	//마일리지
			}else{
				cash_use_type_cd = "MCU";	//유료
			}
			
			if(calRemainSum<(academyCash+academyFreeCash)) {
				// 잔여 쿠폰 부족
				das3.rollback(conn);
				return -2;
			}
			
			calRemainSum = calRemainSum - academyCash - academyFreeCash; //마일리지+유료 합계에서 잔액구하기
			
			LinkedHashMap<Object, Object> chMap = new LinkedHashMap<Object, Object>();
			chMap.put("RAWID", academy_cash_hst_sequnce);
			chMap.put("BUSINESS_TYPE", AppConfig.getInstance().getProperty("scall.callclasstype"));
			chMap.put("ACADEMY_RAWID", academy_rawid);
			chMap.put("CREATE_BY", user_type);
			chMap.put("USER_RAWID", academy_rawid);
			chMap.put("CASH_USE_TYPE_CD", cash_use_type_cd);
			chMap.put("USE_CASH",academyCash);
			chMap.put(funcCreateDt, null);
			chMap.put("REMAIN_CASH", calRemainSum);
			chMap.put("SCALL_PAY_TRX_RAWID", 0);
			chMap.put("USE_SAMPLE_CLASS_COUNT", 0);
			chMap.put("REMAIN_SAMPLE_CLASS_COUNT", 0);
			chMap.put("CREATE_TYPE_CD", "W");
			chMap.put("APPLY_USER_CNT", arrUserList.length);
			chMap.put("NON_APPLY_USER_CNT", 0);
			chMap.put("CASH_TYPE_CD", "MC");
			chMap.put("FREE_CASH",academyFreeCash);
			chList.add(chMap);
			 
			// 사용 내역 관련 INSERT 및 UPDATE
			das3.batchInsert(conn, "COUPON_MONTHLY_TOTAL_TRX", cmttList);
			das3.batchInsert(conn, "USER_MONTH_TRX", stList);
			das3.batchInsert(conn, "USER_MONTH_CASH_HST", cutList); 
			das3.batchInsert(conn, "ACADEMY_CASH_HST", chList);
			
			boolean userMonthCheck = true,userMonthCheckUpdate = true;
			for(String checkYn : insertCheck){
				if(checkYn.equals("Y") && userMonthCheck){
					userMonthCheck = false;
					 das3.batchInsert(conn, "USER_MONTH_MST", ummList);
				}else if(checkYn.equals("N") && userMonthCheckUpdate){
					userMonthCheckUpdate = false;
					 das3.batchUpdate(conn, "USER_MONTH_MST", ummUpdateDataList,"WHERE USER_RAWID = ?",UpdateList);
				}else if(!userMonthCheck && !userMonthCheckUpdate){
					break;
				}
			} 
			
			//4.학원잔여캐시관리 ACADEMY_CASH_MST
			LinkedHashMap<Object, Object> cashUpdate = new LinkedHashMap<Object, Object>();
			
			cashUpdate.put("REMAIN_CASH", remainCount);
			cashUpdate.put("FREE_CASH", freeCash);
			
			dataList.clear();
			dataList.add(userInfo.get("academy_rawid"));
			dataList.add(AppConfig.getInstance().getProperty("scall.callclasstype"));
			
			iResult = das3.update(conn, "ACADEMY_CASH_MST", cashUpdate, "WHERE  ACADEMY_RAWID = ? AND BUSINESS_TYPE = ? ", dataList);
			das3.commit(conn);
		} catch (Exception e) {
			iResult = 0;
			das3.rollback(conn);
			logger.error(e);
		}
		
		return iResult;
	}
	
	private String getTableSequence(LinkedList<String> dataList,String tableSequences) throws Exception{
		dataList.clear();
		LinkedHashMap<Object, Object> temp1 = new LinkedHashMap<Object, Object>();
		temp1 = (LinkedHashMap<Object, Object>) das3.select(null, "select "+tableSequences+".nextval from dual", dataList).get(0);
		String sequence = temp1.get("nextval").toString();
		return sequence;
	}
	
	private boolean userMonthMstInsertCheck(LinkedList<String> dataList, String userRawid) throws Exception {
		// TODO Auto-generated method stub
		dataList.clear();
		boolean returnValue = true;
		String sql = " select rawid from user_month_mst where user_rawid = ? " ;										 
			dataList.add(userRawid);
			
		LinkedList rowset = new LinkedList();
		rowset =  das3.select(null, sql, dataList);
		if(rowset.size() > 0){
			returnValue = false;
		}
		return returnValue;
	}
	
	private  String getCashUserSatus(LinkedList<String> dataList,String user_rawid) throws Exception{
		dataList.clear();
		String sql = " select umt.to_use_status_cd from user_month_mst umm \n" +
				"   join user_month_trx umt on umm.user_month_trx_rawid = umt.rawid \n " +
				"   where umm.user_rawid =  ? and umt.create_dt >= to_char(sysdate,'YYYY-MM-')||'01' \n" ;
								 
			 dataList.add(user_rawid);
			 String status = "";			 		
			 LinkedList<Object> rowset = null;
			 rowset = das3.select(null, sql, dataList);
			 if(rowset.size() > 0) {
					LinkedHashMap<Object, Object> temp = (LinkedHashMap<Object, Object>) rowset.get(0);
					status = (String)temp.get("to_use_status_cd").toString(); 
			 }
		 
		return status;
	}
	
	private LinkedList<Object> getUserLinkBook(
			LinkedHashMap<String, String> userInfo, String[] arrUserList, int index) {
		// TODO Auto-generated method stub
		LinkedList<String> dataList = new LinkedList<String>();
		LinkedList<Object> rowset = null;
		String sql = "SELECT PM.COMPANY_CD, PM.NAME " +
		"FROM USER_BOOK_LINK_MST UB " +
		"INNER JOIN BOOK_MST BM ON BM.RAWID = UB.BOOK_RAWID AND " +
		"UB.START_DT <= TO_CHAR(SYSDATE, 'YYYY-MM-DD') AND " +
		"UB.END_DT >= TO_CHAR(SYSDATE, 'YYYY-MM-DD') " +
		"INNER JOIN PARTNER_MST PM ON PM.COMPANY_CD = BM.COMPANY_CD " +
		"AND PM.SELECT_YN = ? " +
		"WHERE UB.ACADEMY_RAWID = ? AND UB.USER_RAWID = ? " +
		"AND BM.COMPANY_CD NOT IN ( " +
		"    SELECT COMPANY_CD FROM COUPON_MONTHLY_TOTAL_TRX " +
		"    WHERE USER_RAWID = ? AND CHECK_MONTH = TO_CHAR(SYSDATE, 'YYYYMM') " +
		") " +
		"GROUP BY PM.COMPANY_CD, PM.NAME " +
		"ORDER BY PM.NAME ";
		dataList.clear();
		dataList.add("Y");
		dataList.add(userInfo.get("academy_rawid"));
		dataList.add(arrUserList[index]);
		dataList.add(arrUserList[index]);
		 try {
			rowset = das3.select(null, sql, dataList);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return rowset;
	}
	
	//월과금 다음달 수강중지 설정 처리
	public int setCashCancleReservation(String academy_rawid, String user_type, String user_list, String stop_dt) {
		int iResult = 0;
		String arrUserList[] = user_list.split(",");
		ServerTime st = new ServerTime();
		Connection conn = null;
		LinkedList<String> dataList = new LinkedList<String>();
		LinkedList<Object> rowset = null;
		 
		String now_dt = st.getCurrentPatternDate("yyyy-MM-dd");		 
		
		try {
			conn = das3.beginTrans(); 
			
			LinkedList<LinkedHashMap<Object, Object>> ummUpdateDataList = new LinkedList<LinkedHashMap<Object, Object>>();
			LinkedList<LinkedList<String>> updateList = new LinkedList<LinkedList<String>>();
			LinkedList<LinkedHashMap<Object, Object>> stList = new LinkedList<LinkedHashMap<Object, Object>>();
			LinkedList<LinkedList<Object>> delList = new LinkedList<LinkedList<Object>>();
						
			String create_dt = ((LinkedHashMap<Object, Object>) das3.select(null, "SELECT TO_CHAR(SYSDATE, 'YYYY-MM-DD HH24:MI:SS') NOW_DT FROM DUAL", null).get(0)).get("now_dt").toString();		 					 
			//LinkedHashMap<String, String> userInfo = new LinkedHashMap<String, String>();
			//userInfo = this.setInsertCashInfo(user_type, academy_rawid, user_rawid,  now_dt, userInfo,stop_dt,arrUserList.length,haveCash,create_dt);
			
			for(String tempUser : arrUserList) {
				 /*
				  * 학생의 이전 상태값과 시작일 가져옴				 
				  */
				String user_month_trx_sequnce = this.getTableSequence(dataList,"seq_user_month_trx");
				String academy_cash_hst_sequnce = this.getTableSequence(dataList,"seq_academy_cash_hst");
				 
				LinkedHashMap<Object, Object> ummUpdateDataMap = new LinkedHashMap<Object, Object>();					
				LinkedList<String> ummUpdateMap = new LinkedList<String>();
				
				ummUpdateDataMap.put("USER_MONTH_TRX_RAWID",user_month_trx_sequnce); 				  
				ummUpdateDataList.add(ummUpdateDataMap);
				
				ummUpdateMap.add(tempUser);
				updateList.add(ummUpdateMap);
				
				String sql = " select user_month_trx_rawid,to_char(umt.apply_dt,'yyyy-mm-dd')apply_dt,umt.to_use_status_cd ,umt.apply_cash from user_month_mst umm  " +
				"			join user_month_trx umt on umm.user_month_trx_rawid = umt.rawid " +
				"			where umm.user_rawid = ? AND UMM.WORKED_BY = ? " ;
				dataList.clear();
				dataList.add(tempUser);		 
				dataList.add("W");		 
				
				rowset = das3.select(null, sql, dataList);
				LinkedHashMap<Object, Object> temp = (LinkedHashMap<Object, Object>) rowset.get(0);
				LinkedHashMap<Object, Object> stMap = new LinkedHashMap<Object, Object>();
				stMap.put("RAWID", user_month_trx_sequnce);
				stMap.put("ACADEMY_RAWID", academy_rawid);
				stMap.put("USER_RAWID", tempUser);
				stMap.put("FROM_USE_STATUS_CD", temp.get("to_use_status_cd"));
				stMap.put("TO_USE_STATUS_CD", "07"); //요청으로 수동처리
				stMap.put("CREATE_DT+SYSDATE", "");
				stMap.put("MODIFY_USER_TYPE", user_type);
				stMap.put("MODIFY_USER_RAWID", academy_rawid);
				stMap.put("APPLY_DT", temp.get("apply_dt"));
				stMap.put("APPLY_CASH", temp.get("apply_cash"));
				stMap.put("STOP_DT",stop_dt);
				stList.add(stMap);
			}
			
			das3.batchUpdate(conn, "USER_MONTH_MST", ummUpdateDataList,"WHERE USER_RAWID = ?",updateList);
			das3.batchInsert(conn, "USER_MONTH_TRX", stList); 
			 
			das3.commit(conn);
			iResult = 1;
		} catch (Exception e) {
			iResult = 0;
			das3.rollback(conn);
			logger.error(e);
		}
		
		return iResult;
	}
	
	/**
	 * key 조회
	 * @param academyid
	 * @return
	 * @throws Exception 
	 */
	public String selectKey(String key, String query) throws Exception{
		LinkedList rowset = das3.select(null, query, null);
		
		if (rowset.isEmpty())
			return "";
		else {
			return ((LinkedHashMap)rowset.get(0)).get(key).toString();
		}
	}
}
