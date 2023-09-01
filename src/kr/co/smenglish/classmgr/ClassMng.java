package kr.co.smenglish.classmgr;

import java.sql.Connection;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Hashtable;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.apache.log4j.Logger;

import com.sun.corba.se.impl.orbutil.closure.Constant;

import kr.co.smenglish.classmgr.data.BookData;
import kr.co.smenglish.classmgr.data.ClassBookData;
import kr.co.smenglish.classmgr.data.ClassSeriesData;
import kr.co.smenglish.coreclone.util.DASAssistant2;
import kr.co.smenglish.couponmgr.AcademyCoupon;
import kr.co.smenglish.couponmgr.CouponHstEntity;
import kr.co.smenglish.couponmgr.CouponServer;
import kr.co.smenglish.couponmgr.UserInfo;
import kr.co.smenglish.das.sql.DAS;
import kr.co.smenglish.das.sql.DAS3;
import kr.co.smenglish.das.sql.RecordSet;
import kr.co.smenglish.homeworkmgr.data.UserData;
import kr.co.smenglish.servermgr.*;


/**
 * 서버에서 클래스를 관리한다.
 * 
 * 학급에 등록된 시리즈 사용기간을 가지고, 
 * 시리즈에 도서가 추가되었을 때 학급에 도서를 추가한다.
 * 
 * @author sgyou
 *
 */
public class ClassMng {

	static Logger logger = Logger.getLogger(CouponServer.class);
	private DAS3 dasTransaction = null; 
	private Connection conn = null;
	
	/**
	 * 학급-시리즈 테이블 조사하여 해당하는 기간의 도서를 학급-도서테이블에 추가한다.
	 */
	public void AddBookToClass(){
		try {
			dasTransaction = new DAS3(Constants.DAS_CONFIG);
	
			ArrayList<ClassSeriesData> alClassSeries = this.GetClassSeriesData();
			Hashtable<Integer, ArrayList<BookData>> htBookData = new Hashtable<Integer, ArrayList<BookData>>();
			
			for (ClassSeriesData csd : alClassSeries) {
				// 오늘 등록된 도서를 가져옴 (혹은 사용하기 시작한 도서)
				
				ArrayList<BookData> alNewBook = null;
				if (htBookData.containsKey(csd.getSeriesRawid()))
					alNewBook = htBookData.get(csd.getSeriesRawid());
				else{
					alNewBook = this.GetRegBooksBySeriesCode(csd.getSeriesRawid());
					htBookData.put( new Integer(csd.getSeriesRawid()), alNewBook);
				}

				this.conn = this.dasTransaction.beginTrans();
				for (BookData bd : alNewBook){
					// 반에 추가되지 않았다면 반에 추가해주자
					if (this.ExistBookAtClass(csd.getAcademyRawid(), csd.getClassRawid(), bd.getRawid()) == false){
						ClassBookData cbd = new ClassBookData();
						cbd.setAcademyRawId(csd.getAcademyRawid());
						cbd.setClassRawId(csd.getClassRawid());
						cbd.setBookRawId(bd.getRawid());
						cbd.setStartDate(csd.getStartDt());
						cbd.setEndDate(csd.getEndDt());
						cbd.setCreateBy(csd.getAcademyRawid());
						cbd.setUserTypeCode("A");
						cbd.setMainYN("Y");
						
						if (this.addClassBook(cbd) == false)
							logger.error(String.format("==== 도서등록 실패 ====\r\n[학원:%s]\r\n[학급:%s]\r\n[도서:%s]", cbd.getAcademyRawId(), cbd.getClassRawId(), cbd.getBookRawId()));
					}
				}
				this.dasTransaction.commit(this.conn);
			}
			
		} catch (Exception e) {
			logger.error(e);
			if (dasTransaction != null) try { dasTransaction.rollback(this.conn); } catch(Exception ex) {}
		} finally{
		}

	}
	
	@SuppressWarnings("unchecked")
	private boolean addClassBook(ClassBookData cbd) {
		int iResult = 0;
		
		try{
			LinkedList dataList = new LinkedList();
			
			String sql = "INSERT INTO class_book_link_mst ( " +
						 " rawid, academy_rawid, class_rawid, book_rawid, start_dt," +
						 " end_dt, create_dt, create_by, user_type_cd, main_yn " +
						 ") VALUES ( " +
						 " seq_class_book_link_mst.nextval, ?, ?, ?, ?," +
						 " ?, sysdate, ?, ?, ? " +
						 ") ";
			dataList.add(cbd.getAcademyRawId());
			dataList.add(cbd.getClassRawId());
			dataList.add(cbd.getBookRawId());
			dataList.add(cbd.getStartDate());
			
			dataList.add(cbd.getEndDate());
			dataList.add(cbd.getCreateBy());
			dataList.add(cbd.getUserTypeCode());
			dataList.add(cbd.getMainYN());
			
			iResult = dasTransaction.execute(this.conn, sql, dataList);
		} catch (Exception e) {
			logger.error(e);
		} finally{
		}
		
		return iResult == 1 ? true : false;
	}
	
	@SuppressWarnings("unchecked")
	private ArrayList<ClassSeriesData> GetClassSeriesData(){
		LinkedList llRowset = null;
		
		ArrayList<ClassSeriesData> alClassSeries = new ArrayList<ClassSeriesData>();
		try {
			String sql = "SELECT rawid, academy_rawid, class_rawid, create_order, create_dt," +
						 "       created_by, create_type_cd, series_rawid, start_dt, end_dt " +
						 "  FROM class_series_mst " +
						 " WHERE start_dt <= sysdate " +
						 "   AND end_dt > sysdate ";
			
			LinkedList keyList = new LinkedList();
			llRowset = this.dasTransaction.select(null, sql, keyList);
			
			DASAssistant2<ClassSeriesData> daCtd = new DASAssistant2<ClassSeriesData>(llRowset, ClassSeriesData.class);
			alClassSeries = daCtd.getList();
			
		} catch (Exception e) {
			logger.error(e);
		} finally{
		}
		
		return alClassSeries;
	}

	/**
	 * book_mst에서 book_no 가 해당 월인 것을 가져온다.
	 * 
	 * 하위 설명 취소됨 (2012-02-09 rule 변경)
	 * 시리즈에 금일 등록된 도서를 가져온다.
	 *  - 금일 등록된 도서를 알 수 있는 방법이 없음.
	 *    따라서 그냥 금일 수정된 도서중에 사용유무가 사용중인 도서를 가져옴
	 *    따라서 실제 사용할 때 유효성을 검사 해야 함.
	 * @param seriesRawId
	 * @return
	 */
	@SuppressWarnings({ "unchecked" })
	private ArrayList<BookData> GetRegBooksBySeriesCode(long seriesRawId){
		LinkedList llRowset = null;
		ArrayList<BookData> alBookData = new ArrayList<BookData>();
		
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM");
			String bookNo = sdf.format(Calendar.getInstance().getTime());
			String sql = "SELECT bm.* " +
						 "  FROM book_mst bm " +
						 " INNER JOIN series_mst sm " +
						 "         ON sm.series_cd = bm.series_cd " +
						 " WHERE bm.del_yn = 'N' " +
						 "   AND bm.book_no = ? " +
						 "   AND sm.rawid = ? ";
//			String sql = "SELECT bm.* " +
//			 "  FROM book_mst bm " +
//			 " INNER JOIN series_mst sm " +
//			 "         ON sm.series_cd = bm.series_cd " +
//			 " WHERE bm.del_yn = 'N' " +
//			 "   AND bm.update_dt between to_date(to_char(sysdate,'yyyy-mm')|| '-01', 'yyyy-mm-dd') and sysdate + 1 " +
//			 "   AND sm.rawid = ? ";
			
			LinkedList keyList = new LinkedList();
			keyList.add(bookNo);
			keyList.add(seriesRawId + "");
			llRowset = dasTransaction.select(null, sql, keyList);
			
			DASAssistant2<BookData> daCtd = new DASAssistant2<BookData>(llRowset, BookData.class);
			alBookData = daCtd.getList();			
		} catch (Exception e) {
			logger.error(e);
		} finally{
		}
		
		return alBookData;
	}
	
	@SuppressWarnings({ "unchecked", "unused" })
	private boolean ExistBookAtClass(long academyRawId, long classRawId, long bookRawId){
		LinkedList llRowset = null;
		
		boolean existBook = true;
		ArrayList<BookData> alBookData = new ArrayList<BookData>();
		try {
			String sql = "SELECT * " +
						 "  FROM class_book_link_mst " +
						 " WHERE academy_rawid = ? " +
						 "   AND class_rawid = ? " +
						 "   AND book_rawid = ? " +
						 "   AND start_dt <= sysdate " +
						 "   AND end_dt > sysdate " +
						 "";
			
			LinkedList keyList = new LinkedList();
			keyList.add(academyRawId + "");
			keyList.add(classRawId + "");
			keyList.add(bookRawId + "");
			
			llRowset = dasTransaction.select(this.conn, sql, keyList);
			
			if (llRowset.size() > 0)
				existBook = true;
			else
				existBook = false;
			
		} catch (Exception e) {
			logger.error(e);
		} finally{
		}
		
		return existBook;
	}
	
}
