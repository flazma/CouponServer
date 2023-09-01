package kr.co.smenglish.vitalinkmgr;

import java.sql.Connection;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import kr.co.smenglish.coreclone.util.DASAssistant2;
import kr.co.smenglish.couponmgr.CouponServer;
import kr.co.smenglish.das.sql.DAS3;
import kr.co.smenglish.servermgr.Constants;

import org.apache.log4j.Logger;

public class VitaLinkMng {
	static Logger logger = Logger.getLogger(CouponServer.class);
	private DAS3 vitaDas3 = new DAS3(Constants.DAS_CONFIG);
	private DAS3 olyDas3 = new DAS3("OLY");
	private Connection conn = null;
	
	public void olympiadLink(){
		try {
			//학생 insert
			String iStudent = insertStudent(getOlyList("V_VITA_STUDENT"));
			
			//강사 insert
			String iTutor = insertTutor(getOlyList("V_VITA_EMPLOYEE"));
			
			//2012-09-04, 수정등록한다고 주석처리 요청함.
			//반정보  insert
//			String iClass = insertClass(getOlyList("V_VITA_CLASS"));
			
			//반-학생 insert
//			String iClassUser = insertClassUser(getOlyList("V_VITA_CLASS_STUDENT"));
			
			//반-학생 변경 이력(삭제건 추가)
//			String dClassUser = deleteClassUser(getOlyListHist());
			
			logger.info("---- insert Count : iStudent:"+iStudent);
			logger.info("---- insert Count : iTutor:"+iTutor);
//			logger.info("---- insert Count : iClass:"+iClass);
//			logger.info("---- insert Count : iClassUser:"+iClassUser);
//			logger.info("---- insert Count : dClassUser:"+dClassUser);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	//학생,강사 변경 정보 반영(매 한시간마다 확인)
	public void olympiadLink2(){
		try {
			//학생 insert
			String iStudent = insertStudent(getOlyList("V_VITA_STUDENT"));
			
			//강사 insert
			String iTutor = insertTutor(getOlyList("V_VITA_EMPLOYEE"));
			
			logger.info("---- insert Count : iStudent:"+iStudent);
			logger.info("---- insert Count : iTutor:"+iTutor);
			
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	/**
	 * 올림피아드 db 조회
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked" })
	private ArrayList<VitaLink> getOlyList(String tableName) throws Exception {	
		LinkedList rowset = null;
		StringBuffer query = new StringBuffer(32);
		
		query.append("/* use:WEB; method-name:" + this.getClass().getName() + "올림피아드 db 조회*/");
		query.append("\n   SELECT * FROM "+tableName);
		//query.append("\n    WHERE CONVERT(DATETIME, MODDATE) > CONVERT(DATETIME,DATEADD(dd,-2,GETDATE()))");
		
		rowset = olyDas3.select(null, query.toString(), null);
		
		DASAssistant2<VitaLink> vitalink = new DASAssistant2<VitaLink>(rowset, VitaLink.class);
		ArrayList<VitaLink> list = vitalink.getList();		
		
		return list;
	}
	
	/**
	 * 올림피아드 반-학생 변경 이력 조회
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked" })
	private ArrayList<VitaLink> getOlyListHist() throws Exception {	
		LinkedList rowset = null;
		StringBuffer query = new StringBuffer(32);
		
		query.append("/* use:WEB; method-name:" + this.getClass().getName() + "올림피아드 db 조회*/");
		query.append("\n   SELECT * FROM V_VITA_CLASS_STUDENT_HIST WHERE END_DATE IS NOT NULL ");
		//query.append("\n    AND CONVERT(DATETIME, MODDATE) > CONVERT(DATETIME,DATEADD(dd,-2,GETDATE()))");
		
		rowset = olyDas3.select(null, query.toString(), null);
		
		DASAssistant2<VitaLink> vitalink = new DASAssistant2<VitaLink>(rowset, VitaLink.class);
		ArrayList<VitaLink> list = vitalink.getList();		
		
		return list;
	}
	
	/**
	 * 학생 등록
	 * @param student
	 * @return
	 * @throws Exception 
	 */
	@SuppressWarnings({ "unchecked" })
	private String insertStudent(ArrayList<VitaLink> student){
		String result = "";
		LinkedList<LinkedHashMap<String, Object>> insertList = new LinkedList<LinkedHashMap<String, Object>>();
		LinkedList<LinkedHashMap<Object, Object>> updateList = new LinkedList<LinkedHashMap<Object, Object>>();
		LinkedList<LinkedList<String>> updateDataList = new LinkedList<LinkedList<String>>();
		
		try {
			this.conn = vitaDas3.beginTrans();
			
			for (VitaLink list : student){
				//학생 중복확인 > insert
				if(existStudentTutor("USER_MST",list.getUserid())==-1){
					
					LinkedHashMap<String, Object> insertMap = new LinkedHashMap<String, Object>();
					String user_rawid = selectKey("rawid","SELECT USER_MST_SEQ.NEXTVAL RAWID FROM DUAL");
					String academy_rawid = selectKey("rawid","SELECT RAWID FROM ACADEMY_MST WHERE USER_ID = '"+list.getAcademyid()+"'");
					
					insertMap.put("RAWID", user_rawid);
					insertMap.put("ACADEMY_RAWID", academy_rawid);
					insertMap.put("NAME",list.getName().trim());
					insertMap.put("USER_ID",list.getUserid());
					insertMap.put("PASSWD", list.getPasswd());
					insertMap.put("ENNAME", list.getEnname());
					insertMap.put("TELNUM",list.getTelnum());
					insertMap.put("SHELNUM",list.getShelnum());
					insertMap.put("PARENTNUM", list.getParentnum());
					insertMap.put("EMAIL",list.getEmail());
					insertMap.put("DEL_YN","N");
					insertMap.put("USER_STATUS_CD","01");
					
					insertList.add(insertMap);
				}else{
					int rawid = existStudentTutorPW("USER_MST",list.getAcademyid(),list.getUserid(),list.getPasswd());
					
					if(rawid==-1){
						String academy_rawid = selectKey("rawid","SELECT RAWID FROM ACADEMY_MST WHERE USER_ID = '"+list.getAcademyid()+"'");
						LinkedHashMap<Object, Object> updateMap = new LinkedHashMap<Object, Object>();
						LinkedList<String> tempList = new LinkedList<String>();
						
						updateMap.put("ACADEMY_RAWID", academy_rawid);
						updateMap.put("PASSWD", list.getPasswd());
						tempList.add(list.getUserid());
						
						updateList.add(updateMap);
						updateDataList.add(tempList);
					}
				}
			}
			
			logger.info("---- student update :"+updateDataList);
		
			int[] iBatch = vitaDas3.batchInsert(this.conn, "USER_MST", insertList);
			int[] iBatchUp = vitaDas3.batchUpdate(this.conn, "USER_MST", updateList, "WHERE USER_ID = ?", updateDataList);
			
			result = "select-"+student.size()+":insert-"+insertList.size()+":update-"+updateDataList.size();
			
		} catch (Exception e) {
			logger.error(e);
			if (vitaDas3 != null) try { vitaDas3.rollback(this.conn); } catch(Exception ex) {}
		} finally{
			if (vitaDas3 != null) try { vitaDas3.commit(this.conn); } catch(Exception ex) {}
		}
		
		return result;
	}
	
	/**
	 * 강사 등록
	 * @param tutor
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked" })
	private String insertTutor(ArrayList<VitaLink> tutor){
		String result = "";
		LinkedList<LinkedHashMap<String, Object>> insertList = new LinkedList<LinkedHashMap<String, Object>>();
		LinkedList<LinkedHashMap<Object, Object>> updateList = new LinkedList<LinkedHashMap<Object, Object>>();
		LinkedList<LinkedList<String>> updateDataList = new LinkedList<LinkedList<String>>();
		
		try {
			this.conn = vitaDas3.beginTrans();
			
			for (VitaLink list : tutor){
				//강사 중복확인 > insert
				if(existStudentTutor("TUTOR_MST",list.getUserid())==-1){
					LinkedHashMap<String, Object> insertMap = new LinkedHashMap<String, Object>();
					String tutor_rawid = selectKey("rawid","SELECT TUTOR_MST_SEQ.NEXTVAL RAWID FROM DUAL");
					String academy_rawid = selectKey("rawid","SELECT RAWID FROM ACADEMY_MST WHERE USER_ID = '"+list.getAcademyid()+"'");
					
					insertMap.put("RAWID", tutor_rawid);
					insertMap.put("ACADEMY_RAWID", academy_rawid);
					insertMap.put("NAME",list.getName().trim());
					insertMap.put("USER_ID",list.getUserid());
					insertMap.put("PASSWD", list.getPasswd());
					insertMap.put("EMAIL",list.getEmail());
					insertMap.put("DEL_YN","N");
					insertMap.put("TUTOR_STATUS_CD","01");
					insertMap.put("TUTOR_AUTH_CD","00");
					
					insertList.add(insertMap);
				}else{
					int rawid = existStudentTutorPW("TUTOR_MST",list.getAcademyid(),list.getUserid(),list.getPasswd());
					
					if(rawid==-1){
						LinkedHashMap<Object, Object> updateMap = new LinkedHashMap<Object, Object>();
						LinkedList<String> tempList = new LinkedList<String>();
						String academy_rawid = selectKey("rawid","SELECT RAWID FROM ACADEMY_MST WHERE USER_ID = '"+list.getAcademyid()+"'");
						
						updateMap.put("ACADEMY_RAWID", academy_rawid);
						updateMap.put("PASSWD", list.getPasswd());
						tempList.add(list.getUserid());
						
						updateList.add(updateMap);
						updateDataList.add(tempList);
					}
				}
			}
			
			logger.info("---- tutor update :"+updateDataList);
		
			int[] iBatch = vitaDas3.batchInsert(this.conn, "TUTOR_MST", insertList);
			int[] iBatchUp = vitaDas3.batchUpdate(this.conn, "TUTOR_MST", updateList, "WHERE USER_ID = ?", updateDataList);
			
			result = "select-"+tutor.size()+":insert-"+insertList.size()+":update-"+updateDataList.size();
		} catch (Exception e) {
			logger.error(e);
			if (vitaDas3 != null) try { vitaDas3.rollback(this.conn); } catch(Exception ex) {}
		} finally{
			if (vitaDas3 != null) try { vitaDas3.commit(this.conn); } catch(Exception ex) {}
		}
		
		return result;
	}
	
	/**
	 * 반 등록
	 * @param Class
	 * @return
	 */
	@SuppressWarnings({ "unchecked" })
	private String insertClass(ArrayList<VitaLink> Class){
		String result = "";
		LinkedList<LinkedHashMap<String, Object>> insertList = new LinkedList<LinkedHashMap<String, Object>>();
		
		try {
			this.conn = vitaDas3.beginTrans();
			
			for (VitaLink list : Class){
				//반 중복확인 > insert
				if(existClass(list.getAcademyid(),list.getUserid(),list.getClassName())==-1){
					LinkedHashMap<String, Object> insertMap = new LinkedHashMap<String, Object>();
					String class_rawid = selectKey("rawid","SELECT CLASS_MST_SEQ.NEXTVAL RAWID FROM DUAL");
					String academy_rawid = selectKey("rawid","SELECT RAWID FROM ACADEMY_MST WHERE USER_ID = '"+list.getAcademyid()+"'");
					String tutor_rawid = selectKey("rawid","SELECT RAWID FROM TUTOR_MST WHERE USER_ID = '"+list.getUserid()+"'");
					
					if(!academy_rawid.isEmpty()){
						insertMap.put("RAWID", class_rawid);
						insertMap.put("ACADEMY_RAWID", academy_rawid);
						insertMap.put("TUTOR_RAWID", tutor_rawid);
						insertMap.put("CLASS_NAME",list.getClassName().trim());
						insertMap.put("GRADE_CD",list.getGradeCd());
						
						insertList.add(insertMap);
					}
				}
			}
			
			int[] iBatch = vitaDas3.batchInsert(this.conn, "CLASS_MST", insertList);
			result = "select-"+Class.size()+":insert-"+insertList.size();
		} catch (Exception e) {
			logger.error(e);
			if (vitaDas3 != null) try { vitaDas3.rollback(this.conn); } catch(Exception ex) {}
		} finally{
			if (vitaDas3 != null) try { vitaDas3.commit(this.conn); } catch(Exception ex) {}
		}
		
		return result;
	}
	
	/**
	 * 반-학생 등록
	 * @param ClassUser
	 * @return
	 */
	@SuppressWarnings({ "unchecked" })
	private String insertClassUser(ArrayList<VitaLink> ClassUser){
		String result = "";
		LinkedList<LinkedHashMap<String, Object>> insertMstList = new LinkedList<LinkedHashMap<String, Object>>();
		LinkedList<LinkedHashMap<String, Object>> insertTrxList = new LinkedList<LinkedHashMap<String, Object>>();
		
		try {
			this.conn = vitaDas3.beginTrans();
			
			for (VitaLink list : ClassUser){
				//반-학생 중복확인 > insert
				String academy_rawid = selectKey("rawid","SELECT RAWID FROM ACADEMY_MST WHERE USER_ID = '"+list.getAcademyid()+"'");
				String class_rawid = selectKey("rawid","SELECT RAWID FROM CLASS_MST WHERE CLASS_NAME = '"+list.getClassName()+"' AND ACADEMY_RAWID = "+academy_rawid);
				String user_rawid = selectKey("rawid","SELECT RAWID FROM USER_MST WHERE USER_ID = '"+list.getUserid()+"' AND ACADEMY_RAWID = "+academy_rawid);
				if(existClassUser(class_rawid,user_rawid)==-1){
					LinkedHashMap<String, Object> insertMstMap = new LinkedHashMap<String, Object>();
					LinkedHashMap<String, Object> insertTrxMap = new LinkedHashMap<String, Object>();
					
					if(!class_rawid.isEmpty()&&!user_rawid.isEmpty()){
						//CLASS_USER_LINK_MST insert
						insertMstMap.put("RAWID+SEQ_CLASS_USER_LINK_MST.NEXTVAL", null);
						insertMstMap.put("CLASS_RAWID", class_rawid);
						insertMstMap.put("USER_RAWID", user_rawid);
						
						//CLASS_USER_LINK_TRX insert
						insertTrxMap.put("RAWID+SEQ_CLASS_USER_LINK_TRX.NEXTVAL", null);
						insertTrxMap.put("CLASS_RAWID", class_rawid);
						insertTrxMap.put("USER_RAWID", user_rawid);
						insertTrxMap.put("CREATE_DT+SYSDATE", null);
						insertTrxMap.put("CREATE_BY", academy_rawid);
						insertTrxMap.put("USER_TYPE_CD", 'A');
						insertTrxMap.put("WORK_TYPE_CD", 'C');
						
						insertMstList.add(insertMstMap);
						insertTrxList.add(insertTrxMap);
					}
				}
			}
			
			int[] iMstBatch = vitaDas3.batchInsert(this.conn, "CLASS_USER_LINK_MST", insertMstList);
			int[] iTrxBatch = vitaDas3.batchInsert(this.conn, "CLASS_USER_LINK_TRX", insertTrxList);
			result = "select-"+ClassUser.size()+":insertMst-"+insertMstList.size()+":insertTrx-"+insertTrxList.size();
		} catch (Exception e) {
			logger.error(e);
			if (vitaDas3 != null) try { vitaDas3.rollback(this.conn); } catch(Exception ex) {}
		} finally{
			if (vitaDas3 != null) try { vitaDas3.commit(this.conn); } catch(Exception ex) {}
		}
		
		return result;
	}
	
	/**
	 * 반-학생 변경 이력(삭제건 추가)
	 * @param ClassUser
	 * @return
	 */
	private String deleteClassUser(ArrayList<VitaLink> ClassUser){
		String result = "";
		LinkedList<LinkedHashMap<String, Object>> insertList = new LinkedList<LinkedHashMap<String, Object>>();
		LinkedList<LinkedList<Object>> deleteList = new LinkedList<LinkedList<Object>>();
		
		try {
			this.conn = vitaDas3.beginTrans();
			
			for (VitaLink list : ClassUser){
				//반-학생 변경 확인 > delete
				String academy_rawid = selectKey("rawid","SELECT RAWID FROM ACADEMY_MST WHERE USER_ID = '"+list.getAcademyid()+"'");
				String class_rawid = selectKey("rawid","SELECT RAWID FROM CLASS_MST WHERE CLASS_NAME = '"+list.getClassName()+"' AND ACADEMY_RAWID = "+academy_rawid);
				String user_rawid = selectKey("rawid","SELECT RAWID FROM USER_MST WHERE USER_ID = '"+list.getUserid()+"' AND ACADEMY_RAWID = "+academy_rawid);
				
				int deleteRawid = existClassUser(class_rawid,user_rawid);
				if(deleteRawid!=-1){
					LinkedHashMap<String, Object> insertMap = new LinkedHashMap<String, Object>();
					LinkedList<Object> deleteTemp = new LinkedList<Object>();
					
					if(!class_rawid.isEmpty()&&!user_rawid.isEmpty()){
					//CLASS_USER_LINK_TRX insert
					insertMap.put("RAWID+SEQ_CLASS_USER_LINK_TRX.NEXTVAL", null);
					insertMap.put("CLASS_RAWID", class_rawid);
					insertMap.put("USER_RAWID", user_rawid);
					insertMap.put("CREATE_DT+SYSDATE", null);
					insertMap.put("CREATE_BY", academy_rawid);
					insertMap.put("USER_TYPE_CD", 'A');
					insertMap.put("WORK_TYPE_CD", 'D');
					
					//CLASS_USER_LINK_MST delete
					deleteTemp.add(deleteRawid);
					
					insertList.add(insertMap);
					deleteList.add(deleteTemp);
					}
				}
			}
			
			int[] iBatch = vitaDas3.batchInsert(this.conn, "CLASS_USER_LINK_TRX",insertList);
			int[] dBatch = vitaDas3.batchDelete(this.conn, "CLASS_USER_LINK_MST", "WHERE RAWID=?",deleteList);
			result = "select-"+ClassUser.size()+":delete-"+deleteList.size()+":insert-"+insertList.size();
		} catch (Exception e) {
			logger.error(e);
			if (vitaDas3 != null) try { vitaDas3.rollback(this.conn); } catch(Exception ex) {}
		} finally{
			if (vitaDas3 != null) try { vitaDas3.commit(this.conn); } catch(Exception ex) {}
		}
		
		return result;
	}
	
	/**
	 * sm 학생,강사 중복확인
	 * @param academy_userid
	 * @param userid
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked" })
	private int existStudentTutor(String tableName,String userid) throws Exception{
		LinkedList rowset = null;
		LinkedList dataList = new LinkedList();
		StringBuffer query = new StringBuffer(32);
		
		query.append("/* use:WEB; method-name:" + this.getClass().getName() + "중복확인*/");
		query.append("\n   SELECT UM.RAWID ");
		query.append("\n     FROM "+tableName+" UM ");
		query.append("\n    WHERE UM.USER_ID = ? ");
		
		dataList.add(userid);
		
		rowset = vitaDas3.select(null, query.toString(), dataList);

		if (rowset.isEmpty())
			return -1;
		else {
			return Integer.parseInt(((LinkedHashMap)rowset.get(0)).get("rawid").toString());
		}
	}
	
	/**
	 * 학원아이디,패스워드 변경 유무 확인
	 * @param tableName
	 * @param academy_userid
	 * @param userid
	 * @param passwd
	 * @return
	 * @throws Exception
	 */
	private int existStudentTutorPW(String tableName,String academy_userid, String userid, String passwd) throws Exception{
		LinkedList rowset = null;
		LinkedList dataList = new LinkedList();
		StringBuffer query = new StringBuffer(32);
		
		query.append("/* use:WEB; method-name:" + this.getClass().getName() + "중복확인*/");
		query.append("\n   SELECT UM.RAWID ");
		query.append("\n     FROM "+tableName+" UM,ACADEMY_MST AM ");
		query.append("\n    WHERE UM.ACADEMY_RAWID=AM.RAWID ");
		query.append("\n      AND AM.USER_ID = ? AND UM.USER_ID = ? AND UM.PASSWD = ?");
		
		dataList.add(academy_userid);
		dataList.add(userid);
		dataList.add(passwd);
		
		rowset = vitaDas3.select(null, query.toString(), dataList);

		if (rowset.isEmpty())
			return -1;
		else {
			return Integer.parseInt(((LinkedHashMap)rowset.get(0)).get("rawid").toString());
		}
	}
	
	/**
	 * sm 반중복확인
	 * @param academy_userid
	 * @param tutorid
	 * @param class_name
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked" })
	private int existClass(String academy_userid, String tutorid, String class_name) throws Exception{
		LinkedList rowset = null;
		LinkedList dataList = new LinkedList();
		StringBuffer query = new StringBuffer(32);
		
		query.append("/* use:WEB; method-name:" + this.getClass().getName() + "반 중복확인*/");
		query.append("\n   SELECT CM.RAWID ");
		query.append("\n     FROM CLASS_MST CM,ACADEMY_MST AM, TUTOR_MST TM ");
		query.append("\n    WHERE CM.ACADEMY_RAWID=AM.RAWID ");
		query.append("\n      AND AM.USER_ID = ? ");
		query.append("\n      AND TM.USER_ID = ? AND TM.ACADEMY_RAWID = AM.RAWID ");
		query.append("\n      AND CM.CLASS_NAME = ? ");
		
		dataList.add(academy_userid);
		dataList.add(tutorid);
		dataList.add(class_name);
		
		rowset = vitaDas3.select(null, query.toString(), dataList);

		if (rowset.isEmpty())
			return -1;
		else {
			return Integer.parseInt(((LinkedHashMap)rowset.get(0)).get("rawid").toString());
		}
	}
	
	/**
	 * 반-학생 중복확인
	 * @param class_rawid
	 * @param user_rawid
	 * @return
	 * @throws Exception
	 */
	@SuppressWarnings({ "unchecked" })
	private int existClassUser(String class_rawid, String user_rawid) throws Exception{
		LinkedList rowset = null;
		LinkedList dataList = new LinkedList();
		StringBuffer query = new StringBuffer(32);
		
		query.append("/* use:WEB; method-name:" + this.getClass().getName() + "반 중복확인*/");
		query.append("\n   SELECT RAWID ");
		query.append("\n     FROM CLASS_USER_LINK_MST ");
		query.append("\n    WHERE CLASS_RAWID = ? ");
		query.append("\n      AND USER_RAWID = ? ");
		
		dataList.add(class_rawid);
		dataList.add(user_rawid);
		
		rowset = vitaDas3.select(null, query.toString(), dataList);

		if (rowset.isEmpty())
			return -1;
		else {
			return Integer.parseInt(((LinkedHashMap)rowset.get(0)).get("rawid").toString());
		}
	}
	
	/**
	 * key 조회
	 * @param academyid
	 * @return
	 * @throws Exception 
	 */
	private String selectKey(String key, String query) throws Exception{
		LinkedList rowset = vitaDas3.select(null, query, null);
		
		if (rowset.isEmpty())
			return "";
		else {
			return ((LinkedHashMap)rowset.get(0)).get(key).toString();
		}
	}
}
