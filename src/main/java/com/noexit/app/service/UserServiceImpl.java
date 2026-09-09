package com.noexit.app.service;

import java.security.SecureRandom;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.noexit.app.mapper.UserMapper;
import com.noexit.app.model.User;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserServiceImpl implements UserService {

	private final UserMapper userMapper;
	private final ManagerService managerService;
	private final MailService mailService;
	private final PasswordEncoder passwordEncoder;   // BCrypt

	// 인증번호 정책. 매직 넘버를 코드 안에 흩어두면 나중에 바꿀 때 빠뜨린다.
	private static final long AUTH_CODE_VALID_MS = 5 * 60 * 1000L;   // 유효시간 5분
	private static final int  AUTH_CODE_MAX_TRY  = 5;                // 최대 시도 5회

	// SecureRandom 은 스레드 안전하고 생성 비용이 있으므로 한 번만 만들어 재사용한다.
	private static final SecureRandom RANDOM = new SecureRandom();

	@Override
	@Transactional
	public void enroll(User user) {
		// 원문 비번을 BCrypt 해시로 바꿔 저장 (SQL 은 해시 그대로 INSERT)
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		userMapper.insertAccount(user);
		userMapper.insertInfo(user);
	}

	@Override
	public int countByLoginId(String loginId) {
		 return userMapper.countByLoginId(loginId);
	}

	@Override
	public int countByEmail(String email) {
		 return userMapper.countByEmail(email);
	}

	@Override
	public User login(User user) {

		// 1) loginId 로 계정 조회 (해시 비번 포함)
		User dto = userMapper.selectByLoginId(user);

		// 2) 계정 없음 → 로그인 실패 (null 반환)
		if (dto == null) {
			return null;
		}

		// 3) 입력 비번 vs 저장된 해시 비교 (BCrypt matches)
		if (!passwordEncoder.matches(user.getPassword(), dto.getPassword())) {
			return null;   // 비번 불일치 → 실패
		}

		// 4) 성공 → 비번 해시는 화면/세션에 안 남기고 지움
		dto.setPassword(null);
		return dto;
	}

	@Override
	public User findByLoginId(String loginId) {
		User dto = null;
		try {
			dto = userMapper.findByLoginId(loginId);
		} catch (Exception e) {
			log.info("findByLoginId : ", e);
		}
		return dto;
	}

	@Override
	public String findRole(Long userId) {

		// 쿼리가 깨진 것과 "해당 없음"은 다르다.
		// 예외를 삼키면 DB 오류가 '권한 없는 일반 회원'으로 둔갑하므로 잡지 않는다.
		if (userMapper.countCafeByUserId(userId) > 0)
			return "OWNER";

		if (managerService.countActiveByUserId(userId) > 0)
			return "MANAGER";

		return "USER";
	}
	
	@Override
	public User findByNameAndEmail(User user) {

	    User dto = null;

	    try {
	        dto = userMapper.findByNameAndEmail(user);
	    } catch (Exception e) {
	        log.info("findByNameAndEmail : ", e);
	    }

	    return dto;
	}


	// 비밀번호 찾기 인증번호 발송
	@Override
	public boolean sendAuthCode(String loginId, String name, HttpSession session) {

		try {
			User param = new User();
			param.setLoginId(loginId);
			param.setName(name);

			User dto = userMapper.findByLoginIdAndName(param);
			if (dto == null) return false;

			// 6자리 인증번호.
			// java.util.Random 은 시드를 알면 다음 값을 계산할 수 있어서 인증에 쓰면 안 된다.
			// SecureRandom 은 OS 가 모은 엔트로피를 쓰므로 다음 값을 예측할 수 없다.
			String authCode = String.valueOf(100000 + RANDOM.nextInt(900000));

			session.setAttribute("authCode", authCode);
			session.setAttribute("authCodeLoginId", loginId);

			// 발급 시각. 이게 없으면 '언제 만들었는지'를 몰라 만료 자체를 만들 수 없다.
			session.setAttribute("authCodeCreatedAt", System.currentTimeMillis());

			// 시도 횟수. 재발송하면 새 코드이므로 기회도 0 부터 다시 준다.
			session.setAttribute("authCodeTryCount", 0);

			// 이전에 인증을 통과한 흔적을 반드시 지운다.
			// 안 지우면 '내 계정으로 인증 성공 -> 남의 아이디로 재발송' 만으로
			// resetPassword 의 authCodeVerified 검사를 그대로 통과해 버린다.
			session.removeAttribute("authCodeVerified");

			mailService.sendAuthCodeMail(dto.getEmail(), authCode);
			return true;

		} catch (Exception e) {
			log.info("sendAuthCode : ", e);
			return false;
		}
	}


	// 인증번호 검증
	@Override
	public boolean verifyAuthCode(String loginId, String authCode, HttpSession session) {

		String savedCode = (String) session.getAttribute("authCode");
		String savedLoginId = (String) session.getAttribute("authCodeLoginId");

		if (savedCode == null || savedLoginId == null) return false;
		if (!savedLoginId.equals(loginId)) return false;

		// (1) 만료 검사. 틀렸을 때가 아니라 '시작하자마자' 본다.
		//     코드가 맞아도 시간이 지났으면 거부해야 하기 때문이다.
		Long createdAt = (Long) session.getAttribute("authCodeCreatedAt");
		if (createdAt == null || System.currentTimeMillis() - createdAt > AUTH_CODE_VALID_MS) {
			clearAuthCode(session);   // 만료된 코드를 살려둘 이유가 없다. 즉시 폐기.
			return false;
		}

		// (2) 시도 횟수 검사. 넘었으면 거부만 하지 말고 코드를 버린다.
		//     거부만 하고 코드를 살려두면 계속 두드릴 여지가 남는다.
		Integer tryCount = (Integer) session.getAttribute("authCodeTryCount");
		if (tryCount == null) tryCount = 0;
		if (tryCount >= AUTH_CODE_MAX_TRY) {
			clearAuthCode(session);
			return false;
		}

		// (3) 코드 비교. 틀리면 '세고 나서' 돌려보낸다.
		//     세지 않고 검사만 하면 tryCount 가 영원히 0 이라 위 (2)가 절대 참이 되지 않는다.
		if (!savedCode.equals(authCode)) {
			session.setAttribute("authCodeTryCount", tryCount + 1);
			return false;
		}

		// (4) 성공. 인증번호는 1회용이므로 즉시 폐기한다.
		//     남겨두면 같은 코드로 몇 번이든 다시 통과할 수 있다.
		//     authCodeLoginId 는 resetPassword 가 아직 써야 하므로 여기서 지우지 않는다.
		clearAuthCode(session);
		session.setAttribute("authCodeVerified", true);
		return true;
	}


	// 인증번호 관련 세션 값을 한꺼번에 지운다.
	// 지워야 하는 자리가 네 군데(만료/횟수초과/인증성공/비번변경)라 빠뜨리지 않으려고 메서드로 뺐다.
	private void clearAuthCode(HttpSession session) {
		session.removeAttribute("authCode");
		session.removeAttribute("authCodeCreatedAt");
		session.removeAttribute("authCodeTryCount");
	}


	// 비밀번호 변경
	@Override
	public int resetPassword(String loginId, String newPassword, HttpSession session) {

		Boolean verified = (Boolean) session.getAttribute("authCodeVerified");
		String savedLoginId = (String) session.getAttribute("authCodeLoginId");

		if (verified == null || !verified) return 0;
		if (savedLoginId == null || !savedLoginId.equals(loginId)) return 0;

		int result = 0;

		try {
			User dto = userMapper.findByLoginId(loginId);
			if (dto == null) return 0;

			// 새 비번도 BCrypt 해시로 저장
			dto.setPassword(passwordEncoder.encode(newPassword));
			result = userMapper.updatePassword(dto);

			// 세션 정리. 새로 생긴 발급시각·시도횟수까지 같이 지운다.
			clearAuthCode(session);
			session.removeAttribute("authCodeLoginId");
			session.removeAttribute("authCodeVerified");

		} catch (Exception e) {
			log.info("resetPassword : ", e);
		}

		return result;
	}

	// 회원탈퇴
	@Override
	@Transactional
	public void withdraw(Long userId) {
		try {
			userMapper.insertUserDrop(userId);
			userMapper.deleteUserInfo(userId);
		} catch (Exception e) {
			log.info("withdraw : ", e);
			throw e;
		}
	}

	@Override
	public boolean verifyPassword(Long userId, String password) {
		// 저장된 해시를 꺼내와 입력 비번과 BCrypt 비교
		String storedHash = userMapper.selectPasswordByUserId(userId);
		if (storedHash == null) {
			return false;
		}
		return passwordEncoder.matches(password, storedHash);
	}
}

