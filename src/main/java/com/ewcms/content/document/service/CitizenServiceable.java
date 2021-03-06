/*
 * Copyright (c)2010 Jiangxi Institute of Computing Technology(JICT), All rights reserved.
 * JICT PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * http://www.jict.org
 */
package com.ewcms.content.document.service;

import java.util.List;

import com.ewcms.content.document.model.Citizen;


/**
 *
 * @author 吴智俊
 */
public interface CitizenServiceable {
	/**
	 * 新增公民信息
	 * 
	 * @param citizen 公民对象
	 * @return
	 */
	public Integer addCitizen(Citizen citizen);
	
	/**
	 * 修改公民信息
	 * 
	 * @param citizen 公民对象
	 * @return
	 */
	public Integer updCitizen(Citizen citizen);
	
	/**
	 * 查询公民对象
	 * 
	 * @param citizenId 公民编号
	 * @return 公民对象
	 */
	public Citizen getCitizen(Integer citizenId);
	
	/**
	 * 删除公民对象
	 * 
	 * @param citizenId 公民对象
	 */
	public void delCitizen(Integer citizenId);
	
	/**
	 * 查询所有公民对象
	 * 
	 * @return 公民对象集合
	 */
	public List<Citizen> getAllCitizen();
}
