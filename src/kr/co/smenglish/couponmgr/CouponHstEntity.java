package kr.co.smenglish.couponmgr;

import java.util.Date;

public class CouponHstEntity {
	private long rawId = -1;
	private long academyRawId = -1;
	private Date applyDate;
	private String useTypeCode;
	private String createTypeCode;
	private long applyUserCnt = 0;
	private long nonApplyUserCnt = 0;
	private long applyCouponCnt = 0;
	private long remainCouponCnt = 0;
	private String couponType="NC";
	
	public void setRawId(long rawId) {
		this.rawId = rawId;
	}
	public long getRawId() {
		return rawId;
	}
	public void setApplyDate(Date applyDate) {
		this.applyDate = applyDate;
	}
	public Date getApplyDate() {
		return applyDate;
	}
	public void setUseTypeCode(String useTypeCode) {
		this.useTypeCode = useTypeCode;
	}
	public String getUseTypeCode() {
		return useTypeCode;
	}
	public void setCreateTypeCode(String createTypeCode) {
		this.createTypeCode = createTypeCode;
	}
	public String getCreateTypeCode() {
		return createTypeCode;
	}
	public void setApplyUserCnt(long applyUserCnt) {
		this.applyUserCnt = applyUserCnt;
	}
	public long getApplyUserCnt() {
		return applyUserCnt;
	}
	public void setNonApplyUserCnt(long nonApplyUserCnt) {
		this.nonApplyUserCnt = nonApplyUserCnt;
	}
	public long getNonApplyUserCnt() {
		return nonApplyUserCnt;
	}
	public void setApplyCouponCnt(long applyCouponCnt) {
		this.applyCouponCnt = applyCouponCnt;
	}
	public long getApplyCouponCnt() {
		return applyCouponCnt;
	}
	public void setRemainCouponCnt(long remainCouponCnt) {
		this.remainCouponCnt = remainCouponCnt;
	}
	public long getRemainCouponCnt() {
		return remainCouponCnt;
	}
	public void setAcademyRawId(long academyRawId) {
		this.academyRawId = academyRawId;
	}
	public long getAcademyRawId() {
		return academyRawId;
	}
	
	//ADD BY MKKIM
	public void setCouponType(String couponType) {
		this.couponType = couponType;
	}
	public String getCouponType() {
		return this.couponType;
	}
}
