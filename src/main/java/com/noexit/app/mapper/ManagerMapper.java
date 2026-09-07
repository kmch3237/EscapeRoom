package com.noexit.app.mapper;

import java.sql.SQLException;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;

import com.noexit.app.model.Manager;

@Mapper
public interface ManagerMapper {

	public List<Manager> selectActiveByOwnerUserId(Map<String, Object> map);
	// int 반환 = 실제로 INSERT 된 행 수. WHERE 로 걸러져 0건이 될 수 있어 반드시 확인해야 한다.
	public int insertEnroll(Manager manager) throws SQLException;
	public int insertDeact(Manager manager) throws SQLException;
	public int countActiveByUserId(Long userId);
	public int dataCount(Map<String, Object> map);
}
