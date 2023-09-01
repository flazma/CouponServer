package kr.co.smenglish.classmgr.data;

import java.util.Date;

public class ClassSeriesData {
	private int rawid;
	private int academyRawid;
	private int classRawid;
	private int createOrder;
	private Date createDt;
	private int createBy;
	private String createTypeCd;
	private int seriesRawid;
	private Date startDt;
	private Date endDt;
	
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
	public int getClassRawid() {
		return classRawid;
	}
	public void setClassRawid(int classRawid) {
		this.classRawid = classRawid;
	}
	public int getCreateOrder() {
		return createOrder;
	}
	public void setCreateOrder(int createOrder) {
		this.createOrder = createOrder;
	}
	public Date getCreateDt() {
		return createDt;
	}
	public void setCreateDt(Date createDt) {
		this.createDt = createDt;
	}
	public int getCreateBy() {
		return createBy;
	}
	public void setCreateBy(int createBy) {
		this.createBy = createBy;
	}
	public String getCreateTypeCd() {
		return createTypeCd;
	}
	public void setCreateTypeCd(String createTypeCd) {
		this.createTypeCd = createTypeCd;
	}
	public int getSeriesRawid() {
		return seriesRawid;
	}
	public void setSeriesRawid(int seriesRawid) {
		this.seriesRawid = seriesRawid;
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
}
