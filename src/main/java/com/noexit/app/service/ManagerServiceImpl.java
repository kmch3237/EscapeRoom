package com.noexit.app.service;

import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.noexit.app.mapper.CafeMapper;
import com.noexit.app.mapper.ManagerMapper;
import com.noexit.app.model.Cafe;
import com.noexit.app.model.Manager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class ManagerServiceImpl implements ManagerService {

	private final ManagerMapper mapper;
	// 카페 주인이 누구인지 확인하려고 주입. @RequiredArgsConstructor 가 생성자를 다시 만들어준다.
	private final CafeMapper cafeMapper;

	// 인가 검사 : 이 cafeId 가 정말 이 사람 카페인가?
	// 비교 기준은 항상 [DB에서 꺼낸 그 카페의 주인] vs [세션에서 온 나].
	// 둘 중 하나라도 브라우저에서 온 값이면 검사 자체가 무의미해진다.
	private void assertOwnCafe(Long cafeId, Long loginUserId) {
		Cafe cafe = cafeMapper.selectByCafeId(cafeId);
		// || 는 앞이 참이면 뒤를 평가하지 않는다(단축 평가).
		// 그래서 null 검사가 앞에 있어야 뒤의 cafe.getUserId() 에서 NPE 가 안 난다.
		// Long 은 객체라 == 로 비교하면 128 이상에서 값이 같아도 false 가 된다 → equals 필수.
		if (cafe == null || ! cafe.getUserId().equals(loginUserId)) {
			throw new IllegalStateException("본인 카페가 아닙니다. cafeId=" + cafeId + ", loginUserId=" + loginUserId);
		}
	}

	// 매니저 리스트 조회 (페이징 포함)
	@Override
	public List<Manager> selectActiveByOwnerUserId(Map<String, Object> map) {
		List<Manager> list = null;
		try {
			list = mapper.selectActiveByOwnerUserId(map);
		} catch (Exception e) {
			log.info("selectActiveByOwnerUserId : ", e);
		}
		return list;
	}

	// 매니저 갯수 확인
	@Override
	public int dataCount(Map<String, Object> map) {
		int result = 0;
		try {
			result = mapper.dataCount(map);
		} catch (Exception e) {
			log.info("dataCount : ", e);
		}
		return result;
	}

	@Override
	public void enroll(Manager manager, Long loginUserId) throws Exception {
		assertOwnCafe(manager.getCafeId(), loginUserId);
		// SQL 의 WHERE 가 쓸 값. 세션에서 온 값이라 브라우저가 바꿀 수 없다.
		manager.setOwnerUserId(loginUserId);
		try {
			// 0건이면 SQL 이 막았다는 뜻. 예외가 안 나므로 직접 확인해서 던진다.
			if (mapper.insertEnroll(manager) == 0) {
				throw new IllegalStateException("매니저 등록 실패 : 본인 카페가 아닙니다. cafeId=" + manager.getCafeId());
			}
		} catch (Exception e) {
			log.info("enroll : ", e);
			throw e;
		}
	}

	@Override
	public void deact(Manager manager, Long loginUserId) throws Exception {
		assertOwnCafe(manager.getCafeId(), loginUserId);
		manager.setOwnerUserId(loginUserId);
		try {
			if (mapper.insertDeact(manager) == 0) {
				throw new IllegalStateException("매니저 해제 실패 : 본인 카페가 아닙니다. cafeId=" + manager.getCafeId());
			}
		} catch (Exception e) {
			log.info("deact : ", e);
			throw e;
		}
	}

	@Override
	public int countActiveByUserId(Long userId) {
		int count = 0;
		try {
			count = mapper.countActiveByUserId(userId);
		} catch (Exception e) {
			log.info("countActiveByUserId : ", e);
		}
		return count;
	}
}
