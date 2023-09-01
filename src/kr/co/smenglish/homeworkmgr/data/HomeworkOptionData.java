package kr.co.smenglish.homeworkmgr.data;

import java.util.Date;
import java.util.LinkedHashMap;

import kr.co.smenglish.coreclone.util.NullToValue;

public class HomeworkOptionData {
	private boolean wordYn;
	private String wordRateCd;
	private String studyMadeItem;
	private String lcStudyCd;
	private String dictationLevelCd;
	private String dictationMethodCd;
	private int dictationRate;
	private int dictationRepeatCnt;
	private int dictationHintCnt;
	private String optHomework;
	private String compositionMethodCd;
	private int compositionRate;
	private int compositionRepeatCnt;
	private int compositionHintCnt;
	private int translationRate;
	private int translationHintCnt;
	private int translationRepeatCnt;
	private int translationTextlengthRate;
	private String speakingMethodCd;
	private int speakingRate;
	private int speakingRepeatCnt;
	private int speakingHintCnt;
	private boolean speakingDictationYn;
	
	public boolean isWordYn() {
		return wordYn;
	}
	public void setWordYn(boolean wordYn) {
		this.wordYn = wordYn;
	}
	public void setWordYn(String wordYn) {
		if (wordYn.equals("N"))
			this.wordYn = false;
		else
			this.wordYn = true;
	}
	public String getWordRateCd() {
		return wordRateCd;
	}
	public void setWordRateCd(String wordRateCd) {
		this.wordRateCd = wordRateCd;
	}
	public String getStudyMadeItem() {
		return studyMadeItem;
	}
	public void setStudyMadeItem(String studyMadeItem) {
		this.studyMadeItem = studyMadeItem;
	}
	public String getLcStudyCd() {
		return lcStudyCd;
	}
	public void setLcStudyCd(String lcStudyCd) {
		this.lcStudyCd = lcStudyCd;
	}
	public String getDictationLevelCd() {
		return dictationLevelCd;
	}
	public void setDictationLevelCd(String dictationLevelCd) {
		this.dictationLevelCd = dictationLevelCd;
	}
	public String getDictationMethodCd() {
		return dictationMethodCd;
	}
	public void setDictationMethodCd(String dictationMethodCd) {
		this.dictationMethodCd = dictationMethodCd;
	}
	public int getDictationRate() {
		return dictationRate;
	}
	public void setDictationRate(int dictationRate) {
		this.dictationRate = dictationRate;
	}
	public int getDictationRepeatCnt() {
		return dictationRepeatCnt;
	}
	public void setDictationRepeatCnt(int dictationRepeatCnt) {
		this.dictationRepeatCnt = dictationRepeatCnt;
	}
	public int getDictationHintCnt() {
		return dictationHintCnt;
	}
	public void setDictationHintCnt(int dictationHintCnt) {
		this.dictationHintCnt = dictationHintCnt;
	}
	public String getOptHomework() {
		return optHomework;
	}
	public void setOptHomework(String optHomework) {
		this.optHomework = optHomework;
	}
	public String getCompositionMethodCd() {
		return compositionMethodCd;
	}
	public void setCompositionMethodCd(String compositionMethodCd) {
		this.compositionMethodCd = compositionMethodCd;
	}
	public int getCompositionRate() {
		return compositionRate;
	}
	public void setCompositionRate(int compositionRate) {
		this.compositionRate = compositionRate;
	}
	public int getCompositionRepeatCnt() {
		return compositionRepeatCnt;
	}
	public void setCompositionRepeatCnt(int compositionRepeatCnt) {
		this.compositionRepeatCnt = compositionRepeatCnt;
	}
	public int getCompositionHintCnt() {
		return compositionHintCnt;
	}
	public void setCompositionHintCnt(int compositionHintCnt) {
		this.compositionHintCnt = compositionHintCnt;
	}
	public int getTranslationRate() {
		return translationRate;
	}
	public void setTranslationRate(int translationRate) {
		this.translationRate = translationRate;
	}
	public int getTranslationHintCnt() {
		return translationHintCnt;
	}
	public void setTranslationHintCnt(int translationHintCnt) {
		this.translationHintCnt = translationHintCnt;
	}
	public int getTranslationRepeatCnt() {
		return translationRepeatCnt;
	}
	public void setTranslationRepeatCnt(int translationRepeatCnt) {
		this.translationRepeatCnt = translationRepeatCnt;
	}
	public int getTranslationTextlengthRate() {
		return translationTextlengthRate;
	}
	public void setTranslationTextlengthRate(int translationTextlengthRate) {
		this.translationTextlengthRate = translationTextlengthRate;
	}
	public String getSpeakingMethodCd() {
		return speakingMethodCd;
	}
	public void setSpeakingMethodCd(String speakingMethodCd) {
		this.speakingMethodCd = speakingMethodCd;
	}
	public int getSpeakingRate() {
		return speakingRate;
	}
	public void setSpeakingRate(int speakingRate) {
		this.speakingRate = speakingRate;
	}
	public int getSpeakingRepeatCnt() {
		return speakingRepeatCnt;
	}
	public void setSpeakingRepeatCnt(int speakingRepeatCnt) {
		this.speakingRepeatCnt = speakingRepeatCnt;
	}
	public int getSpeakingHintCnt() {
		return speakingHintCnt;
	}
	public void setSpeakingHintCnt(int speakingHintCnt) {
		this.speakingHintCnt = speakingHintCnt;
	}
	public boolean isSpeakingDictationYn() {
		return speakingDictationYn;
	}
	public void setSpeakingDictationYn(boolean speakingDictationYn) {
		this.speakingDictationYn = speakingDictationYn;
	}
	public void setSpeakingDictationYn(String speakingDictationYN) {
		if (speakingDictationYN.equals("N"))
			this.speakingDictationYn = false;
		else
			this.speakingDictationYn = true;
	}
	
	public LinkedHashMap<String, String> MakeRowSet() {
		LinkedHashMap<String, String> lhmRowset = new LinkedHashMap<String, String>();
		
		return lhmRowset;
	}
	
	public LinkedHashMap<String, Object> MakeRowSet(LinkedHashMap<String, Object> lhmRowset) {
		
		//lhmRowset.put("RAWID", hw_rawid);
		//lhmRowset.put("CATEGORY_CD", (String) temp.get("category_cd"));
		//lhmRowset.put("SUB_CATEGORY_CD", (String) temp.get("sub_category_cd"));
		//lhmRowset.put("TITLE", temp.get("title").toString());
		//lhmRowset.put("USER_RAWID", user_list[k]);
		//lhmRowset.put("CONTENTS_RAWID", content_rawid);
		//lhmRowset.put("PROGRESS", "0");
		//lhmRowset.put("CREATE_DT+SYSDATE", null);
		//lhmRowset.put("START_DT", "");
		//lhmRowset.put("END_DT", "");
		//lhmRowset.put("SUBJECT_DT", subject_dt);
		lhmRowset.put("WORD_YN", this.wordYn ? "Y" : "N");
		lhmRowset.put("DICTATION_LEVEL_CD", this.dictationLevelCd);
		//lhmRowset.put("USER_TYPE", user_type);
		//lhmRowset.put("TUTOR_RAWID", );
		//lhmRowset.put("DEL_YN", "N");
		//lhmRowset.put("CLASS_RAWID", this.cl);
		lhmRowset.put("WORD_RATE_CD", NullToValue.NVL(this.wordRateCd, "").toString());
		lhmRowset.put("STUDY_MADE_ITEM", this.studyMadeItem);//study_made_item.get(study_type));
		lhmRowset.put("LC_STUDY_CD", this.lcStudyCd);
		lhmRowset.put("DICTATION_METHOD_CD", NullToValue.NVL(this.dictationMethodCd + "", "").toString());
		lhmRowset.put("DICTATION_RATE", NullToValue.NVL(this.dictationRate + "", "").toString());
		lhmRowset.put("DICTATION_HINT_CNT", NullToValue.NVL(this.dictationHintCnt + "", "").toString());
		lhmRowset.put("DICTATION_REPEAT_CNT", NullToValue.NVL(this.dictationRepeatCnt + "", "").toString());
		lhmRowset.put("COMPOSITION_METHOD_CD", NullToValue.NVL(this.compositionMethodCd, "").toString());
		lhmRowset.put("COMPOSITION_RATE", NullToValue.NVL(this.compositionRate + "", "").toString());
		lhmRowset.put("COMPOSITION_REPEAT_CNT", NullToValue.NVL(this.compositionRepeatCnt + "", "").toString());
		lhmRowset.put("COMPOSITION_HINT_CNT", NullToValue.NVL(this.compositionHintCnt + "", "").toString());
		lhmRowset.put("TRANSLATION_RATE", NullToValue.NVL(this.translationRate + "", "").toString());
		lhmRowset.put("TRANSLATION_HINT_CNT", NullToValue.NVL(this.translationHintCnt + "", "").toString());
		lhmRowset.put("TRANSLATION_REPEAT_CNT", NullToValue.NVL(this.translationRepeatCnt + "", "").toString());
		lhmRowset.put("TRANSLATION_TEXTLENGTH_RATE", NullToValue.NVL(this.translationTextlengthRate + "", "").toString());
		lhmRowset.put("OPT_HOMEWORK", this.optHomework);//"<USER_SET>" + user_setting + "</USER_SET>");
		lhmRowset.put("SPEAKING_METHOD_CD", NullToValue.NVL(this.speakingMethodCd, "").toString());
		lhmRowset.put("SPEAKING_RATE", NullToValue.NVL(this.speakingRate + "", "").toString());
		lhmRowset.put("SPEAKING_REPEAT_CNT", NullToValue.NVL(this.speakingRepeatCnt + "", "").toString());
		lhmRowset.put("SPEAKING_HINT_CNT", NullToValue.NVL(this.speakingHintCnt + "", "").toString());
		lhmRowset.put("SPEAKING_DICTATION_YN", NullToValue.NVL(this.speakingDictationYn ? "Y" : "N", "").toString());
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
