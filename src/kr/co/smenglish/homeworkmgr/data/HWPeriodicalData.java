package kr.co.smenglish.homeworkmgr.data;

import java.util.Date;
import java.util.LinkedHashMap;

import kr.co.smenglish.coreclone.util.SimpleDateCtrl;

public class HWPeriodicalData {
	private int rawid;
	private int academyRawid;
	private int seriesRawid;
	private String seriesCd;
	private Date startDt;
	private Date endDt;
	private int hwPeriod;
	private String hwWeekdays;
	private int classRawid;
	private int tutorRawid;
	private String userType;
	private boolean delYn;
	private Date createDt;
	private int createrRawid;
	private String createrTypeCd;
	
	private HomeworkOptionData hod = new HomeworkOptionData();
	
	public boolean ContainsWeekday(int weekday){
		return this.hwWeekdays.contains(weekday + "");
	}
	
	public void setHomeworkOptionData(HomeworkOptionData hod) {
		this.hod = hod;
	}
	public HomeworkOptionData getHomeworkOptionData() {
		return hod;
	}
	
	public int getRawid() {
		return rawid;
	}
	public void setRawid(int rawid) {
		this.rawid = rawid;
	}
	public void setAcademyRawid(int academyRawid) {
		this.academyRawid = academyRawid;
	}

	public int getAcademyRawid() {
		return academyRawid;
	}

	public int getSeriesRawid() {
		return seriesRawid;
	}
	public void setSeriesRawid(int seriesRawid) {
		this.seriesRawid = seriesRawid;
	}
	public void setSeriesCd(String seriesCd) {
		this.seriesCd = seriesCd;
	}

	public String getSeriesCd() {
		return seriesCd;
	}

	public Date getStartDt() {
		return startDt;
	}
	public void setStartDt(Date startDt) {
		this.startDt = startDt;
	}
	public Date getEndDt() {
		return endDt;
	}
	public void setEndDt(Date endDt) {
		this.endDt = endDt;
	}
	public int getHwPeriod() {
		return hwPeriod;
	}
	public void setHwPeriod(int hwPeriod) {
		this.hwPeriod = hwPeriod;
	}
	public String getHwWeekdays() {
		return hwWeekdays;
	}
	public void setHwWeekdays(String hwWeekdays) {
		this.hwWeekdays = hwWeekdays;
	}
	public int getClassRawid() {
		return classRawid;
	}
	public void setClassRawid(int classRawid) {
		this.classRawid = classRawid;
	}
	public int getTutorRawid() {
		return tutorRawid;
	}
	public void setTutorRawid(int tutorRawid) {
		this.tutorRawid = tutorRawid;
	}
	public void setUserType(String userType) {
		this.userType = userType;
	}

	public String getUserType() {
		return userType;
	}

	public boolean isDelYn() {
		return delYn;
	}
	public void setDelYn(boolean delYn) {
		this.delYn = delYn;
	}
	public void setDelYn(String delYn) {
		if (delYn.equals("N"))
			this.delYn = false;
		else
			this.delYn = true;
	}
	public Date getCreateDt() {
		return createDt;
	}
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}
	public int getCreaterRawid() {
		return createrRawid;
	}
	public void setCreaterRawid(int createrRawid) {
		this.createrRawid = createrRawid;
	}
	public String getCreaterTypeCd() {
		return createrTypeCd;
	}
	public void setCreaterTypeCd(String createrTypeCd) {
		this.createrTypeCd = createrTypeCd;
	}
	
//	public LinkedHashMap<String, String> MakeHomeworkRowSet() {
//		LinkedHashMap<String, String> lhmRowset = new LinkedHashMap<String, String>();
//		
//		return lhmRowset;
//	}

	/**
	 * DAS3의 배치을 위한 rowset에 값을 셋(set)한다.
	 */
	public LinkedHashMap<String, Object> MakeHomeworkRowSet(LinkedHashMap<String, Object> lhmRowset) {
		
		//lhmRowset.put("RAWID", hw_rawid);
		//lhmRowset.put("CATEGORY_CD", );
		//lhmRowset.put("SUB_CATEGORY_CD", );
		//lhmRowset.put("TITLE", this.ti);
		//lhmRowset.put("USER_RAWID", this.u);
		//lhmRowset.put("CONTENTS_RAWID", this.con);
//		lhmRowset.put("PROGRESS", "0");
//		lhmRowset.put("CREATE_DT+SYSDATE", null);
//		lhmRowset.put("START_DT", SimpleDateCtrl.convertDateToStr(this.startDt));
//		lhmRowset.put("END_DT", SimpleDateCtrl.convertDateToStr(SimpleDateCtrl.addMonth(this.startDt, 1)));
//		lhmRowset.put("SUBJECT_DT", SimpleDateCtrl.convertDateToStr(SimpleDateCtrl.addDay(this.startDt, this.hwPeriod)));
		
		
		// 여기까지....
		lhmRowset.put("USER_TYPE", this.userType);
		lhmRowset.put("TUTOR_RAWID", this.tutorRawid + "");
		lhmRowset.put("DEL_YN", "N");
		lhmRowset.put("CLASS_RAWID", this.classRawid + "");
		
		this.hod.MakeRowSet(lhmRowset);
		
//		lhmRowset.put("WORD_YN", this.wordYn ? "Y" : "N");
//		lhmRowset.put("DICTATION_LEVEL_CD", this.dictationLevelCd);
//		lhmRowset.put("WORD_RATE_CD", NullToValue.NVL(this.wordRateCd, "").toString());
//		lhmRowset.put("STUDY_MADE_ITEM", this.studyMadeItem);//study_made_item.get(study_type));
//		lhmRowset.put("LC_STUDY_CD", this.lcStudyCd);
//		lhmRowset.put("DICTATION_METHOD_CD", NullToValue.NVL(this.dictationMethodCd + "", "").toString());
//		lhmRowset.put("DICTATION_RATE", NullToValue.NVL(this.dictationRate + "", "").toString());
//		lhmRowset.put("DICTATION_HINT_CNT", NullToValue.NVL(this.dictationHintCnt + "", "").toString());
//		lhmRowset.put("DICTATION_REPEAT_CNT", NullToValue.NVL(this.dictationRepeatCnt + "", "").toString());
//		lhmRowset.put("COMPOSITION_METHOD_CD", NullToValue.NVL(this.compositionMethodCd, "").toString());
//		lhmRowset.put("COMPOSITION_RATE", NullToValue.NVL(this.compositionRate + "", "").toString());
//		lhmRowset.put("COMPOSITION_REPEAT_CNT", NullToValue.NVL(this.compositionRepeatCnt + "", "").toString());
//		lhmRowset.put("COMPOSITION_HINT_CNT", NullToValue.NVL(this.compositionHintCnt + "", "").toString());
//		lhmRowset.put("TRANSLATION_RATE", NullToValue.NVL(this.translationRate + "", "").toString());
//		lhmRowset.put("TRANSLATION_HINT_CNT", NullToValue.NVL(this.translationHintCnt + "", "").toString());
//		lhmRowset.put("TRANSLATION_REPEAT_CNT", NullToValue.NVL(this.translationRepeatCnt + "", "").toString());
//		lhmRowset.put("TRANSLATION_TEXTLENGTH_RATE", NullToValue.NVL(this.translationTextlengthRate + "", "").toString());
//		lhmRowset.put("OPT_HOMEWORK", this.optHomework);//"<USER_SET>" + user_setting + "</USER_SET>");
//		lhmRowset.put("SPEAKING_METHOD_CD", NullToValue.NVL(this.speakingMethodCd, "").toString());
//		lhmRowset.put("SPEAKING_RATE", NullToValue.NVL(this.speakingRate + "", "").toString());
//		lhmRowset.put("SPEAKING_REPEAT_CNT", NullToValue.NVL(this.speakingRepeatCnt + "", "").toString());
//		lhmRowset.put("SPEAKING_HINT_CNT", NullToValue.NVL(this.speakingHintCnt + "", "").toString());
//		lhmRowset.put("SPEAKING_DICTATION_YN", NullToValue.NVL(this.speakingDictationYn ? "Y" : "N", "").toString());
		//lhmRowset.put("CLASS_BOOK_LINK_MST_RAWID", (String) temp.get("class_book_link_mst_rawid"));
		//lhmRowset.put("CLASS_HOMEWORK_TRX_RAWID", class_hw_rawid);
		//lhmRowset.put("CHECK_COMMENT", "");
		//lhmRowset.put("CHECK_CD", "N");
		//lhmRowset.put("CHECK_USER_TYPE", "");
		//lhmRowset.put("CHECK_USER_RAWID", "");
		//lhmRowset.put("CHECK_DT", "");

		return lhmRowset;
	}	
}
