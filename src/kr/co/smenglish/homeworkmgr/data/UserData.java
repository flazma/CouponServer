package kr.co.smenglish.homeworkmgr.data;

import java.util.Date;

public class UserData {
	private int rawid;
	private int academyRawid;
	private String name;
//	private String birthDt;
//	private String address1;
//	private String address2;
	private String userId;
//	private String passwd;
//	private String intro;
//	private String mymotto;
	private String userStatusCd;
	private Date delDt;
	private Date endDt;
//	private String userStudyTrxRawid;
//	private String email;
//	private String emailParent;
//	private String academyTypeCd;
//	private String enname;
//	private String telnum;
//	private String shelnum;
//	private String smPoint;
//	private String createDt;
//	private String parentnum;
//	private String userIp;
//	private String game_nickname;
//	private String ohCode;
//	private String sexCd;
	private boolean delYn;
//	private String tutorYn;
//	private String juminNumber;
	private String gradeCd;
	private String textBookCd;
	private String gradeClassName;
//	private String shareYn;
	private boolean cpApplyYn;
	private Date cpApplyDt;
//	private String schoolRawid;
//	private String mobileIds;
//	private String aboutStudent;
//	private String ipin;
	
	public int getRawid() {
		return rawid;
	}
	public void setRawid(int rawid) {
		this.rawid = rawid;
	}
	public int getAcademyRawid() {
		return academyRawid;
	}
	public void setAcademyRawid(int academyRawid) {
		this.academyRawid = academyRawid;
	}
	public String getName() {
		return name;
	}
	public void setName(String name) {
		this.name = name;
	}
	public String getUserId() {
		return userId;
	}
	public void setUserId(String userId) {
		this.userId = userId;
	}
	public String getUserStatusCd() {
		return userStatusCd;
	}
	public void setUserStatusCd(String userStatusCd) {
		this.userStatusCd = userStatusCd;
	}
	public Date getDelDt() {
		return delDt;
	}
	public void setDelDt(Date delDt) {
		this.delDt = delDt;
	}
	public Date getEndDt() {
		return endDt;
	}
	public void setEndDt(Date endDt) {
		this.endDt = endDt;
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
	public String getGradeCd() {
		return gradeCd;
	}
	public void setGradeCd(String gradeCd) {
		this.gradeCd = gradeCd;
	}
	public String getTextBookCd() {
		return textBookCd;
	}
	public void setTextBookCd(String textBookCd) {
		this.textBookCd = textBookCd;
	}
	public String getGradeClassName() {
		return gradeClassName;
	}
	public void setGradeClassName(String gradeClassName) {
		this.gradeClassName = gradeClassName;
	}
	public boolean isCpApplyYn() {
		return cpApplyYn;
	}
	public void setCpApplyYn(boolean cpApplyYn) {
		this.cpApplyYn = cpApplyYn;
	}
	public void setCpApplyYn(String cpApplyYn) {
		if (cpApplyYn.equals("N"))
			this.cpApplyYn = false;
		else
			this.cpApplyYn = true;
	}
	public Date getCpApplyDt() {
		return cpApplyDt;
	}
	public void setCpApplyDt(Date cpApplyDt) {
		this.cpApplyDt = cpApplyDt;
	}
	
}
