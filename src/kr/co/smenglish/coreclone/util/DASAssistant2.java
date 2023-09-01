package kr.co.smenglish.coreclone.util;

import java.lang.reflect.InvocationTargetException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.beanutils.ConstructorUtils;
import org.apache.commons.beanutils.ConvertUtils;
import org.apache.commons.logging.Log;


import kr.co.smenglish.coreclone.util.DateConverter;

public class DASAssistant2<T> {

	/*
	 * DAS 값 커스텀을 위한 보조 클래스 (모든 값을 Object 타입으로 가져오기 때문에 데이터형식 변환을 위해 생성)
	 */

	@SuppressWarnings("unchecked")
	private LinkedHashMap map;
	@SuppressWarnings("unchecked")
	private LinkedList rowset;
	private Class<T> t;

	@SuppressWarnings("unchecked")
	public DASAssistant2(LinkedHashMap map, Class<T> type) {
		this.map = map;
		ConvertUtils.register(new DateConverter(), Date.class);
		this.t = type;
	}

	@SuppressWarnings("unchecked")
	public DASAssistant2(LinkedList rowset, Class<T> type) {
		this.rowset = rowset;
		ConvertUtils.register(new DateConverter(), Date.class);
		this.t = type;
	}

	public String getString(String key) {
		key = key.toLowerCase();
		return map.get(key) == null ? "" : (String) map.get(key);
	}

	public int getInteger(String key) {
		key = key.toLowerCase();
		return ((String) map.get(key)).isEmpty() ? 0 : Integer.parseInt((String) map.get(key));
	}

	public Date getDate(String key) {
		key = key.toLowerCase();
		try {
			SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
			return sdf.parse((String) map.get(key));
		} catch (Exception e) {
			return null;
		}
	}


	/**
	 * 쿼리 결과 Bean mapping 2010.12.02 - ADD(PHJ)
	 * 
	 * @param bean
	 * @return
	 * @throws IllegalAccessException
	 * @throws InvocationTargetException
	 */

	public ArrayList<T> getList() throws Exception {
		ArrayList<T> col = new ArrayList<T>();

		for (int i = 0; i < rowset.size(); i++) {
			this.map = (LinkedHashMap) rowset.get(i);
			T targetBean = (T) ConstructorUtils.invokeConstructor(this.t, null);
			
			this.setValueToBean(targetBean, map);
//			Iterator iter = map.keySet().iterator();
//			while (iter.hasNext()) {
//				String key = (String) iter.next();
//				Object value = map.get(key);
//				int idx = key.indexOf("_");
//				if (idx > -1) {
//					String[] tmp = key.split("_");
//					key = tmp[0].toLowerCase();
//					for (int k = 1; k < tmp.length; k++) {
//						key += tmp[k].substring(0, 1).toUpperCase()
//								+ tmp[k].substring(1).toLowerCase();
//					}
//				}
//
//				BeanUtils.setProperty(targetBean, key, value);
//			}
			col.add(targetBean);
		}

		return col;
	}
	
	/**
	 * 해당 빈에 set메소드를 이용한 값을 넣는다.
	 * @param targetBean
	 * @param map
	 * @throws Exception
	 */
	public void setValueToBean(T targetBean, LinkedHashMap map) throws Exception {
		Iterator iter = map.keySet().iterator();

		//T targetBean = (T) ConstructorUtils.invokeConstructor(this.t, null);
		while (iter.hasNext()) {
			String key = (String) iter.next();
			Object value = map.get(key);
			int idx = key.indexOf("_");
			if (idx > -1) {
				String[] tmp = key.split("_");
				key = tmp[0].toLowerCase();
				for (int k = 1; k < tmp.length; k++) {
					key += tmp[k].substring(0, 1).toUpperCase()
							+ tmp[k].substring(1).toLowerCase();
				}
			}

			BeanUtils.setProperty(targetBean, key, value);
		}
	}

	/**
	 * 쿼리 결과 Bean 반환 2010.12.02 - ADD(PHJ)
	 * 
	 * @param bean
	 * @return
	 * @throws Exception
	 */
	public T getBean() throws Exception {
		ArrayList<T> list = this.getList();
		T obj = (T) ConstructorUtils.invokeConstructor(this.t, null);
		if (list.size() > 0) {
			obj = list.get(0);
		}
		return obj;
	}
}
