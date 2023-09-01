package kr.co.smenglish.couponmgr;

public class AcademyRefund {
	private long rawId = -1;
	private long remainVirtualCouponCount = 0;
	
	public void setRawId(long rawId) {
		this.rawId = rawId;
	}
	public long getRawId() {
		return rawId;
	}
	public void setRemainVirtualCouponCount(long remainVirtualCouponCount) {
		this.remainVirtualCouponCount = remainVirtualCouponCount;
	}
	public long getRemainVirtualCouponCount() {
		return remainVirtualCouponCount;
	}
	
}
