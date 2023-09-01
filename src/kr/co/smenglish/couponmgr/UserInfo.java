package kr.co.smenglish.couponmgr;

import java.util.Date;

public class UserInfo {
	private long rawId = -1;
	private long academyRawId = -1;
	private String cpapply = "N";
	private Date cpApplyDate;
	
	//add by mkkim
	private String requiredYN = "N";
	private String differYN = "N";
	private String minusYN = "N";
	private String monthlyCompanyBefore="";
	private String monthlyCompanyAfter="";
	private String prevUse = "";
	private String nowUse = "";
	private String couponWorkTypeCD = "";
	private String applyStartDay="";
	private String reservationYN="";
	
	private long overCompanyCount = 0;
	private long baseCouponCount = 0;
	private long addCompanyCouponCount = 0;
	private long totalCouponCount = 0;
	private long prevRemainCouponCount = 0;
	private long minusCouponCount = 0;
	
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
	public void setCpapply(String cp_apply) {
		this.cpapply = cp_apply;
	}
	public String getCpapply() {
		return cpapply;
	}
	public void setCpApplyDate(Date cpApplyDate) {
		this.cpApplyDate = cpApplyDate;
	}
	public Date getCpApplyDate() {
		return cpApplyDate;
	}
	//add by mkkim
	public void setRequiredYN(String requiredYN) {
		this.requiredYN = requiredYN;
	}
	public String getRequiredYN() {
		return requiredYN;
	}

	public void setReservationYN(String reservationYN) {
		this.reservationYN = reservationYN;
	}
	public String getReservationYN() {
		return reservationYN;
	}

	
	public void setApplyStartDay(String applyStartDay) {
		this.applyStartDay = applyStartDay;
	}
	public String getApplyStartDay() {
		return applyStartDay;
	}
	
	public void setDifferYN(String differYN) {
		this.differYN = differYN;
	}
	public String getDifferYN() {
		return differYN;
	}
	
	public void setMinusYN(String minusYN) {
		this.minusYN = minusYN;
	}
	public String getMinusYN() {
		return minusYN;
	}
	
	
	public void setOverCompanyCount(long overCompanyCount) {
		this.overCompanyCount = overCompanyCount;
	}
	public long getOverCompanyCount() {
		return overCompanyCount;
	}
	
	
	public void setMonthlyCompanyBefore(String monthlyCompanyBefore) {
		this.monthlyCompanyBefore = monthlyCompanyBefore;
	}
	public String getMonthlyCompanyBefore() {
		return monthlyCompanyBefore;
	}
	
	public void setMonthlyCompanyAfter(String monthlyCompanyAfter) {
		this.monthlyCompanyAfter = monthlyCompanyAfter;
	}
	public String getMonthlyCompanyAfter() {
		return monthlyCompanyAfter;
	}
	
	public void setPrevUse(String prevUse) {
		this.prevUse = prevUse;
	}
	public String getPrevUse() {
		return prevUse;
	}
	
	public void setNowUse(String nowUse) {
		this.nowUse = nowUse;
	}
	public String getNowUse() {
		return nowUse;
	}
	
	
	public void setCouponWorkTypeCD(String couponWorkTypeCD) {
		this.couponWorkTypeCD = couponWorkTypeCD;
	}
	public String getCouponWorkTypeCD() {
		return couponWorkTypeCD;
	}
	
	public void setBaseCouponCount(long baseCouponCount) {
		this.baseCouponCount = baseCouponCount;
	}
	public long getBaseCouponCount() {
		return baseCouponCount;
	}
	
	public void setAddCompanyCouponCount(long addCompanyCouponCount) {
		this.addCompanyCouponCount = addCompanyCouponCount;
	}
	public long getAddCompanyCouponCount() {
		return addCompanyCouponCount;
	}
	
	public void setTotalCouponCount(long totalCouponCount) {
		this.totalCouponCount = totalCouponCount;
	}
	public long getTotalCouponCount() {
		return totalCouponCount;
	}
	
	public void setPrevRemainCouponCount(long prevRemainCouponCount) {
		this.prevRemainCouponCount = prevRemainCouponCount;
	}
	public long getPrevRemainCouponCount() {
		return prevRemainCouponCount;
	}
	
	public void setMinusCouponCount(long minusCouponCount) {
		this.minusCouponCount = minusCouponCount;
	}
	public long getMinusCouponCount() {
		return minusCouponCount;
	}
	
}
