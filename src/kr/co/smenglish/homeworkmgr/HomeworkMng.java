package kr.co.smenglish.homeworkmgr;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import kr.co.smenglish.coreclone.util.DASAssistant2;
import kr.co.smenglish.coreclone.util.SimpleDateCtrl;
import kr.co.smenglish.couponmgr.CouponServer;
import kr.co.smenglish.das.sql.DAS3;
import kr.co.smenglish.homeworkmgr.data.ContentsData;
import kr.co.smenglish.homeworkmgr.data.HWPeriodicalData;
import kr.co.smenglish.homeworkmgr.data.HomeworkOptionData;
import kr.co.smenglish.homeworkmgr.data.UserData;
import kr.co.smenglish.servermgr.Constants;

import org.apache.log4j.Logger;

/**
 * 정기간행물에 대한 자동숙제를 출제 한다.
 * @author sgyou
 *
 */
public class HomeworkMng {

	static Logger logger = Logger.getLogger(CouponServer.class);
	private DAS3 dasTransaction = null;
	private Connection conn = null;
	
	/**
	 * 정기간행물에 대한 자동숙제를 처리 한다.
	 */
	public void giveHomework(){
		try {
			dasTransaction = new DAS3(Constants.DAS_CONFIG);
			
			Hashtable<String, ArrayList<UserData>> htClassUser = new Hashtable<String, ArrayList<UserData>>();
			Hashtable<String, Integer> htClassBookRawid = new Hashtable<String, Integer>();
			
			ArrayList<HWPeriodicalData> alHWPeriodical = this.getHWPeriodicalDataAtToday();
			for (HWPeriodicalData hpd : alHWPeriodical) {
				
				Calendar cal = Calendar.getInstance();
				// 출제요일인지 체크
				if (hpd.ContainsWeekday(cal.get(Calendar.DAY_OF_WEEK)) == false)
					continue;
				
				try {
					this.conn = dasTransaction.beginTrans();
					if (hpd.getClassRawid() == 131233){
						System.out.println("xxxx");
					}
					
					// 학습일이 오늘인 contents를 가져온다.
					ArrayList<ContentsData> alContentsData = this.getContentsByStudyDate(hpd.getSeriesCd());
					for (ContentsData ctd : alContentsData) {
						ArrayList<UserData> alUserData = this.getUserListByClassRawId(hpd.getClassRawid(), htClassUser);
						if (alUserData.size() <= 0) 
							continue;
						
						int classBookLinkRawid = this.getClassBookLinkRawid(hpd.getAcademyRawid(), hpd.getClassRawid(), ctd.getUnitLevel(), htClassBookRawid);
						if (classBookLinkRawid < 0) 
							continue;
						
						int classHomeworkRawid = this.addCommonHomeworkInClass(hpd, ctd, classBookLinkRawid);
						if (classHomeworkRawid > 0){
							if (this.batchAddUserHomework(alUserData, hpd, ctd, classBookLinkRawid, classHomeworkRawid) > 0){
							}
						}
					}
				} catch (Exception e) {
					logger.error("------------------------------------------------------------");
					logger.error(String.format("[Rawid:%s][Series:%s][Class:%s] 저장 실패!!!", hpd.getRawid(), hpd.getSeriesCd(), hpd.getClassRawid()));
					logger.error(e);
					logger.error("------------------------------------------------------------");
					if (dasTransaction != null) try { dasTransaction.rollback(this.conn); } catch(Exception ex) {}
				} finally{
					if (dasTransaction != null) try { dasTransaction.commit(this.conn); } catch(Exception ex) {}
				}
			}
		} catch (Exception e) {
			logger.error(e);
		} finally{
		}
	}
	
	/**
	 * 금일 출제될 숙제 데이타를 가져온다.
	 * @return
	 */
	@SuppressWarnings("unchecked")
	private ArrayList<HWPeriodicalData> getHWPeriodicalDataAtToday(){
		LinkedList llRowset = null;
		
		ArrayList<HWPeriodicalData> alHWPeriodicalData = new ArrayList<HWPeriodicalData>();
		try {
			String sql = "SELECT /* kr.co.smenglish.homeworkmgr.HomeworkMng.getHWPeriodicalDataAtToday() */ " +
					     "       hpt.* " +
					     "       ,(select series_cd from series_mst where rawid = hpt.series_rawid) series_cd " +
					     "  FROM hw_periodical_trx hpt " +
					     " INNER JOIN class_mst cm " +
					     "         ON cm.rawid = hpt.class_rawid " +
					     "        AND cm.del_yn = 'N' " +
					     " INNER JOIN PRODUCT_LINK_MST pl " +
					     "		   ON pl.series_rawid = hpt.series_rawid " +
					     " INNER JOIN product_mst pm " +
					     "		   ON pm.rawid=pl.product_rawid " +
					     " INNER JOIN product_group_mst pg " +
					     " 		   ON pg.product_rawid=pm.rawid " +
					     " INNER JOIN academy_mst am " +
					     "		   ON am.product_group_cd = pg.product_group_cd " +
					     "        AND am.rawid = cm.academy_rawid " +
					     " WHERE hpt.start_dt <= sysdate " +
					     "   AND hpt.end_dt > sysdate " +
					     "   AND hpt.del_yn = 'N' ";
			
			LinkedList keyList = new LinkedList();
			llRowset = this.dasTransaction.select(null, sql, keyList);
			
			DASAssistant2<HWPeriodicalData> daHPD = new DASAssistant2<HWPeriodicalData>(llRowset, HWPeriodicalData.class);
			DASAssistant2<HomeworkOptionData> daHOD = new DASAssistant2<HomeworkOptionData>(llRowset, HomeworkOptionData.class);
			
			for (int i = 0; i < llRowset.size(); i++) {
				LinkedHashMap lhmRow = (LinkedHashMap)llRowset.get(i);
				
				HWPeriodicalData hpd = new HWPeriodicalData();
				HomeworkOptionData hod = hpd.getHomeworkOptionData();
				
				daHPD.setValueToBean(hpd, lhmRow);
				daHOD.setValueToBean(hod, lhmRow);
				
				alHWPeriodicalData.add(hpd);
			}
			
		} catch (Exception e) {
			logger.warn(e);
		} finally{
		}
		
		return alHWPeriodicalData;
	}
	
	/**
	 * 시리즈에 금일 기사(contents)를 가져온다.
	 * @param seriesCode
	 * @return
	 */
	@SuppressWarnings({"unchecked" })
	private ArrayList<ContentsData> getContentsByStudyDate(String seriesCode){
		LinkedList llRowset = null;
		
		ArrayList<ContentsData> alContentsData = new ArrayList<ContentsData>();
		try {
			String sql = "SELECT /* kr.co.smenglish.homeworkmgr.HomeworkMng.getContentsByStudyDate() */ " +
					     "       cm.rawid, cm.category_cd, cm.sub_category_cd, cm.unit_level, cm.unit_order, " +
					     "       cm.title, cm.study_type, cm.del_yn, cm.study_start_dt " +
					     "  FROM contents_mst cm " +
					     " WHERE cm.sub_category_cd = ? " +
					     "   AND cm.study_start_dt >= to_date(to_char(sysdate, 'yyyy-mm-dd'), 'yyyy-mm-dd') " +
					     "   AND cm.study_start_dt <  to_date(to_char(sysdate, 'yyyy-mm-dd'), 'yyyy-mm-dd') + 1 ";
			
			
			LinkedList keyList = new LinkedList();
			keyList.add(seriesCode);
			llRowset = this.dasTransaction.select(this.conn, sql, keyList);
			
			DASAssistant2<ContentsData> daCtd = new DASAssistant2<ContentsData>(llRowset, ContentsData.class);
			alContentsData = daCtd.getList();			
		} catch (Exception e) {
			logger.warn(e);
		} finally{
		}
		
		return alContentsData;
	}

	/**
	 * 해당 반의 모든 학생을 가져온다.
	 * @param classRawid
	 * @param htClassUser
	 * @return
	 */
	private ArrayList<UserData> getUserListByClassRawId(int classRawid, Hashtable<String, ArrayList<UserData>> htClassUser){
		ArrayList<UserData> alUserData = null;
		
		String keyClassRawid = classRawid + "";
		if (htClassUser.containsKey(keyClassRawid))
			alUserData = htClassUser.get(keyClassRawid);
		else{
			alUserData = this.getUserListByClassRawId(classRawid);
			htClassUser.put(keyClassRawid, alUserData);
		}
		
		return alUserData;
	}
	
	/**
	 * 해당 반의 모든 학생을 가져온다.
	 * @param seriesCode
	 * @return
	 */
	@SuppressWarnings({"unchecked" })
	private ArrayList<UserData> getUserListByClassRawId(int classRawid){
		LinkedList llRowset = null;
		
		ArrayList<UserData> alUserData = new ArrayList<UserData>();
		try {
			String sql = "SELECT /* kr.co.smenglish.homeworkmgr.HomeworkMng.getUserListByClassRawId() */ " +
					     "       um.rawid, um.academy_rawid, um.name, um.user_id, um.user_status_cd, " +
					     "       um.del_dt, um.end_dt, um.del_yn, um.grade_cd, um.text_book_cd," +
					     "       um.grade_class_name, um.cp_apply_yn, um.cp_apply_dt " +
					     "  FROM class_user_link_mst culm " +
					     " INNER JOIN user_mst um " +
					     "         ON um.rawid = culm.user_rawid " +
					     " WHERE culm.class_rawid = ? ";
			
			LinkedList keyList = new LinkedList();
			keyList.add(classRawid + "");
			llRowset = this.dasTransaction.select(this.conn, sql, keyList);
			
			DASAssistant2<UserData> daCtd = new DASAssistant2<UserData>(llRowset, UserData.class);
			alUserData = daCtd.getList();			
		} catch (Exception e) {
			logger.warn(e);
		} finally{
		}
		
		return alUserData;
	}

	/**
	 * CLASS_BOOK_LINK_MST의 키를 가져온다.
	 * @param academyRawid
	 * @param classRawid
	 * @param bookCode
	 * @param htClassBookRawid
	 * @return
	 * @throws Exception
	 */
	private int getClassBookLinkRawid(int academyRawid, int classRawid, String bookCode, Hashtable<String, Integer> htClassBookRawid) throws Exception {
		int classBookLinkRawid = -1;
		String key= String.format("%s-%s", classRawid, bookCode);
		if (htClassBookRawid.containsKey(key))
			classBookLinkRawid = htClassBookRawid.get(key);
		else {
			classBookLinkRawid = this.getClassBookLinkRawid(academyRawid,classRawid, bookCode);
			htClassBookRawid.put(key, classBookLinkRawid);
		}

		return classBookLinkRawid;
	}

	/**
	 * 
	 * @param academyRawid
	 * @param classRawid
	 * @param bookCode
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private int getClassBookLinkRawid(int academyRawid, int classRawid, String bookCode) throws Exception {

		int classBookLinkRawid = -1;
		
		String sql = "SELECT /* kr.co.smenglish.homeworkmgr.HomeworkMng.getClassBookLinkRawid() */ " +
				     "       cblm.rawid " +
				     "  FROM class_book_link_mst cblm " +
				     " INNER JOIN book_mst bm " +
				     "         ON bm.rawid = cblm.book_rawid " +
				     "        AND bm.book_cd = ? " +
				     "        AND bm.del_yn = 'N' " +
				     " WHERE cblm.academy_rawid = ? " +
				     "   AND cblm.class_rawid = ? " +
				     "   AND cblm.start_dt <= sysdate " +
				     "   AND cblm.end_dt > sysdate ";
		
		LinkedList keyList = new LinkedList();
		keyList.add(bookCode);
		keyList.add(academyRawid + "");
		keyList.add(classRawid + "");
		
		LinkedList llRowset = this.dasTransaction.select(this.conn, sql, keyList);
			
		if (llRowset.size() > 0){
			classBookLinkRawid = Integer.parseInt(((LinkedHashMap)llRowset.get(0)).get("rawid").toString());
		}
		
		return classBookLinkRawid;
	}
	
//	/**
//	 * 공용 숙제를 제출한다.
//	 * @param hpd
//	 * @param cd
//	 * @param classBookLinkRawid
//	 * @return 저장된 rawid를 리턴하다.
//	 * @throws Exception
//	 */
//	private int batchAddClassHomework(HWPeriodicalData hpd, ContentsData cd, int classBookLinkRawid) throws Exception {
//		LinkedList<LinkedHashMap<String, String>> llColSet = new LinkedList<LinkedHashMap<String, String>>();
//		
//		if (this.existHomeworkInClass(academyRawid, classRawid, seriesCode, contentsRawid))
//		
//		int classHomeworkRawid = this.getRawidOfClassHomeworkTrx();
//		
//		
//	}
	
	private int getRawidOfClassHomeworkTrx() throws Exception {
		return this.getSeqNumber("seq_class_homework_trx");
	}
	
	/**
	 * 키(RAWID)를 가져온다.
	 * @param seqName
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private int getSeqNumber(String seqName) throws Exception {
		String sql = "SELECT " + seqName + ".nextval rawid " +
				     "  FROM dual ";

		LinkedList keyList = new LinkedList();
		LinkedList llRowset = this.dasTransaction.select(this.conn, sql, keyList);
		LinkedHashMap lhmRow = (LinkedHashMap)llRowset.get(0);
		
		return Integer.parseInt(lhmRow.get("rawid").toString());
	}

	/**
	 * 학생에게 숙제를 제출한다. 
	 * @param alUserData
	 * @param hpd
	 * @param cd
	 * @param classBookLinkRawid
	 * @param classHomeworkRawid
	 * @return
	 */
	private int batchAddUserHomework(
			ArrayList<UserData> alUserData, HWPeriodicalData hpd, ContentsData cd,
			int classBookLinkRawid, int classHomeworkRawid) throws Exception {
		
		LinkedList<LinkedHashMap<String, Object>> llColSet = new LinkedList<LinkedHashMap<String, Object>>();
			
		int xx = 0;
		for (UserData ud : alUserData) {
			// 숙제가 존재하면 더이상 처리 않함.
			if (this.existHomeworkInUser(ud.getRawid(), hpd.getClassRawid(), cd.getRawid(), classHomeworkRawid))
				continue;
			
			LinkedHashMap<String, Object> lhmRowset = new LinkedHashMap<String, Object>();
			lhmRowset.put("RAWID", this.getRawidForHomework() + "");
			lhmRowset.put("CATEGORY_CD", cd.getCategoryCd());
			lhmRowset.put("SUB_CATEGORY_CD", cd.getSubCategoryCd());
			lhmRowset.put("TITLE", cd.getTitle());
			lhmRowset.put("USER_RAWID", ud.getRawid() + "");
			lhmRowset.put("CONTENTS_RAWID", cd.getRawid() + "");
			lhmRowset.put("CLASS_BOOK_LINK_MST_RAWID", classBookLinkRawid + "");
			lhmRowset.put("CLASS_HOMEWORK_TRX_RAWID", classHomeworkRawid + "");
			lhmRowset.put("PROGRESS", "0");
			lhmRowset.put("CREATE_DT+SYSDATE", null);
			lhmRowset.put("START_DT", hpd.getStartDt());
			lhmRowset.put("END_DT", SimpleDateCtrl.addMonth(hpd.getStartDt(), 1));
			lhmRowset.put("SUBJECT_DT", SimpleDateCtrl.addDay(cd.getStudyStartDt(), hpd.getHwPeriod()));
			
			hpd.MakeHomeworkRowSet(lhmRowset);
			
			llColSet.add(lhmRowset);
		}
		this.dasTransaction.batchInsert(this.conn, "USER_HOMEWORK_TRX", llColSet);
		
		return llColSet.size();
	}
	
	/**
	 * 이용자에게 지난 한달간 컨텐츠 숙제가 출제되었는지 확인한다.
	 * 
	 * @param userRawid
	 * @param classRawid
	 * @param contentsRawid
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings("unchecked")
	private boolean existHomeworkInUser(int userRawid, int classRawid, int contentsRawid, int classHomeworkRawid) throws Exception {
		LinkedList llRowset = null;
		
		String sql = "SELECT /* kr.co.smenglish.homeworkmgr.HomeworkMng.existHomeworkInUser() */ " +
				     "       uht.rawid " +
				     "  FROM user_homework_trx uht " +
				     " WHERE uht.user_rawid = ? " +
				     "   AND uht.class_rawid = ? " +
				     "   AND uht.contents_rawid = ? " +
				     "   AND uht.subject_dt >= sysdate - 30 " +
				     "   AND uht.subject_dt < sysdate + 7 " +
				     "   AND uht.del_yn = 'N' " +
				     "   AND uht.start_dt >= sysdate - 40 " +
				     "   AND uht.start_dt < sysdate + 7 " +
				     "   AND uht.class_homework_trx_rawid = ? ";
		
		
		LinkedList keyList = new LinkedList();
		keyList.add(userRawid + "");
		keyList.add(classRawid + "");
		keyList.add(contentsRawid + "");
		keyList.add(classHomeworkRawid + "");
		llRowset = this.dasTransaction.select(this.conn, sql, keyList);

		if (llRowset.isEmpty())
			return false;
		else 
			return true;
	}
//	private boolean existHomeworkInUser(int userRawid, int classRawid, int contentsRawid) throws Exception {
//		LinkedList llRowset = null;
//		
//		String sql = "SELECT /* kr.co.smenglish.homeworkmgr.HomeworkMng.existHomeworkInUser() */ " +
//				     "       uht.rawid " +
//				     "  FROM user_homework_trx uht " +
//				     " WHERE uht.user_rawid = ? " +
//				     "   AND uht.class_rawid = ? " +
//				     "   AND uht.contents_rawid = ? " +
//				     "   AND uht.subject_dt >= sysdate - 30 " +
//				     "   AND uht.subject_dt < sysdate + 7 " +
//				     "   AND uht.del_yn = 'N' " +
//				     "   AND uht.start_dt >= sysdate - 40 " +
//				     "   AND uht.start_dt < sysdate + 7 ";
//		
//		
//		LinkedList keyList = new LinkedList();
//		keyList.add(userRawid + "");
//		keyList.add(classRawid + "");
//		keyList.add(contentsRawid + "");
//		llRowset = this.dasTransaction.select(this.conn, sql, keyList);
//
//		if (llRowset.isEmpty())
//			return false;
//		else 
//			return true;
//	}
	
	private int getRawidForHomework() throws Exception {
		return this.getSeqNumber("seq_user_homework_trx");
	}
	
	private int addCommonHomeworkInClass(HWPeriodicalData hpd, ContentsData cd, int classBookLinkRawid) throws Exception {
		LinkedList<LinkedHashMap<String, Object>> llColSet = new LinkedList<LinkedHashMap<String, Object>>();
		
		int existRawid = this.existCommonHomeworkInClass(hpd.getAcademyRawid(), hpd.getClassRawid(), cd.getSubCategoryCd(), cd.getRawid()); 
		if (existRawid > 0) // 존재하므로 리턴
			return existRawid;
		
		int classHomeworkRawid = this.getRawidOfClassHomeworkTrx();
		
		LinkedHashMap<String, Object> lhmRowset = new LinkedHashMap<String, Object>();
		lhmRowset.put("RAWID", classHomeworkRawid + "");
		lhmRowset.put("ACADEMY_RAWID", hpd.getAcademyRawid() + "");
		lhmRowset.put("CATEGORY_CD", cd.getCategoryCd() + "");
		lhmRowset.put("SUB_CATEGORY_CD", cd.getSubCategoryCd() + "");
		lhmRowset.put("CLASS_BOOK_LINK_MST_RAWID", classBookLinkRawid + "");
		lhmRowset.put("TITLE", cd.getTitle() + "");
		lhmRowset.put("CONTENTS_RAWID", cd.getRawid() + "");
		lhmRowset.put("CREATE_DT+SYSDATE", null);
		//lhmRowset.put("SUBJECT_DT", SimpleDateCtrl.convertDateToStr(SimpleDateCtrl.addDay(hpd.getStartDt(), hpd.getHwPeriod())));
		lhmRowset.put("SUBJECT_DT", SimpleDateCtrl.addDay(cd.getStudyStartDt(), hpd.getHwPeriod()));
		
		hpd.MakeHomeworkRowSet(lhmRowset);
		
		lhmRowset.remove("USER_TYPE");
		lhmRowset.remove("TUTOR_RAWID");
		
		llColSet.add(lhmRowset);
				
		this.dasTransaction.batchInsert(this.conn, "CLASS_HOMEWORK_TRX", llColSet);
			
		return classHomeworkRawid;
	}
	
	/**
	 * 공용 숙제가 있는지 없는지?
	 * @param academyRawid
	 * @param classRawid
	 * @param seriesCode
	 * @param contentsRawid
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked" })
	private int existCommonHomeworkInClass(int academyRawid, int classRawid, String seriesCode, int contentsRawid) throws Exception {
		LinkedList llRowset = null;
		
		String sql = "SELECT /* kr.co.smenglish.homeworkmgr.HomeworkMng.existHomeworkInClass() */ " +
				     "       cht.rawid " +
				     "  FROM class_homework_trx cht " +
				     " WHERE cht.academy_rawid = ? " +
				     "   AND cht.class_rawid = ? " +
				     "   AND cht.sub_category_cd = ? " +
				     "   AND cht.contents_rawid = ? " +
				     "   AND cht.subject_dt >= sysdate - 30 " +
				     "   AND cht.subject_dt < sysdate + 30 " +
				     "   AND cht.create_dt >= sysdate - 30 " +
				     "   AND cht.create_dt < sysdate + 30 " +
				     "   AND cht.del_yn = 'N' ";
		
		LinkedList keyList = new LinkedList();
		keyList.add(academyRawid + "");
		keyList.add(classRawid + "");
		keyList.add(seriesCode);
		keyList.add(contentsRawid + "");
		llRowset = this.dasTransaction.select(this.conn, sql, keyList);

		if (llRowset.isEmpty())
			return -1;
		else {
			return Integer.parseInt(((LinkedHashMap)llRowset.get(0)).get("rawid").toString());
		}
	}
	
}
