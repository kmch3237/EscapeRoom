package com.noexit.app.service;

import java.util.List;
import java.util.Map;

import com.noexit.app.model.AttendanceListDTO;
import com.noexit.app.model.AttendCrew;
import com.noexit.app.model.AttendForm;

import jakarta.servlet.http.HttpSession;

public interface AttendanceService {

	// 출석체크 목록 조회 
	public List<AttendanceListDTO> selectListByOwnerUserId(Map<String, Object> map);
	// 매니저 출석체크 목록 조회 
	public List<AttendanceListDTO> selectListByManagerUserId(Map<String, Object> map);
	// 출석체크 목록 갯수 (역할별)
	public int dataCountByRole(Map<String, Object> map, String role);

	// 이 예약이 내(사장/매니저) 카페 것인지 검증. 아니면 예외.
	public void assertMyReservation(Long reservationId, Long staffUserId);

	public List<AttendCrew> selectCrewByReservationId(Long reservationId, Long staffUserId);

	// 개별 출석체크 임시저장 (세션 누적용)
	public void saveDraft(AttendForm form, HttpSession session, Long staffUserId) throws Exception;

	// 최종확인용 draft 리스트를 ATTENDANCE + ATTENDANCE_DETAIL에 INSERT
	public void finalizeAttendance(HttpSession session, Long staffUserId) throws Exception;
	
	// 역할에 따른 출석 목록 조회
	public List<AttendanceListDTO> selectAttendListByRole(Map<String, Object> map, String role);
	
	// draft 기준 done/partial 분류
	public Map<String, List<Long>> checkStatus(HttpSession session, List<AttendanceListDTO> attendList);

	// 출석기록 목록 (역할별)
	public List<AttendanceListDTO> selectHistoryByRole(Map<String, Object> map, String role);
	// 출석기록 갯수 (역할별)
	public int dataCountHistoryByRole(Map<String, Object> map, String role);
	
	public List<AttendCrew> getCrewDraftStatus(Long reservationId, HttpSession session, Long staffUserId);
	
	// 출석기록 상세 
	public List<AttendCrew> selectHistoryDetail(Long reservationId, Long staffUserId);

}
