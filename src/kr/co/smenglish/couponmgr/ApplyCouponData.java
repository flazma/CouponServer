package kr.co.smenglish.couponmgr;

public class ApplyCouponData {

	private String couponWorkTypeCD = "";
	private String checkOverCompany = "";
	private String stopApplyYN      = "";
	private String useTypeCD 		= "";
	private String couponType		= "";
	private String freeYN		    = "N";
	private String renewType        = "N";
	
	
	private long stopNoClassCount = 0;
	private long stopNoCouponCount = 0;
	
	public void setCouponWorkTypeCD(String couponWorkTypeCD) {
		this.couponWorkTypeCD = couponWorkTypeCD;
	}
	public String getCouponWorkTypeCD() {
		return couponWorkTypeCD;
	}
	
	public void setCheckOverCompany(String checkOverCompany) {
		this.checkOverCompany = checkOverCompany;
	}
	public String getCheckOverCompany() {
		return checkOverCompany;
	}
	
	public void setStopApplyYN(String stopApplyYN) {
		this.stopApplyYN = stopApplyYN;
	}
	public String getStopApplyYN() {
		return stopApplyYN;
	}

	public void setUseTypeCD(String useTypeCD) {
		this.useTypeCD = useTypeCD;
	}
	public String getUseTypeCD() {
		return useTypeCD;
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
	
	public void setCouponType(String couponType) {
		this.couponType = couponType;
	}
	public String getCouponType() {
		return couponType;
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
