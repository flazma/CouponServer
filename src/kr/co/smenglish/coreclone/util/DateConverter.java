package kr.co.smenglish.coreclone.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.beanutils.Converter;

public class DateConverter implements Converter {

	/**
	 * Bean mapping 데이터 형식 변환
	 * 2010.12.06 - ADD(Kim A Leum)
	 * @param Class, Object
	 * @return Date
	 * @throws ParseException
	 */
	
	public Date convert(Class arg0, Object arg1) {
		
		if(((String)arg1).isEmpty())
			return null;
		
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		Date date = null;
		try {
			date = sdf.parse((String) arg1);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return date;
	}

}
