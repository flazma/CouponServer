package kr.co.smenglish.classmgr.data;

import java.util.Date;

public class BookData {
	private int rawid;
	private String companyCd;
	private String bookCd;
	private String bookName;
	private String seriesCd;
	private String smLevelCd;
	//private long displayOrder;
	private long updatedBy;
	private Date updateDt;
	//private long price;
	//private long salePrice;
	private boolean delYn;
	//private boolean testYN;
	
	public int getRawid() {
		return rawid;
	}

	public void setRawid(int rawid) {
		this.rawid = rawid;
	}

	public String getCompanyCd() {
		return companyCd;
	}

	public void setCompanyCd(String companyCd) {
		this.companyCd = companyCd;
	}

	public String getBookCd() {
		return bookCd;
	}

	public void setBookCd(String bookCd) {
		this.bookCd = bookCd;
	}

	public String getBookName() {
		return bookName;
	}

	public void setBookName(String bookName) {
		this.bookName = bookName;
	}

	public String getSeriesCd() {
		return seriesCd;
	}

	public void setSeriesCd(String seriesCd) {
		this.seriesCd = seriesCd;
	}

	public String getSmLevelCd() {
		return smLevelCd;
	}

	public void setSmLevelCd(String smLevelCd) {
		this.smLevelCd = smLevelCd;
	}

	public long getUpdatedBy() {
		return updatedBy;
	}

	public void setUpdatedBy(long updatedBy) {
		this.updatedBy = updatedBy;
	}

	public Date getUpdateDt() {
		return updateDt;
	}

	public void setUpdateDt(Date updateDt) {
		this.updateDt = updateDt;
	}

	public boolean isDelYn() {
		return delYn;
	}

	public void setDelYn(boolean delYn) {
		this.delYn = delYn;
	}

	public void setDelYn(String deleteYN){
		if (deleteYN.equals("N"))
			this.setDelYn(false);
		else
			this.setDelYn(true);
	}
	
}
