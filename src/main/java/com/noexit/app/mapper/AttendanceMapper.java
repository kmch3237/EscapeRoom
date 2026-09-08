package com.noexit.app.mapper;

import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import com.noexit.app.model.AttendanceListDTO;
import com.noexit.app.model.AttendCrew;
import com.noexit.app.model.AttendItemDTO;
import com.noexit.app.model.Manner;

@Mapper
public interface AttendanceMapper {
	
	public List<AttendanceListDTO> selectListByOwnerUserId(Map<String, Object> map);
	public List<AttendanceListDTO> selectListByManagerUserId(Map<String, Object> map);
	public int dataCountByOwnerUserId(Map<String, Object> map);
	public int dataCountByManagerUserId(Map<String, Object> map);

	// 이 예약이 staffUserId 가 사장이거나 활성 매니저인 카페의 것인지 (1/0)
	public int countStaffReservation(@Param("reservationId") Long reservationId,
	                                 @Param("staffUserId") Long staffUserId);

	// 한 예약의 파티원 리스트
	public List<AttendCrew> selectCrewByReservationId(@Param("reservationId") Long reservationId,
	                                                  @Param("staffUserId") Long staffUserId);
	
    public int selectAttendanceExists(Long reservationId);
    public void insertAttendance(AttendItemDTO item);

    public void insertAttendDetail(AttendItemDTO item);

	// 파티원 1명당 출석 상태 INSERT
	public void insertAttendDetailByUser(AttendItemDTO item);

	// 노쇼 시 매너온도 차감
	public void callInsertNoshow(Manner manner);
	
	public Long selectAttendanceIdByReservationId(Long reservationId);

	// 출석기록 페이지용
	public List<AttendanceListDTO> selectHistoryByOwnerUserId(Map<String, Object> map);
	public List<AttendanceListDTO> selectHistoryByManagerUserId(Map<String, Object> map);
	public int dataCountHistoryByOwnerUserId(Map<String, Object> map);
	public int dataCountHistoryByManagerUserId(Map<String, Object> map);
	
	// 출석 기록 상세
	public List<AttendCrew> selectHistoryDetail(@Param("reservationId") Long reservationId,
	                                            @Param("staffUserId") Long staffUserId);

}
