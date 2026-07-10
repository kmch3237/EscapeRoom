package com.noexit.app.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Service;

import com.noexit.app.common.PaginateUtil;
import com.noexit.app.common.PagingUtil;
import com.noexit.app.mapper.MyReservationMapper;
import com.noexit.app.model.CancelMailDTO;
import com.noexit.app.model.MyReservationDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class MyReservationServiceImpl implements MyReservationService {

	// 주요 속성 구성
	private final MyReservationMapper mapper;
	private final PaginateUtil paginateUtil;
	private final PagingUtil pagingUtil;      // Thymeleaf 페이징용 (숫자 계산)
	private final MailService mailService;

	@Override
	public int bookedCount(Long userId) {

		int result = 0;	
		
		try {
			result = mapper.bookedCount(userId);
			
		} catch (Exception e) {
			log.info("bookedCount: ", e);
			throw e;
		}
		return result;
	}


	@Override
	public int doneCount(Long userId) {
		int result = 0;	
		
		try {
			result = mapper.doneCount(userId);
			
		} catch (Exception e) {
			log.info("doneCount: ", e);
			throw e;
		}
		return result;
	}

	@Override
	public int canceledCount(Long userId) {
		int result = 0;	
		
		try {
			result = mapper.canceledCount(userId);
			
		} catch (Exception e) {
			log.info("canceledCount: ", e);
			throw e;
		}
		return result;
	}
	
	
	
	@Override
	public List<MyReservationDTO> bookedList(Map<String, Object> map) {
		
		List<MyReservationDTO> list = new ArrayList<>();
		
		try {
			list = mapper.bookedList(map);
		} catch (Exception e) {
			log.error("bookedList: ", e);
		}
		
		return list;
	}

	@Override
	public List<MyReservationDTO> doneList(Map<String, Object> map) {
		List<MyReservationDTO> list = new ArrayList<>();
		
		try {
			list = mapper.doneList(map);
		} catch (Exception e) {
			log.error("doneList: ", e);
		}
		
		return list;
	}

	@Override
	public List<MyReservationDTO> canceledList(Map<String, Object> map) {
		List<MyReservationDTO> list = new ArrayList<>();
		
		try {
			list = mapper.canceledList(map);
		} catch (Exception e) {
			log.error("canceledList: ", e);
		}
		
		return list;
	}


	@Override
	public Map<String, Object> getReservationPageData(Long userId, int currentTab, int currentPage, int size) {
		
		Map<String, Object> result = new HashMap<>();
		
		try {
			
			int totalPage = 0;
			int dataCount = 0;
			
			// 탭 별 전체 페이지 수
			if(currentTab==1) {
				dataCount = bookedCount(userId);
			}else if(currentTab==2) {
				dataCount = doneCount(userId);
			}else if(currentTab==3) {
				dataCount = canceledCount(userId);
			}
			
			if(dataCount!=0)
				totalPage = paginateUtil.pageCount(dataCount, size);
			
			// 리스트에 출력할 데이터 가져오기
			int offset = (currentPage - 1) * size;
			if(offset<0)
				offset = 0;
			
			Map<String, Object> map = new HashMap<>();
			map.put("offset", offset);
			map.put("size", size);
			map.put("userId", userId);
			
			// 탭 별 목록 조회
			List<MyReservationDTO> list = new ArrayList<>();
			
			if(currentTab==1) {
				list = bookedList(map);
			}else if(currentTab==2) {
				list = doneList(map);
			}else if(currentTab==3) {
				list = canceledList(map);
			}
			
			// 페이징 url 구성 (JSP용 HTML 문자열 - 기존 유지)
			String listUrl = "/reservations?tab="+currentTab;
			String paging = paginateUtil.paging(currentPage, totalPage, listUrl);

			// Thymeleaf용 블록 페이징 숫자 (내 PagingUtil)
			int startPage = pagingUtil.startPage(currentPage);
			int endPage = pagingUtil.endPage(startPage, totalPage);

			 result.put("list", list);
		     result.put("dataCount", dataCount);
		     result.put("totalPage", totalPage);
		     result.put("paging", paging);
		     result.put("startPage", startPage);
		     result.put("endPage", endPage);
		     result.put("currentPage", currentPage);
		     result.put("bookedCount", bookedCount(userId));
			
		} catch (Exception e) {
			log.error("getReservationPageData: ",e);
		}
		return result;
	}


	@Override
	public void cancelReservation(Long reservationId, Long userId) throws Exception {
		
		try {
			
			// 프로시저 호출 (취소 + 트리거 party_drop 자동 Insert) 
			mapper.cancelReservation(reservationId, userId);
			
			// 예약취소에 엮인 파티장과 파티원들에게 취소 메일 보내기
			// 메일 보낼 대상 조회
			List<CancelMailDTO> mail = mailList(reservationId);
			
			// 메일 발송
			for(CancelMailDTO dto :mail) {
				mailService.sendCancelMail(dto);
				
			}
			
			
		} catch (DataAccessException e) {

			String errorMsg = e.getCause().getMessage();
			throw new Exception(errorMsg);
		
		} catch (Exception e) {
			log.error("cancelReservation: ", e);
			throw e;
		}

	}


	@Override
	public List<CancelMailDTO> mailList(Long reservationId) {
		List<CancelMailDTO> list = new ArrayList<>();
		
		try {
			
			list = mapper.mailList(reservationId);
			
		} catch (Exception e) {
			log.error("mailList: ",e);
		}
		
		
		return list;
	}

	
	
	
	
	
	
}
