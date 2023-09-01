package kr.co.smenglish.coreclone.util;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

public class SimpleDateCtrl {

	public static String convertDateToStr(Date date){
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		
		return sdf.format(date);
	}

	public static Date addDay(Date date, int day){
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		
		cal.add(Calendar.DATE, day);
		
		return cal.getTime();
	}

	public static Date addMonth(Date date, int month){
		Calendar cal = Calendar.getInstance();
		cal.setTime(date);
		
		cal.add(Calendar.MONTH, month);
		
		return cal.getTime();
	}
}
