package kr.co.smenglish.classmgr.data;

import java.util.Date;

public class ClassBookData {
	private long rawId;
	private long academyRawId;
	private long classRawId;
	private long bookRawId;
	private Date startDate;
	private Date endDate;
	private Date createDate;
	private long createBy;
	private String userTypeCode;
	private String weekDays;
	private String studytimeByDays;
	private String mainYN;
	private long curriBookGroupRawId;
	
	public String getStudytimeByDays() {
		return studytimeByDays;
	}
	public void setStudytimeByDays(String studytimeByDays) {
		this.studytimeByDays = studytimeByDays;
	}
	public String getMainYN() {
		return mainYN;
	}
	public void setMainYN(String mainYN) {
		this.mainYN = mainYN;
	}
	public long getCurriBookGroupRawId() {
		return curriBookGroupRawId;
	}
	public void setCurriBookGroupRawId(long curriBookGroupRawId) {
		this.curriBookGroupRawId = curriBookGroupRawId;
	}
	
	public void setRawId(long rawId) {
		this.rawId = rawId;
	}
	public long getRawId() {
		return rawId;
	}
	public void setAcademyRawId(long academyRawId) {
		this.academyRawId = academyRawId;
	}
	public long getAcademyRawId() {
		return academyRawId;
	}
	public void setClassRawId(long classRawId) {
		this.classRawId = classRawId;
	}
	public long getClassRawId() {
		return classRawId;
	}
	public void setBookRawId(long bookRawId) {
		this.bookRawId = bookRawId;
	}
	public long getBookRawId() {
		return bookRawId;
	}
	public void setStartDate(Date startDate) {
		this.startDate = startDate;
	}
	public Date getStartDate() {
		return startDate;
	}
	public void setEndDate(Date endDate) {
		this.endDate = endDate;
	}
	public Date getEndDate() {
		return endDate;
	}
	public void setCreateDate(Date createDate) {
		this.createDate = createDate;
	}
	public Date getCreateDate() {
		return createDate;
	}
	public void setCreateBy(long createBy) {
		this.createBy = createBy;
	}
	public long getCreateBy() {
		return createBy;
	}
	public void setUserTypeCode(String userTypeCode) {
		this.userTypeCode = userTypeCode;
	}
	public String getUserTypeCode() {
		return userTypeCode;
	}
	public void setWeekDays(String weekDays) {
		this.weekDays = weekDays;
	}
	public String getWeekDays() {
		return weekDays;
	}
}
