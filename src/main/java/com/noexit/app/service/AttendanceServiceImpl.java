package com.noexit.app.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noexit.app.mapper.AttendanceMapper;
import com.noexit.app.model.AttendanceListDTO;
import com.noexit.app.model.AttendCrew;
import com.noexit.app.model.AttendForm;
import com.noexit.app.model.AttendItemDTO;
import com.noexit.app.model.Manner;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class AttendanceServiceImpl implements AttendanceService {

	private final AttendanceMapper mapper;


	// 사장 출석 목록 (페이징 포함)
	@Override
	public List<AttendanceListDTO> selectListByOwnerUserId(Map<String, Object> map) {

		List<AttendanceListDTO> list = null;

		try {
			list = mapper.selectListByOwnerUserId(map);
		} catch (Exception e) {
			log.info("selectListByOwnerUserId : ", e);
		}

		return list;
	}

	// 매니저 출석 목록 (페이징 포함)
	@Override
	public List<AttendanceListDTO> selectListByManagerUserId(Map<String, Object> map) {

		List<AttendanceListDTO> list = null;

		try {
			list = mapper.selectListByManagerUserId(map);
		} catch (Exception e) {
			log.info("selectListByManagerUserId : ", e);
		}

		return list;
	}

	// 역할별 출석 목록 갯수
	@Override
	public int dataCountByRole(Map<String, Object> map, String role) {

		int result = 0;

		try {
			if ("OWNER".equals(role)) {
				result = mapper.dataCountByOwnerUserId(map);
			} else {
				result = mapper.dataCountByManagerUserId(map);
			}
		} catch (Exception e) {
			log.info("dataCountByRole : ", e);
		}

		return result;
	}

	// role 검사(checkStaff)는 "스태프냐"만 본다. 어느 카페 스태프인지는 보지 않으므로
	// 예약 한 건마다 "내 카페 것이냐"를 따로 물어야 한다. 연결 관계는 DB 에만 있으므로 DB 에 묻는다.
	@Override
	public void assertMyReservation(Long reservationId, Long staffUserId) {

		if (reservationId == null || staffUserId == null) {
			throw new IllegalArgumentException("출석 검증 파라미터 누락 : reservationId=" + reservationId);
		}

		if (mapper.countStaffReservation(reservationId, staffUserId) == 0) {
			// 존재하지 않는 예약과 남의 카페 예약을 구분해 알려주지 않는다(번호 넘겨짚기 방지).
			throw new IllegalStateException(
					"출석 접근 거부 : 내 카페 예약이 아님. reservationId=" + reservationId + ", userId=" + staffUserId);
		}
	}

	@Override
	public List<AttendCrew> selectCrewByReservationId(Long reservationId, Long staffUserId) {

		List<AttendCrew> list = null;

		try {
			list = mapper.selectCrewByReservationId(reservationId, staffUserId);
		} catch (Exception e) {
			log.info("selectCrewByReservationId : ", e);
		}

		return list;
	}



	// 개별 출석체크 임시저장
	@Override
	public void saveDraft(AttendForm form, HttpSession session, Long staffUserId) throws Exception {

		// 폼 값은 hidden 이어도 브라우저가 바꿀 수 있다. 세션에 담기 전에 검증한다.
		assertMyReservation(form.getReservationId(), staffUserId);

		try {
			@SuppressWarnings("unchecked")
			List<AttendItemDTO> drafts = (List<AttendItemDTO>) session.getAttribute("attendDraft");
			if (drafts == null) drafts = new ArrayList<>();

			// 같은 예약의 이전 draft 제거
			List<AttendItemDTO> newDrafts = new ArrayList<>();
			for (AttendItemDTO d : drafts) {
				if (!form.getReservationId().equals(d.getReservationId())) {
					newDrafts.add(d);
				}
			}

			// 이번 폼 항목 누적 (미정은 스킵)
			for (int i = 0; i < form.getUserIds().size(); i++) {
				Long statusId = form.getAttendStatusIds().get(i);
				if (statusId == null) continue;

				AttendItemDTO item = new AttendItemDTO();
				item.setReservationId(form.getReservationId());
				item.setUserId(form.getUserIds().get(i));
				item.setAttendStatusId(statusId);
				newDrafts.add(item);
			}

			session.setAttribute("attendDraft", newDrafts);

		} catch (Exception e) {
			log.info("saveDraft : ", e);
			throw e;
		}
	}


	// 최종확인 : ATTENDANCE + ATTENDANCE_DETAIL INSERT
	@Override
	@Transactional(rollbackFor = Exception.class)
	public void finalizeAttendance(HttpSession session, Long staffUserId) throws Exception {

		try {
			@SuppressWarnings("unchecked")
			List<AttendItemDTO> drafts = (List<AttendItemDTO>) session.getAttribute("attendDraft");
			if (drafts == null || drafts.isEmpty()) return;

			// 중복 없는 reservationId 모으기
			List<Long> resIds = new ArrayList<>();
			for (AttendItemDTO d : drafts) {
				if (!resIds.contains(d.getReservationId())) {
					resIds.add(d.getReservationId());
				}
			}

			// 예약별 처리
			for (Long reservationId : resIds) {

			    // draft 는 세션에 있지만 담길 때의 검증만으로는 부족하다.
			    // 매너온도를 깎는 쓰기 경로이므로 확정 직전에 한 번 더 확인한다.
			    assertMyReservation(reservationId, staffUserId);

			    AttendItemDTO head = new AttendItemDTO();

			    // 스케줄러가 먼저 박은 경우 → ATTENDANCE ID 재사용
			    Long existingId = mapper.selectAttendanceIdByReservationId(reservationId);

			    if (existingId != null) {
			        head.setAttendanceId(existingId);
			    } else {
			        // ATTENDANCE 
			        head.setReservationId(reservationId);
			        head.setUserId(staffUserId);
			        mapper.insertAttendance(head);
			    }

			    // ATTENDANCE_DETAIL 
			    for (AttendItemDTO dto : drafts) {
			        if (!reservationId.equals(dto.getReservationId())) continue;

			        dto.setAttendanceId(head.getAttendanceId());
			        mapper.insertAttendDetailByUser(dto);

			        // 노쇼면 매너온도 차감
			        if (dto.getAttendStatusId() != null && dto.getAttendStatusId() == 2L) {
			            Manner m = new Manner();
			            m.setUserId(dto.getUserId());
			            mapper.callInsertNoshow(m);
			        }			  
			    }
			}
			session.removeAttribute("attendDraft");
		} catch (Exception e) {
			log.info("finalizeAttendance : ", e);
			throw e;
		}
	}
	
	
	@Override
	public List<AttendanceListDTO> selectAttendListByRole(Map<String, Object> map, String role) {
	    if ("OWNER".equals(role)) {
	        return selectListByOwnerUserId(map);
	    } else {
	        return selectListByManagerUserId(map);
	    }
	}

	//draft 기준 done/partial 분류
	@Override
	public Map<String, List<Long>> checkStatus(HttpSession session, List<AttendanceListDTO> attendList) {
	   
		List<Long> doneList = new ArrayList<>();
	    List<Long> partialList = new ArrayList<>();

	    @SuppressWarnings("unchecked")
	    List<AttendItemDTO> draftList = (List<AttendItemDTO>) session.getAttribute("attendDraft");

	    if (draftList != null) {
	        // 중복 없는 reservationId 모으기
	        List<Long> resIds = new ArrayList<>();
	        for (AttendItemDTO d : draftList) {
	            if (!resIds.contains(d.getReservationId())) {
	                resIds.add(d.getReservationId());
	            }
	        }

	        // 예약별 draft 개수랑 TOTAL_MEMBER 비교
	        for (Long rid : resIds) {
	            int draftCount = 0;
	            for (AttendItemDTO d : draftList) {
	                if (rid.equals(d.getReservationId())) draftCount++;
	            }

	            int total = 0;
	            for (AttendanceListDTO a : attendList) {
	                if (rid.equals(a.getReservationId())) {
	                    total = a.getTotalMember();
	                    break;
	                }
	            }

	            if (draftCount >= total) {
	                doneList.add(rid);
	            } else {
	                partialList.add(rid);
	            }
	        }
	    }

	    Map<String, List<Long>> result = new HashMap<>();
	    result.put("done", doneList);
	    result.put("partial", partialList);

	    return result;
	}


	// 출석기록 목록 (역할별)
	@Override
	public List<AttendanceListDTO> selectHistoryByRole(Map<String, Object> map, String role) {

		List<AttendanceListDTO> list = null;

		try {
			if ("OWNER".equals(role)) {
				list = mapper.selectHistoryByOwnerUserId(map);
			} else {
				list = mapper.selectHistoryByManagerUserId(map);
			}
		} catch (Exception e) {
			log.info("selectHistoryByRole : ", e);
		}

		return list;
	}

	// 출석기록 갯수 (역할별)
	@Override
	public int dataCountHistoryByRole(Map<String, Object> map, String role) {

		int result = 0;

		try {
			if ("OWNER".equals(role)) {
				result = mapper.dataCountHistoryByOwnerUserId(map);
			} else {
				result = mapper.dataCountHistoryByManagerUserId(map);
			}
		} catch (Exception e) {
			log.info("dataCountHistoryByRole : ", e);
		}

		return result;
	}
	
	@Override
	public List<AttendCrew> getCrewDraftStatus(Long reservationId, HttpSession session, Long staffUserId) {

	    assertMyReservation(reservationId, staffUserId);

	    List<AttendCrew> crewList = selectCrewByReservationId(reservationId, staffUserId);

	    // 이전 선택값 복원
	    @SuppressWarnings("unchecked")
	    List<AttendItemDTO> drafts = (List<AttendItemDTO>) session.getAttribute("attendDraft");

	    if (drafts != null && crewList != null) {
	        for (AttendCrew c : crewList) {
	            for (AttendItemDTO d : drafts) {
	                if (reservationId.equals(d.getReservationId()) && c.getUserId().equals(d.getUserId())) {
	                    c.setAttendStatusId(d.getAttendStatusId());
	                    break;
	                }
	            }
	        }
	    }

	    return crewList;
	}
	
	// 출석기록 상세 (파티원별 확정 출석상태)
	@Override
	public List<AttendCrew> selectHistoryDetail(Long reservationId, Long staffUserId) {

	    assertMyReservation(reservationId, staffUserId);

	    List<AttendCrew> list = null;

	    try {
	        list = mapper.selectHistoryDetail(reservationId, staffUserId);
	    } catch (Exception e) {
	        log.info("selectHistoryDetail : ", e);
	    }

	    return list;
	}
	

}
