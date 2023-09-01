package kr.co.smenglish.couponmgr;

public class AcademyCoupon {
	private long rawId = -1;
	private long remainCouponCount = 0;
	private long stopNoClassCount  = 0;
	private long stopNoCouponCount = 0;
	private String freeYN		   = "N";
	private String renewType       = "R";
	
	public void setRawId(long rawId) {
		this.rawId = rawId;
	}
	public long getRawId() {
		return rawId;
	}
	public void setRemainCouponCount(long remainCouponCount) {
		this.remainCouponCount = remainCouponCount;
	}
	public long getRemainCouponCount() {
		return remainCouponCount;
	}
	
	
	public void setStopNoClassCount(long stopNoClassCount) {
		this.stopNoClassCount += stopNoClassCount;
	}
	public long getStopNoClassCount() {
		return stopNoClassCount;
	}
	
	public void setStopNoCouponCount(long stopNoCouponCount) {
		this.stopNoCouponCount += stopNoCouponCount;
	}
	public long getStopNoCouponCount() {
		return stopNoCouponCount;
	}
	
	public void setFreeYN(String freeYN) {
		this.freeYN = freeYN;
	}
	public String getFreeYN() {
		return freeYN;
	}
	
	public void setRenewType(String renewType) {
		this.renewType = renewType;
	}
	public String getRenewType() {
		return renewType;
	}
}
