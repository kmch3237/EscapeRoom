package com.noexit.app.service;

import java.util.List;
import java.util.Map;

import com.noexit.app.model.Manager;

public interface ManagerService {

	public List<Manager> selectActiveByOwnerUserId(Map<String, Object> map);
	// loginUserId = 세션에서 꺼낸 사장 본인 id. 브라우저가 못 건드리는 값이라 소유권 판정의 기준이 된다.
	public void enroll(Manager manager, Long loginUserId) throws Exception;
	public void deact(Manager manager, Long loginUserId) throws Exception;
	public int countActiveByUserId(Long userId);
	public int dataCount(Map<String, Object> map);
}
