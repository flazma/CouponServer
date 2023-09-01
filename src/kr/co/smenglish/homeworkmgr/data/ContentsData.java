package kr.co.smenglish.homeworkmgr.data;

import java.util.Date;

public class ContentsData {
	private int rawid;
	private String categoryCd;
	private String subCategoryCd;
	private String unitLevel;
	private String unitOrder;
	private String title;
	private String edesc;
	private String kdesc;
	private String url;
	private String textTime;
	private String studyType;
	private Date studyDay;
	private String streamUrl;
	private String textEn;
	private String textKr;
	private int linkRawid;
	private Date createDt;
	private int updateBy;
	private String userTypeCd;
	private String updateDt;
	private boolean delYn;
	private String version;
	private boolean callStudyYn;
	private Date studyStartDt;
	
	public int getRawid() {
		return rawid;
	}
	public void setRawid(int rawid) {
		this.rawid = rawid;
	}
	public String getCategoryCd() {
		return categoryCd;
	}
	public void setCategoryCd(String categoryCd) {
		this.categoryCd = categoryCd;
	}
	public String getSubCategoryCd() {
		return subCategoryCd;
	}
	public void setSubCategoryCd(String subCategoryCd) {
		this.subCategoryCd = subCategoryCd;
	}
	public String getUnitLevel() {
		return unitLevel;
	}
	public void setUnitLevel(String unitLevel) {
		this.unitLevel = unitLevel;
	}
	public String getUnitOrder() {
		return unitOrder;
	}
	public void setUnitOrder(String unitOrder) {
		this.unitOrder = unitOrder;
	}
	public String getTitle() {
		return title;
	}
	public void setTitle(String title) {
		this.title = title;
	}
	public String getEdesc() {
		return edesc;
	}
	public void setEdesc(String edesc) {
		this.edesc = edesc;
	}
	public String getKdesc() {
		return kdesc;
	}
	public void setKdesc(String kdesc) {
		this.kdesc = kdesc;
	}
	public String getUrl() {
		return url;
	}
	public void setUrl(String url) {
		this.url = url;
	}
	public String getTextTime() {
		return textTime;
	}
	public void setTextTime(String textTime) {
		this.textTime = textTime;
	}
	public String getStudyType() {
		return studyType;
	}
	public void setStudyType(String studyType) {
		this.studyType = studyType;
	}
	public Date getStudyDay() {
		return studyDay;
	}
	public void setStudyDay(Date studyDay) {
		this.studyDay = studyDay;
	}
	public String getStreamUrl() {
		return streamUrl;
	}
	public void setStreamUrl(String streamUrl) {
		this.streamUrl = streamUrl;
	}
	public String getTextEn() {
		return textEn;
	}
	public void setTextEn(String textEn) {
		this.textEn = textEn;
	}
	public String getTextKr() {
		return textKr;
	}
	public void setTextKr(String textKr) {
		this.textKr = textKr;
	}
	public int getLinkRawid() {
		return linkRawid;
	}
	public void setLinkRawid(int linkRawid) {
		this.linkRawid = linkRawid;
	}
	public Date getCreateDt() {
		return createDt;
	}
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}
	public int getUpdateBy() {
		return updateBy;
	}
	public void setUpdateBy(int updateBy) {
		this.updateBy = updateBy;
	}
	public String getUserTypeCd() {
		return userTypeCd;
	}
	public void setUserTypeCd(String userTypeCd) {
		this.userTypeCd = userTypeCd;
	}
	public String getUpdateDt() {
		return updateDt;
	}
	public void setUpdateDt(String updateDt) {
		this.updateDt = updateDt;
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
	public String getVersion() {
		return version;
	}
	public void setVersion(String version) {
		this.version = version;
	}
	public boolean isCallStudyYn() {
		return callStudyYn;
	}
	public void setCallStudyYn(boolean callStudyYn) {
		this.callStudyYn = callStudyYn;
	}
	public void setCallStudyYn(String callStudyYn) {
		if (callStudyYn.equals("N"))
			this.callStudyYn = false;
		else
			this.callStudyYn = true;
	}
	public Date getStudyStartDt() {
		return studyStartDt;
	}
	public void setStudyStartDt(Date studyStartDt) {
		this.studyStartDt = studyStartDt;
	}
}
