/*
 * Copyright (c)2010 Jiangxi Institute of Computing Technology(JICT), All rights reserved.
 * JICT PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 * http://www.jict.org
 */
package com.ewcms.content.document.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ewcms.content.document.dao.CitizenDAO;
import com.ewcms.content.document.model.Citizen;

/**
 *
 * @author 吴智俊
 */
@Service()
public class CitizenService implements CitizenServiceable {
	
	@Autowired
	private CitizenDAO citizenDAO;

	@Override
	public Integer addCitizen(Citizen citizen) {
		Citizen isCitizen = citizenDAO.findCitizenByCitizenName(citizen.getName());
		if (isCitizen != null) return null;
		citizenDAO.persist(citizen);
		return citizen.getId();
	}

	@Override
	public void delCitizen(Integer citizenId) {
		citizenDAO.removeByPK(citizenId);
	}

	@Override
	public List<Citizen> getAllCitizen() {
		return citizenDAO.findAll();
	}

	@Override
	public Citizen getCitizen(Integer citizenId) {
		return citizenDAO.get(citizenId);
	}

	@Override
	public Integer updCitizen(Citizen citizen) {
		Citizen isCitizen = citizenDAO.findCitizenByCitizenIdAndCitizenName(citizen.getId(), citizen.getName());
		if (isCitizen != null) return null;
		citizenDAO.merge(citizen);
		return citizen.getId();
	}

}
