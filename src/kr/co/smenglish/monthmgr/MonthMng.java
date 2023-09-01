package kr.co.smenglish.monthmgr;

import java.sql.Connection;
import java.util.LinkedHashMap;
import java.util.LinkedList;


import kr.co.smenglish.das.sql.DAS;
import kr.co.smenglish.das.sql.DAS3;
import kr.co.smenglish.servermgr.Constants;

import org.apache.log4j.Logger;

public class MonthMng {
	static Logger logger = Logger.getLogger(MonthMng.class);
	private DAS3 das3 = new DAS3(Constants.DAS_CONFIG);
	private Connection conn = null;
	private String pay_cd = "M";
	private String Tacademy_rawid = "";  //수동실행시 학원 rawid
	private String Tcell_number = "";
	SqlList sqlList = new SqlList();
	
	//환불
	public void refundCash(String predate, int refundLastDay, String runCd){
		try {
			String result = existRefund(predate,refundLastDay, runCd);
			logger.info("---- refundCash : "+result);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//차감
	public void cashMng(String predate, int refundLastDay, String runCd){
		try {
			String result = runCashMng(predate,refundLastDay, runCd);
			logger.info("---- runCashMng Ok!");
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//수강관리 예약 처리
	public void runBookingMng(String pay_cd){
		try {
			String result = existBooking(pay_cd);
			logger.info("---- runBookingMng : "+result);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	 //출판사과금 처리 2017-08-21 최현일
		public void runOverCountCompany(){
			try {
				overCountCompany();
				logger.info("---- runOverCountCompany Ok!");
				
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	//수강관리 예약 실행
	private String existBooking(String pay_cd) throws Exception{
		String result = "";
		LinkedList rowset = null;
		LinkedList dataList = new LinkedList();
		StringBuffer query = new StringBuffer(32);
		
		try{
			conn = das3.beginTrans();
			
			query.append("/* use:WEB; method-name:" + this.getClass().getName() + "예약정보 확인 */");
			query.append("\n SELECT * ");
			query.append("\n   FROM ( SELECT RAWID, ACADEMY_RAWID, USER_RAWID,PAY_CD,APPLY_YN,APPLY_DT, ");
			query.append("\n                 CASE WHEN PAY_CD = 'C' THEN TO_CHAR(DECODE(APPLY_YN,'Y',APPLY_DT,APPLY_DT+1),'YYYY-MM-DD') ");
			query.append("\n                      WHEN PAY_CD='M' THEN TO_CHAR(ADD_MONTHS(APPLY_DT,1),'YYYY-MM')||'-01' ");
			query.append("\n                  END RE_APPLY_DT, COUPON_TYPE ");
			query.append("\n            FROM BOOKING_APPLY_TRX )");
			query.append("\n  WHERE PAY_CD = '"+pay_cd+"' and RE_APPLY_DT = TO_CHAR(SYSDATE,'YYYY-MM-DD') ");
			LinkedList<Object> bookingRowset = das3.select(null, query.toString(), null);
			
			for (Object list : bookingRowset){
				LinkedHashMap bookingTemp = ((LinkedHashMap)list);
				String rawid = bookingTemp.get("rawid").toString();
				String academy_rawid  = bookingTemp.get("academy_rawid").toString();
				String user_rawid  = bookingTemp.get("user_rawid").toString(); 
				String apply_yn = bookingTemp.get("apply_yn").toString(); 
				String re_apply_dt = bookingTemp.get("re_apply_dt").toString(); 
				String coupon_type = bookingTemp.get("coupon_type").toString(); 
				int nReturn = 0;
				if(pay_cd.equals("C")){
					//코인
					if(apply_yn.equals("Y")){
						//수강신청 SQLSetCoupon.startCourses() 참고
						nReturn = sqlList.startCourses(academy_rawid, "A", academy_rawid, user_rawid, re_apply_dt,coupon_type);
					}else if(apply_yn.equals("N")){
						//수강중지
						nReturn = sqlList.stopCourses(academy_rawid, "A", user_rawid,re_apply_dt,coupon_type);
					}
				}else if(pay_cd.equals("M")){
					//월결제
					if(apply_yn.equals("Y")){
						//수강신청
						nReturn = sqlList.startCoursesCash(academy_rawid, "A", user_rawid, re_apply_dt); 
					}else if(apply_yn.equals("N")){
						//수강중지
						nReturn = sqlList.setCashCancleReservation(academy_rawid, "A", user_rawid, re_apply_dt);
					}
				}
				
				//코인,캐쉬부족
				if(nReturn==-2){
					query.setLength(0);
					query.append("   INSERT INTO BOOKING_APPLY_HST (RAWID,ACADEMY_RAWID,USER_RAWID,PAY_CD,APPLY_YN,APPLY_DT,CREATE_DT,CREATE_BY,CREATE_TYPE_CD,COUPON_TYPE,INFO) ");
					query.append("   SELECT SEQ_BOOKING_APPLY_HST.NEXTVAL, ACADEMY_RAWID,USER_RAWID,PAY_CD,APPLY_YN,APPLY_DT,CREATE_DT,CREATE_BY,CREATE_TYPE_CD,COUPON_TYPE,'캐쉬부족' FROM BOOKING_APPLY_TRX WHERE RAWID = 15 ");
					dataList.clear();
					dataList.add(rawid);
					das3.execute(conn,query.toString(), dataList);
				}
				
				//예약정보삭제
				if(nReturn==1){
					query.setLength(0);
					query.append("   DELETE BOOKING_APPLY_TRX WHERE RAWID = ? ");
					
					dataList.clear();
					dataList.add(rawid);
					das3.execute(conn,query.toString(), dataList);
				}
			}//end for
			
			das3.commit(conn);
			result = bookingRowset.size()+"건 OK!";
		} catch (Exception e) {
			logger.error(e);
			if (das3 != null) try { das3.rollback(this.conn); } catch(Exception ex) {}
		} finally{
			if (das3 != null) try { das3.commit(this.conn); } catch(Exception ex) {}
		}
		
		return result;
	}
	
	//환불처리
	private String existRefund(String predate, int refundLastDay, String runYn) throws Exception{
		String result = "";
		LinkedList rowset = null;
		LinkedList dataList = new LinkedList();
		StringBuffer query = new StringBuffer(32);
		
		LinkedList<Object> academyRowset = existAcademy();
		
		try{
			conn = das3.beginTrans();
			for (Object list : academyRowset){
				LinkedHashMap academyTemp = ((LinkedHashMap)list);
				
				String academy_rawid = academyTemp.get("rawid").toString();
				String org_cd = academyTemp.get("organization_type_cd").toString(); 
				if(org_cd.equals("SR")) org_cd = "YS";
				
				int academyRefund = 0; //예상환불캐시
				
				query.setLength(0);
				query.append("/* use:WEB; method-name:" + this.getClass().getName() + "중지상태 사용일계산 */");
				query.append("\n SELECT UT.ACADEMY_RAWID,UT.USER_RAWID,UT.FROM_USE_STATUS_CD,UT.TO_USE_STATUS_CD,TO_CHAR(UT.APPLY_DT,'YYYY-MM-DD') APPLY_DT,TO_CHAR(UT.STOP_DT,'YYYY-MM-DD') STOP_DT, ");
				query.append("\n       NVL((TO_DATE(STOP_DT)-TO_DATE(APPLY_DT))+1 ,0) USE_COUNT, APPLY_CASH,");
				
				if(runYn.equals("Y")){
					query.append("\n   TO_DATE(TO_CHAR((LAST_DAY(ADD_MONTHS(SYSDATE,-1))),'YYYY-MM-DD'))-TO_DATE(APPLY_DT)+1 BASIC_COUNT ");
					
				}else{
					//환불예상
					query.append("\n   TO_DATE(TO_CHAR((LAST_DAY(SYSDATE)),'YYYY-MM-DD'))-TO_DATE(APPLY_DT)+1 BASIC_COUNT ");
				}
				
				query.append("\n   FROM USER_MONTH_TRX UT ");
				query.append("\n  WHERE UT.ACADEMY_RAWID = ? ");
				query.append("\n    AND TO_CHAR(UT.APPLY_DT,'YYYY-MM') = ? ");
				query.append("\n    AND TO_USE_STATUS_CD = '06' ");
				
				dataList.clear();
				dataList.add(academy_rawid);
				dataList.add(predate);
				
				rowset = das3.select(null, query.toString(), dataList);
				
				//학원캐시이력 rawid
				String academy_cash_hst_rawid = selectKey("rawid","SELECT SEQ_ACADEMY_CASH_HST.NEXTVAL RAWID FROM DUAL");
				int apply_user_cnt = 0;
				
				//=================================================================
				//학생 월과금 - 환불처리
				//=================================================================
				for(int i = 0; i < rowset.size(); i++) {
					LinkedHashMap temp = (LinkedHashMap) rowset.get(i);
					
					//1)재수강신청 사용일수 확인, 2014-04-07 추가(웹에서 재수강신청시 차감안하는 것으로 변경되어 수정함)
					LinkedList tempRowset = null;
					query.setLength(0);
					query.append("/* use:WEB; method-name:" + this.getClass().getName() + "재수강신청 사용일계산 */");
					query.append("\n SELECT UT.ACADEMY_RAWID,UT.USER_RAWID,UT.FROM_USE_STATUS_CD,UT.TO_USE_STATUS_CD,TO_CHAR(UT.APPLY_DT,'YYYY-MM-DD') APPLY_DT,TO_CHAR(UT.STOP_DT,'YYYY-MM-DD') STOP_DT, ");
					
					if(runYn.equals("Y")){
						query.append("\n        NVL((TO_DATE(LAST_DAY(ADD_MONTHS(SYSDATE,-1)))-TO_DATE(APPLY_DT))+1,0) USE_COUNT ");
					}else{
						//환불예상
						query.append("\n        NVL((TO_DATE(LAST_DAY(SYSDATE))-TO_DATE(APPLY_DT))+1,0) USE_COUNT ");
					}
					
					query.append("\n   FROM USER_MONTH_TRX UT, USER_MONTH_MST UM ");
					query.append("\n  WHERE UT.ACADEMY_RAWID = ? ");
					query.append("\n    AND UM.USER_RAWID = ? ");
					query.append("\n    AND UM.USER_MONTH_TRX_RAWID =UT.RAWID ");
					query.append("\n    AND TO_CHAR(UT.APPLY_DT,'YYYY-MM') = ? ");
					query.append("\n    AND TO_USE_STATUS_CD = '01' ");
					
					dataList.clear();
					dataList.add(academy_rawid);
					dataList.add(temp.get("user_rawid").toString());
					dataList.add(predate);
					
					tempRowset = das3.select(null, query.toString(), dataList);
					
					String reUseCount = "0";
					if(tempRowset.size()>0){
						LinkedHashMap tempMap = (LinkedHashMap) tempRowset.get(0);
						reUseCount = tempMap.get("use_count").toString();
					}
					
					//2)학원 월과금 가격
					String tot_price = selectKey("tot_price","SELECT NVL(TOT_PRICE,0) TOT_PRICE FROM ACADEMY_CASH_PRODUCT_MST WHERE ACADEMY_RAWID = '"+academy_rawid+"' AND USE_YN='Y' AND CASH_TYPE_CD='MC'");
					int reApplyCash = 0; //재수강신청 차감금액을 계산함. (2014-04-16 추가)
					if(!tot_price.equals("")&&refundLastDay>0) {
						reApplyCash = (Integer.parseInt(tot_price)/refundLastDay)*Integer.parseInt(reUseCount);
					}
					
					int refundCash = 0;
					int refundRate = 0; 
					int useCount = Integer.parseInt(temp.get("use_count").toString());
					int basicCount = Integer.parseInt(temp.get("basic_count").toString()); //비율계산 기준일자
					int applyCash = 0;
					if(!voidNull(temp.get("apply_cash").toString()).equals("")){
						applyCash = Integer.parseInt(voidNull(temp.get("apply_cash").toString()));
					}
					String user_rawid = temp.get("user_rawid").toString();
					String apply_dt = temp.get("apply_dt").toString();
					String stop_dt = temp.get("stop_dt").toString();
					
					//1.환불비율 2/3 확인
					if((int) Math.ceil((double)basicCount/3)>=useCount){
						refundRate = (int)Math.floor(((double)2/3)*100);
						refundCash = (applyCash/3)*2;
					}
					//2.환불비율 1/2 확인
					else if((int) Math.ceil((double)basicCount/2)>=useCount){
						refundRate = (int)Math.floor(((double)1/2)*100);
						refundCash = (applyCash/2);
					}
					
					//3.최종 환불금액 계산
					refundCash = refundCash-reApplyCash;
					if(refundCash<0) refundCash = 0; //마이너스는 0원 처리함
					
					academyRefund = academyRefund+refundCash;
					
					//환불처리 실행
					if(runYn.equals("Y") && refundCash>0){
						//1.학생 월캐시 이력 등록.
						String rawid = selectKey("rawid","SELECT MAX(RAWID) RAWID FROM USER_MONTH_CASH_HST WHERE ACADEMY_RAWID = '"+academy_rawid+"' AND USER_RAWID = '"+user_rawid+"'");
						
						query.setLength(0);
						query.append(" INSERT INTO USER_MONTH_CASH_HST ");
						query.append(" (RAWID,ACADEMY_CASH_HST_RAWID,ACADEMY_RAWID,USER_RAWID,PREV_TOTAL_USE,NOW_TOTAL_USE, ");
						query.append("  PREV_USE, NOW_USE,WORKED_BY, APPLY_CASH, REFUND_CASH, START_DT, END_DT, REFUND_RATE, ");
						query.append("  CREATE_BY, CREATE_DT) ");
						query.append(" SELECT SEQ_USER_MONTH_CASH_HST.NEXTVAL,?,?,?,PREV_TOTAL_USE,NOW_TOTAL_USE, ");
						query.append(" PREV_USE, NOW_USE,'S', 0, ?, ?, ?, ?, ?, SYSDATE ");
						query.append("  FROM USER_MONTH_CASH_HST WHERE RAWID = ? ");
						
						dataList.clear();
						dataList.add(academy_cash_hst_rawid);
						dataList.add(academy_rawid);
						dataList.add(user_rawid);
						dataList.add(refundCash);
						dataList.add(apply_dt);
						dataList.add(stop_dt);
						dataList.add(refundRate);
						dataList.add(academy_rawid);
						dataList.add(rawid);
						das3.execute(conn,query.toString(), dataList);
						
						apply_user_cnt = apply_user_cnt+1;
					}
				}//end for
				
				//logger.info("---- academy_rawid :"+academy_rawid+" academyRefund:"+academyRefund);
				if(academyRefund>0){
					if(runYn.equals("N")){
						//환불예상캐시
						query.setLength(0);
						query.append("   UPDATE ACADEMY_CASH_MST SET REFUND_CASH = ? WHERE ACADEMY_RAWID = ? ");
						
						dataList.clear();
						dataList.add(academyRefund);
						dataList.add(academy_rawid);
						das3.execute(conn,query.toString(), dataList);
					}else{
						//환불처리
						String remainCash = selectKey("remain_cash","SELECT REMAIN_CASH+"+academyRefund+" REMAIN_CASH FROM ACADEMY_CASH_MST WHERE ACADEMY_RAWID = '"+academy_rawid+"' AND BUSINESS_TYPE = '"+org_cd+"'");
						
						//2.학원 캐시이력 등록
						query.setLength(0);
						query.append(" INSERT INTO ACADEMY_CASH_HST (RAWID,BUSINESS_TYPE,ACADEMY_RAWID,CREATE_BY,USER_RAWID,");
						query.append(" CASH_USE_TYPE_CD,USE_CASH,CREATE_DT,REMAIN_CASH,CREATE_TYPE_CD,APPLY_USER_CNT,CASH_TYPE_CD) ");
						query.append(" VALUES(?,?,?,'A',?,");
						query.append(" 'MCR',?,SYSDATE,?,'S',?,'MC') ");
						
						dataList.clear();
						dataList.add(academy_cash_hst_rawid);
						dataList.add(org_cd);
						dataList.add(academy_rawid);
						dataList.add(academy_rawid);
						dataList.add(academyRefund);
						dataList.add(remainCash);
						dataList.add(apply_user_cnt);
						das3.execute(conn,query.toString(), dataList);
						
						//3.학원 잔여캐시 수정
						query.setLength(0);
						query.append("   UPDATE ACADEMY_CASH_MST SET REMAIN_CASH = ?, REFUND_CASH = 0 WHERE ACADEMY_RAWID = ? ");
						
						dataList.clear();
						dataList.add(remainCash);
						dataList.add(academy_rawid);
						das3.execute(conn,query.toString(), dataList);
					}
					
					logger.info("---- academyRefund("+runYn+") >> academy_rawid :"+academy_rawid+" refundCash :"+academyRefund);
				}
			}//end for
			
			das3.commit(conn);
			result = "OK!";
		} catch (Exception e) {
			logger.error(e);
			if (das3 != null) try { das3.rollback(this.conn); } catch(Exception ex) {}
		} finally{
			if (das3 != null) try { das3.commit(this.conn); } catch(Exception ex) {}
		}
		
		return result;
	}
	
	//차감
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private String runCashMng(String predate, int refundLastDay, String runYn) throws Exception{
		String result = "";
		LinkedList rowset = null;
		LinkedList dataList = new LinkedList();
		StringBuffer query = new StringBuffer(32);
		
		LinkedList<Object> academyRowset = existAcademy();
		
		try{
			conn = das3.beginTrans();
			for (Object list : academyRowset){
				LinkedHashMap academyTemp = ((LinkedHashMap)list);
				
				String academy_rawid = academyTemp.get("rawid").toString();
				String org_cd = academyTemp.get("organization_type_cd").toString(); 
				if(org_cd.equals("SR")) org_cd = "YS"; //영신
				String user_id = academyTemp.get("user_id").toString();
				String name = academyTemp.get("name").toString();
				String cell_number = academyTemp.get("cell_number").toString();
				String msg = "월과금 차감 예상 캐시가 부족합니다. 필요하시면 더 충전해주세요.";
				
				if(org_cd.equals("YS")){
					msg = "[영신]"+msg;
				}else{
					msg = "[SM]"+msg;
				}
				
				
				query.setLength(0);
				query.append("/* use:WEB; method-name:" + this.getClass().getName() + "사용중 차감 대상 */");
				query.append("\n   SELECT  UT.ACADEMY_RAWID,UT.USER_RAWID,UT.FROM_USE_STATUS_CD,UT.TO_USE_STATUS_CD,TO_CHAR(UT.APPLY_DT,'YYYY-MM-DD') APPLY_DT,TO_CHAR(UT.STOP_DT,'YYYY-MM-DD') STOP_DT, ");
				query.append("\n           US.TO_USER_STATUS_CD ");
				query.append("\n      FROM USER_MONTH_MST UM, USER_MONTH_TRX UT, ");
				query.append("\n           (SELECT USER_RAWID, TO_USER_STATUS_CD ");
				query.append("\n              FROM ( ");
				query.append("\n                    SELECT A.USER_RAWID,TO_USER_STATUS_CD, ROW_NUMBER()OVER(PARTITION BY A.USER_RAWID ORDER BY A.RAWID DESC) RN ");
				query.append("\n                      FROM USER_STATUS_TRX A, USER_MST B ");
				query.append("\n                     WHERE B.ACADEMY_RAWID = ? ");
				query.append("\n                       AND A.USER_RAWID=B.RAWID ");
				query.append("\n            )WHERE RN = 1) US ");
				query.append("\n      WHERE UM.ACADEMY_RAWID = ? ");
				query.append("\n      AND UM.USER_MONTH_TRX_RAWID = UT.RAWID ");
				query.append("\n AND TO_CHAR(UT.APPLY_DT,'YYYY-MM') = ? ");
				query.append("\n AND TO_USE_STATUS_CD = '01' ");
				query.append("\n AND US.USER_RAWID=UM.USER_RAWID ");
				
				dataList.clear();
				dataList.add(academy_rawid);
				dataList.add(academy_rawid);
				dataList.add(predate);
				
				rowset = das3.select(null, query.toString(), dataList);
				
				//학원 월과금 가격
				String tot_price = selectKey("tot_price","SELECT NVL(TOT_PRICE,0) TOT_PRICE FROM ACADEMY_CASH_PRODUCT_MST WHERE ACADEMY_RAWID = '"+academy_rawid+"' AND USE_YN='Y' AND CASH_TYPE_CD='MC'");
				if(tot_price.equals("")) tot_price = "0";
				//학원캐시이력 rawid
				String academy_cash_hst_rawid = selectKey("rawid","SELECT SEQ_ACADEMY_CASH_HST.NEXTVAL RAWID FROM DUAL");
				//학원차감캐시
				int apply_user_cnt = 0;
				int academyCash = 0; //유료캐시 차감 합계
				int academyFreeCash = 0; //학원 마일리지캐시 차감 합계
				
				//잔여예상캐시확인(말일 2,5일전 )
				if(runYn.equals("N")){
					String remainCash = selectKey("remain_cash","SELECT NVL(REMAIN_CASH,0)+NVL(REFUND_CASH,0) REMAIN_CASH FROM ACADEMY_CASH_MST WHERE ACADEMY_RAWID = '"+academy_rawid+"' AND BUSINESS_TYPE = '"+org_cd+"'");
					if(remainCash.equals("")) remainCash = "0";

					//차감대상 학생수
					for(int i = 0; i < rowset.size(); i++) {
						LinkedHashMap temp = (LinkedHashMap) rowset.get(i);
						String user_status_cd = temp.get("to_user_status_cd").toString();
						if(user_status_cd.equals("01")){
							apply_user_cnt = apply_user_cnt+1;
						}
					}
					academyCash = apply_user_cnt*Integer.parseInt(tot_price); //차감예상 학원캐시
					
					logger.info("---- SMS(문자 발송 확인) >> academy_rawid:"+academy_rawid+" academyCash:"+academyCash+" remainCash:"+remainCash);
					
					if(Integer.parseInt(remainCash)<academyCash){
						//캐시부족 문자 발송
						logger.info("---- SMS(문자 발송 대상) >> academy_rawid:"+academy_rawid+" cell_number:"+voidNull(cell_number));
						LinkedHashMap alInform = new LinkedHashMap();
						alInform.put("ACADEMY_RAWID" 	, academy_rawid);
						alInform.put("USER_ID"			, user_id);
						alInform.put("NAME"				, name);
						alInform.put("CELL_NUMBER" 		, voidNull(cell_number));
						alInform.put("MESSAGE" 			, msg);
						this.SendSMS(alInform, true);
					}
				}else if(runYn.equals("Y")){
					//=================================================================
					//학생 월과금 - 차감처리
					//=================================================================
					//학원 -마일리지캐시
					String freeCash = selectKey("free_cash","SELECT NVL(FREE_CASH,0) FREE_CASH FROM ACADEMY_CASH_MST WHERE ACADEMY_RAWID = '"+academy_rawid+"' AND BUSINESS_TYPE = '"+org_cd+"' AND ADD_MONTHS(FREE_DT,3) > SYSDATE");
					
					//현재캐시
					String remainCash = selectKey("remain_cash","SELECT NVL(REMAIN_CASH,0) REMAIN_CASH FROM ACADEMY_CASH_MST WHERE ACADEMY_RAWID = '"+academy_rawid+"' AND BUSINESS_TYPE = '"+org_cd+"'");
					if(freeCash.equals("")) freeCash = "0";
					if(remainCash.equals("")) remainCash = "0";
					int calFreeCash = Integer.parseInt(freeCash);
					int calRemainCash = Integer.parseInt(remainCash);
					int calRemainSum = calRemainCash + calFreeCash; //유료,무료 합계
					for(int i = 0; i < rowset.size(); i++) {
						LinkedHashMap temp = (LinkedHashMap) rowset.get(i);
						String user_rawid = temp.get("user_rawid").toString();
						String user_month_trx_rawid = selectKey("rawid","SELECT SEQ_USER_MONTH_TRX.NEXTVAL RAWID FROM DUAL");
						String user_status_cd = temp.get("to_user_status_cd").toString(); //학생상태 : 01 - 사용중
						
						if(user_status_cd.equals("01")){
							if((calRemainCash+calFreeCash)<Integer.parseInt(tot_price)){
								//캐시부족 중지 처리
								//1.학생 월결제 수강변경 이력
								query.setLength(0);
								query.append(" INSERT INTO USER_MONTH_TRX(RAWID,ACADEMY_RAWID,USER_RAWID,FROM_USE_STATUS_CD,");
								query.append(" TO_USE_STATUS_CD,CREATE_DT,MODIFY_USER_TYPE,MODIFY_USER_RAWID,APPLY_DT,STOP_DT,APPLY_CASH,WORKED_BY) ");
								query.append(" VALUES (?,?,?,'01',");
								query.append(" '04',SYSDATE,'A',?,TO_CHAR(SYSDATE,'YYYY-MM')||'-01',TO_CHAR(SYSDATE,'YYYY-MM')||'-01',0,'S') ");
								
								dataList.clear();
								dataList.add(user_month_trx_rawid);
								dataList.add(academy_rawid);
								dataList.add(user_rawid);
								dataList.add(academy_rawid);
								das3.execute(conn,query.toString(), dataList);
								
								//2.학생 월결제 수강변경 수정
								query.setLength(0);
								query.append(" UPDATE USER_MONTH_MST SET USER_MONTH_TRX_RAWID = ?, CREATE_DT=SYSDATE");
								query.append(" WHERE ACADEMY_RAWID = ? AND USER_RAWID = ?  ");
								
								dataList.clear();
								dataList.add(user_month_trx_rawid);
								dataList.add(academy_rawid);
								dataList.add(user_rawid);
								das3.execute(conn,query.toString(), dataList);
								
								logger.info("---- userStop(캐시부족 중지) >> academy_rawid:"+academy_rawid+" user_rawid:"+user_rawid);
							}else{
								apply_user_cnt = apply_user_cnt+1; //학생 차감수
								int calApplyCash = Integer.parseInt(tot_price); //유료캐시 차감금액
								
								//0.마일리지 캐시 확인
								if(calFreeCash>0){
									if(calFreeCash >= Integer.parseInt(tot_price)){
										//마일리지 캐시 사용
										calApplyCash = 0;
										calFreeCash = calFreeCash-Integer.parseInt(tot_price);  //학원 마일리지 차감 잔액
										academyFreeCash = academyFreeCash + Integer.parseInt(tot_price);
									}else{
										//마일리지 + 유료캐시 사용
										calApplyCash = Integer.parseInt(tot_price)-calFreeCash;
										academyCash = academyCash + calApplyCash; 
										academyFreeCash = academyFreeCash + calFreeCash;
										calRemainCash = calRemainCash-calApplyCash;  //학원캐시 차감 잔액
										calFreeCash = 0;  //학원 마일리지 차감 잔액
									}
								}else{
									//유료캐시 사용
									calRemainCash = calRemainCash-Integer.parseInt(tot_price);  //학원캐시 차감 잔액
									academyCash = academyCash + Integer.parseInt(tot_price); //학원 차감캐시
								}
								
								//1.학생 월결제 수강변경 이력
								query.setLength(0);
								query.append(" INSERT INTO USER_MONTH_TRX(RAWID,ACADEMY_RAWID,USER_RAWID,FROM_USE_STATUS_CD,");
								query.append(" TO_USE_STATUS_CD,CREATE_DT,MODIFY_USER_TYPE,MODIFY_USER_RAWID,APPLY_DT,APPLY_CASH,WORKED_BY) ");
								query.append(" VALUES (?,?,?,'01',");
								query.append(" '01',SYSDATE,'A',?,TO_CHAR(SYSDATE,'YYYY-MM')||'-01',?,'S') ");
								
								dataList.clear();
								dataList.add(user_month_trx_rawid);
								dataList.add(academy_rawid);
								dataList.add(user_rawid);
								dataList.add(academy_rawid);
								dataList.add(calApplyCash);
								das3.execute(conn,query.toString(), dataList);
								
								//2.학생 월결제 수강변경 수정
								query.setLength(0);
								query.append(" UPDATE USER_MONTH_MST SET USER_MONTH_TRX_RAWID = ?, CREATE_DT=SYSDATE");
								query.append(" WHERE ACADEMY_RAWID = ? AND USER_RAWID = ?  ");
								
								dataList.clear();
								dataList.add(user_month_trx_rawid);
								dataList.add(academy_rawid);
								dataList.add(user_rawid);
								das3.execute(conn,query.toString(), dataList);
								
								//3.학생 월캐시이력 등록
								String rawid = selectKey("rawid","SELECT MAX(RAWID) RAWID FROM USER_MONTH_CASH_HST WHERE ACADEMY_RAWID = '"+academy_rawid+"' AND USER_RAWID = '"+user_rawid+"'");
								
								query.setLength(0);
								query.append(" INSERT INTO USER_MONTH_CASH_HST ");
								query.append(" (RAWID,ACADEMY_CASH_HST_RAWID,ACADEMY_RAWID,USER_RAWID,PREV_TOTAL_USE,NOW_TOTAL_USE, ");
								query.append("  PREV_USE, NOW_USE,WORKED_BY, APPLY_CASH, REFUND_CASH, START_DT,CREATE_BY, CREATE_DT) ");
								query.append(" SELECT SEQ_USER_MONTH_CASH_HST.NEXTVAL,?,?,?,PREV_TOTAL_USE,NOW_TOTAL_USE, ");
								query.append(" PREV_USE, NOW_USE,'S', ?, '0', TO_CHAR(SYSDATE,'YYYY-MM')||'-01', ?, SYSDATE ");
								query.append("  FROM USER_MONTH_CASH_HST WHERE RAWID = ? ");
								
								dataList.clear();
								dataList.add(academy_cash_hst_rawid);
								dataList.add(academy_rawid);
								dataList.add(user_rawid);
								dataList.add(tot_price);
								dataList.add(academy_rawid);
								dataList.add(rawid);
								das3.execute(conn,query.toString(), dataList);
								
								logger.info("---- userCash(캐시차감) >> academy_rawid:"+academy_rawid+" user_rawid:"+user_rawid);
							}
						}//학생상태 사용중일때 차감처리 if end
						else{
							//학생상태 사용중 이외 상태 - 수강신청 중지 처리
							//1.학생 월결제 수강변경 이력
							query.setLength(0);
							query.append(" INSERT INTO USER_MONTH_TRX(RAWID,ACADEMY_RAWID,USER_RAWID,FROM_USE_STATUS_CD,");
							query.append(" TO_USE_STATUS_CD,CREATE_DT,MODIFY_USER_TYPE,MODIFY_USER_RAWID,APPLY_DT,STOP_DT,APPLY_CASH,WORKED_BY) ");
							query.append(" VALUES (?,?,?,'01',");
							query.append(" '02',SYSDATE,'A',?,TO_CHAR(SYSDATE,'YYYY-MM')||'-01',TO_CHAR(SYSDATE,'YYYY-MM')||'-01',0,'S') ");
							
							dataList.clear();
							dataList.add(user_month_trx_rawid);
							dataList.add(academy_rawid);
							dataList.add(user_rawid);
							dataList.add(academy_rawid);
							das3.execute(conn,query.toString(), dataList);
							
							//2.학생 월결제 수강변경 수정
							query.setLength(0);
							query.append(" UPDATE USER_MONTH_MST SET USER_MONTH_TRX_RAWID = ?, CREATE_DT=SYSDATE");
							query.append(" WHERE ACADEMY_RAWID = ? AND USER_RAWID = ?  ");
							
							dataList.clear();
							dataList.add(user_month_trx_rawid);
							dataList.add(academy_rawid);
							dataList.add(user_rawid);
							das3.execute(conn,query.toString(), dataList);
							
							logger.info("---- userStop(학생상태로 인한 중지) >> academy_rawid:"+academy_rawid+" user_rawid:"+user_rawid);
						}
					}//end for
					
					if((academyCash+academyFreeCash)>0){
						//학원 잔여캐시 수정
						query.setLength(0);
						query.append(" UPDATE ACADEMY_CASH_MST SET REMAIN_CASH = ?, REFUND_CASH = 0, FREE_CASH = ? WHERE ACADEMY_RAWID = ? ");
						
						dataList.clear();
						dataList.add(calRemainCash);
						dataList.add(calFreeCash);
						dataList.add(academy_rawid);
						das3.execute(conn,query.toString(), dataList);
						
						//학원 캐시이력 등록
						calRemainSum = calRemainSum - academyCash - academyFreeCash;
						query.setLength(0);
						query.append(" INSERT INTO ACADEMY_CASH_HST (RAWID,BUSINESS_TYPE,ACADEMY_RAWID,CREATE_BY,USER_RAWID,");
						query.append(" CASH_USE_TYPE_CD,USE_CASH,CREATE_DT,REMAIN_CASH,CREATE_TYPE_CD,APPLY_USER_CNT,CASH_TYPE_CD,FREE_CASH) ");
						query.append(" VALUES(?,?,?,'A',?,");
						query.append(" ?,?,SYSDATE,?,'S',?,'MC',?) ");
						
						dataList.clear();
						dataList.add(academy_cash_hst_rawid);
						dataList.add(org_cd);
						dataList.add(academy_rawid);
						dataList.add(academy_rawid);
						if(academyCash>0&&academyFreeCash>0){
							dataList.add("MMR");	//유료+마일리지
						}else if(academyFreeCash>0){
							dataList.add("MFR");	//마일리지
						}else{
							dataList.add("MCU");	//유료
						}
						dataList.add(academyCash);
						dataList.add(calRemainSum);
						dataList.add(apply_user_cnt);
						dataList.add(academyFreeCash);
						das3.execute(conn,query.toString(), dataList);
						
						logger.info("---- academyCash(학원캐시차감) >> academy_rawid:"+academy_rawid+" academyCash:"+academyCash+" academyFreeCash:"+academyFreeCash);
					}
				}//차감 처리 완료
			}//end for
			
			das3.commit(conn);
//			result = "select-"+academyRowset.size()+":delete-"+deleteList.size()+":insert-"+insertList.size();
		} catch (Exception e) {
			logger.error(e);
			if (das3 != null) try { das3.rollback(this.conn); } catch(Exception ex) {}
		} finally{
			if (das3 != null) try { das3.commit(this.conn); } catch(Exception ex) {}
		}
		
		return result;
	}
	
	/**
	 * 월과금 대상 학원조회
	 * @param class_rawid
	 * @param user_rawid
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked", "unused" })
	private LinkedList existAcademy() throws Exception{
		LinkedList rowset = null;
		LinkedList dataList = new LinkedList();
		StringBuffer query = new StringBuffer(32);
		
		query.append("/* method-name:" + this.getClass().getName() + "월과금대상학원 */");
		query.append("\n   SELECT RAWID, ORGANIZATION_TYPE_CD, USER_ID, NAME, CELL_NUMBER FROM ACADEMY_MST ");
		query.append("\n    WHERE PAY_CD = ?");
		
		//영신은 무료월 이후로 차감 처리
		query.append("\n      AND (ORGANIZATION_TYPE_CD != 'SR' OR ORGANIZATION_TYPE_CD = 'SR' AND TO_CHAR(SM_CASH_START_DT,'YYYY-MM') < TO_CHAR(SYSDATE,'YYYY-MM')) ");
		
		dataList.add(pay_cd);
		
		//TODO : TEST
		if(!Tacademy_rawid.equals("")){
			query.append("\n    AND RAWID = ?");
			dataList.add(Tacademy_rawid);
		}
		query.append("\n      AND DEL_YN = 'N' ");
		
		
		
		rowset = das3.select(null, query.toString(), dataList);
		
		return rowset;
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
			das = new DAS(Constants.DAS_CONFIG);
			
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
			
			//TODO :TEST
			if(!Tcell_number.equals("")){
				cell_number = Tcell_number;
			}
			
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
	
	/**
	 * 출판사과금 처리 2017-08-21 최현일 
	 * 1.USER_MONTH_CASH_HST insert
	 * 2.ACADEMY_CASH_HST insert
	 * 2.ACADEMY_CASH_MST update
	 * @return 
	 */
	@SuppressWarnings({ "rawtypes", "unchecked" })
	private void overCountCompany()
	{
		LinkedList rowset = null;
		LinkedList dataList = new LinkedList();
		StringBuffer query = new StringBuffer();
		String academy_rawid = "";
		String class_rawid = "";
		String user_rawid = "";
		try{
		//월과금 대상학원을 먼저 구한다.
		conn = das3.beginTrans();
		query.append("   SELECT RAWID, ORGANIZATION_TYPE_CD, USER_ID, NAME, CELL_NUMBER FROM ACADEMY_MST ");
		query.append("   WHERE PAY_CD = 'M' AND DEL_YN = 'N'");
		rowset = das3.select(conn, query.toString(), dataList);
		//사용하는 학원을 구한다.
		LinkedList<Object> academyRowset = rowset;
		for (Object list : academyRowset){
			 int resultCash = 0;  	//적용캐쉬
			 int apply_user_cnt = 0; //적용인원
			 LinkedHashMap academyTemp = ((LinkedHashMap)list);
			 academy_rawid = academyTemp.get("rawid").toString();
			 String org_cd = academyTemp.get("organization_type_cd").toString(); 
			 if(org_cd.equals("SR")) org_cd = "YS";
			 int totalOverCompanyPrice = 0;
			 query.setLength(0);
			 //학원별 출판사 과금적용 학생을 구한다.
			 query.append(" SELECT DISTINCT USER_RAWID,USER_NAME,USER_ID,CREATE_DT,COMPANY_CNT,ORGANIZATION_TYPE_CD,CLASS_RAWID");
			 query.append("          FROM (  ");
			 query.append("          SELECT UM.RAWID AS USER_RAWID,UM.NAME AS USER_NAME, UM.USER_ID,TO_CHAR (UM.CREATE_DT, 'YYYY-MM-DD') AS CREATE_DT,CM2.COMPANY_CNT,AM.ORGANIZATION_TYPE_CD,CM2.CLASS_RAWID");
			 query.append("          FROM USER_MST UM ");
			 query.append("          INNER JOIN ACADEMY_MST AM ON AM.RAWID = UM.ACADEMY_RAWID AND AM.PAY_CD ='M' AND AM.IS_USED='A' AND AM.DEL_YN ='N'");
			 query.append("          INNER JOIN USER_MONTH_MST UMM ON UMM.USER_RAWID = UM.RAWID AND TO_CHAR(UMM.CREATE_DT,'YYYY-MM') = TO_CHAR(SYSDATE,'YYYY-MM')  ");
			 query.append("          INNER JOIN USER_MONTH_TRX UMT ON UMT.RAWID = UMM.USER_MONTH_TRX_RAWID AND UMT.APPLY_DT <= SYSDATE AND TO_USE_STATUS_CD = 01  ");
			 query.append("          LEFT JOIN (    ");
			 query.append("            SELECT USER_RAWID,ACADEMY_RAWID,COUNT(*) AS COMPANY_CNT,CLASS_RAWID  FROM ( ");
			 query.append("                         SELECT UBLM.USER_RAWID,BM.COMPANY_CD,UBLM.ACADEMY_RAWID,COUNT(*) AS COMPANY_CNT,CU.CLASS_RAWID  ");
			 query.append("                        FROM USER_BOOK_LINK_MST UBLM     ");
			 query.append("          			   INNER JOIN CLASS_USER_LINK_MST CU  ON CU.USER_RAWID =UBLM.USER_RAWID  AND CU.CLASS_RAWID = UBLM.CLASS_RAWID AND CLASS_KIND_CD ='NC'");
			 query.append("                        INNER JOIN CLASS_MST CM ON CM.RAWID = CU.CLASS_RAWID  AND CM.DEL_YN='N' ");
			 query.append("                        INNER JOIN BOOK_MST BM  ON  UBLM.BOOK_RAWID = BM.RAWID ");
			 query.append("                        INNER JOIN PARTNER_MST PM ON PM.COMPANY_CD =BM.COMPANY_CD    ");;
			 query.append("                        AND UBLM.START_DT <= TO_CHAR(SYSDATE, 'yyyy-mm-dd')    ");
			 query.append("                        AND UBLM.END_DT >= TO_CHAR(SYSDATE, 'yyyy-mm-dd')     ");
			 query.append("                        AND PM.SELECT_YN ='Y'     ");
			 query.append("                        GROUP BY UBLM.USER_RAWID,BM.COMPANY_CD,UBLM.ACADEMY_RAWID,CU.CLASS_RAWID) GROUP BY USER_RAWID,ACADEMY_RAWID,CLASS_RAWID");
			 query.append("           )CM2 ON CM2.ACADEMY_RAWID = UM.ACADEMY_RAWID AND CM2.USER_RAWID = UM.RAWID     ");
			 query.append("  	AND UM.DEL_YN = 'N'  AND UM.ACADEMY_RAWID = ?");
			 query.append("  )  WHERE COMPANY_CNT > 3 ORDER BY CREATE_DT DESC");
			 dataList.clear();
			 dataList.add(academy_rawid);
			 LinkedList<Object> overCountRowset = das3.select(conn, query.toString(), dataList);
			 String academy_cash_hst_rawid = selectKey("rawid","SELECT SEQ_ACADEMY_CASH_HST.NEXTVAL RAWID FROM DUAL");
			 //학원에 출판사 과금 학생이 존재한다면...
			 if(overCountRowset.size() > 0){
			 for (Object overList : overCountRowset){
						 LinkedHashMap overCountUser = ((LinkedHashMap)overList);
						 user_rawid= overCountUser.get("user_rawid").toString(); //학생 아이디
						 class_rawid= overCountUser.get("class_rawid").toString(); //클래스 아이디
						 //학원 출판사과금 가격
						 String tot_price = selectKey("tot_price","SELECT NVL(TOT_PRICE,0) TOT_PRICE FROM ACADEMY_CASH_PRODUCT_MST WHERE ACADEMY_RAWID = '"+academy_rawid+"' AND USE_YN='Y' AND CASH_TYPE_CD='MC'");
						 int overCompanyPrice = (Integer.parseInt(overCountUser.get("company_cnt").toString()) - 3) * (int)(Integer.parseInt(tot_price) * 0.1);  //과금 금액
						 //1.학생 월캐시 이력 등록.
						 String rawid = selectKey("rawid","SELECT MAX(RAWID) RAWID FROM USER_MONTH_CASH_HST WHERE ACADEMY_RAWID = '"+academy_rawid+"' AND USER_RAWID = '"+user_rawid+"'");
						 query.setLength(0);
						 query.append(" SELECT LISTAGG(NAME, ',') WITHIN GROUP (ORDER BY NAME) NOW_COMPANY_NAME FROM( ") ;  
						 query.append(" SELECT CB.CLASS_RAWID, BM.COMPANY_CD, PMA.NAME, COUNT(*) OVER() COMPANY_COUNT ");
						 query.append(" FROM CLASS_BOOK_LINK_MST CB");
						 query.append(" INNER JOIN BOOK_MST BM ON BM.RAWID = CB.BOOK_RAWID"); 
						 query.append(" INNER JOIN PARTNER_MST PMA ON PMA.COMPANY_CD = BM.COMPANY_CD"); 
						 query.append(" WHERE CB.CLASS_RAWID =" + class_rawid);
						 query.append(" AND CB.START_DT <= TO_CHAR(SYSDATE ,'YYYY-MM-DD')"); 
						 query.append(" AND CB.END_DT >= SYSDATE"); 
						 query.append(" GROUP BY CB.CLASS_RAWID, BM.COMPANY_CD, PMA.NAME)"); 
						 query.append(" GROUP BY CLASS_RAWID, COMPANY_COUNT ");
						 String now_company_name = selectKey("now_company_name",query.toString());
						 query.setLength(0);
						 query.append(" INSERT INTO USER_MONTH_CASH_HST ");
						 query.append(" (RAWID,ACADEMY_CASH_HST_RAWID,ACADEMY_RAWID,USER_RAWID,PREV_TOTAL_USE,NOW_TOTAL_USE, ");
						 query.append(" PREV_USE, NOW_USE,WORKED_BY, APPLY_CASH, REFUND_CASH, START_DT, END_DT, REFUND_RATE, ");
						 query.append(" CREATE_BY, CREATE_DT) ");
						 query.append(" SELECT SEQ_USER_MONTH_CASH_HST.NEXTVAL,?,?,?,PREV_TOTAL_USE,?, ");
						 query.append(" PREV_USE,NOW_USE,'S', ?, 0, START_DT,END_DT, 0, ?, SYSDATE ");
						 query.append(" FROM USER_MONTH_CASH_HST WHERE RAWID = ? ");
						 dataList.clear();
						 dataList.add(academy_cash_hst_rawid);
						 dataList.add(academy_rawid);
						 dataList.add(user_rawid);
						 dataList.add(now_company_name);
						 dataList.add(overCompanyPrice);
						 dataList.add(academy_rawid);
						 dataList.add(rawid);
						 totalOverCompanyPrice += overCompanyPrice;
						 apply_user_cnt = apply_user_cnt+1; 
						 das3.execute(conn,query.toString(), dataList);
		 		 		}
						//학원 출판사과금 캐시이력 등록
						 String nowCash = selectKey("remain_cash","SELECT NVL(REMAIN_CASH,0) REMAIN_CASH FROM ACADEMY_CASH_MST WHERE ACADEMY_RAWID = '"+academy_rawid+"'");
						 query.setLength(0);
						 query.append(" INSERT INTO ACADEMY_CASH_HST (RAWID,BUSINESS_TYPE,ACADEMY_RAWID,CREATE_BY,USER_RAWID,");
						 query.append(" CASH_USE_TYPE_CD,USE_CASH,CREATE_DT,REMAIN_CASH,SCALL_PAY_TRX_RAWID,CREATE_TYPE_CD,APPLY_USER_CNT,NON_APPLY_USER_CNT,CASH_TYPE_CD,FREE_CASH) ");
						 query.append(" VALUES(?,?,?,'A',?,");
						 query.append(" ?,?,SYSDATE,?,'0','S',?,'0','MC','0') ");
						 resultCash = Integer.parseInt(nowCash) - totalOverCompanyPrice;
						 dataList.clear();
						 dataList.add(academy_cash_hst_rawid);
						 dataList.add(org_cd);
						 dataList.add(academy_rawid);
						 dataList.add(user_rawid);
						 dataList.add("PP");	
						 dataList.add(totalOverCompanyPrice);
						 dataList.add(resultCash);
						 dataList.add(apply_user_cnt);
						 das3.execute(conn,query.toString(), dataList); 	   
						 //학원 출판사과금 캐시 수정
						 query.setLength(0);
						 query.append("UPDATE ACADEMY_CASH_MST SET REMAIN_CASH = ? WHERE ACADEMY_RAWID = ? ");
						 dataList.clear();
						 dataList.add(resultCash);
						 dataList.add(academy_rawid);
						 das3.execute(conn,query.toString(), dataList);
						 logger.info("---- academyCash(출판사과금) >> academy_rawid:"+academy_rawid+" totalOverCompanyPrice:"+totalOverCompanyPrice);
			 		}
				}//end for
				das3.commit(conn);
			}catch (Exception e) {
					logger.error(e);
					if (das3 != null) try { das3.rollback(this.conn); } catch(Exception ex) {}
				}
		}
	
	/**
	 * key 조회
	 * @param academyid
	 * @return
	 * @throws Exception 
	 */
	private String selectKey(String key, String query) throws Exception{
		LinkedList rowset = das3.select(null, query, null);
		
		if (rowset.isEmpty())
			return "";
		else {
			return ((LinkedHashMap)rowset.get(0)).get(key).toString();
		}
	}
	
	public static String voidNull(String param){
	    if(param == null)
	        return "";
	    if(param.trim().equals("")){
	    	return "";
	    }else{
	         return param.trim();
	    }
	}	
}
