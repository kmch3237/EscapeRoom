package com.noexit.app.controller;

import java.io.IOException;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.noexit.app.model.User;
import com.noexit.app.model.UserEnrollForm;
import com.noexit.app.service.MailService;
import com.noexit.app.service.UserService;

import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Controller
@Slf4j
@RequiredArgsConstructor
@RequestMapping("/user")
public class UserController {

	private final UserService service;
	private final MailService mailService;

	 // 아이디 중복확인 (Ajax)
	 @PostMapping("/id-check")
	 public void idCheck(User user, HttpServletResponse response) throws IOException {
	     int count = service.countByLoginId(user.getLoginId());
	     response.setContentType("text/html; charset=UTF-8");
	     response.getWriter().print(count == 0 ? "OK" : "NO");
	  }

	 // 이메일 중복확인 (Ajax)
	 @PostMapping("/email-check")
	 public void emailCheck(User user, HttpServletResponse response) throws IOException {
	     int count = service.countByEmail(user.getEmail());
	     response.setContentType("text/html; charset=UTF-8");
	     response.getWriter().print(count == 0 ? "OK" : "NO");
	  }
	
	
	// 아이디 찾기 폼
	@GetMapping("/findId")
	public String findIdForm() {
		return "user/findId";
	}
	
	@PostMapping("/findId")
	public void findId(User user, HttpServletResponse response) throws IOException {

	    User dto = service.findByNameAndEmail(user);

	    response.setContentType("text/plain;charset=UTF-8");

	    if (dto == null) {
	        response.getWriter().print("NOT_FOUND");
	        return;
	    }

	    mailService.sendUserIdMail(dto.getEmail(), dto.getLoginId());

	    response.getWriter().print("SUCCESS");
	}
	
	

	// 비밀번호 찾기 폼
	@GetMapping("/findPw")
	public String findPwForm() {
		return "user/findPw";
	}

	// 비밀번호 찾기 인증번호 발송
	@PostMapping("/findPwAuth")
	public void findPwAuth(@RequestParam(name = "name") String name
	                     , @RequestParam(name = "userId") String loginId
	                     , HttpSession session, HttpServletResponse response) throws IOException {

		boolean ok = service.sendAuthCode(loginId, name, session);

		response.setContentType("text/plain;charset=UTF-8");
		response.getWriter().print(ok ? "SUCCESS" : "NOT_FOUND");
	}

	// 인증번호 검증
	@PostMapping("/verifyCode")
	public void verifyCode(@RequestParam(name = "userId") String loginId
	                     , @RequestParam(name = "authCode") String authCode
	                     , HttpSession session, HttpServletResponse response) throws IOException {

		boolean ok = service.verifyAuthCode(loginId, authCode, session);

		response.setContentType("application/json;charset=UTF-8");
		if (ok) {
			response.getWriter().print("{\"status\":\"success\"}");
		} else {
			response.getWriter().print("{\"status\":\"fail\",\"message\":\"인증번호가 올바르지 않습니다.\"}");
		}
	}

	// 비밀번호 변경
	@PostMapping("/resetPw")
	public void resetPw(@RequestParam(name = "userId") String loginId
	                  , @RequestParam(name = "newPw") String newPw
	                  , HttpSession session, HttpServletResponse response) throws IOException {

		int result = service.resetPassword(loginId, newPw, session);

		response.setContentType("application/json;charset=UTF-8");
		if (result > 0) {
			response.getWriter().print("{\"status\":\"success\"}");
		} else {
			response.getWriter().print("{\"status\":\"fail\",\"message\":\"비밀번호 변경에 실패했습니다.\"}");
		}
	}

	// 회원가입 폼
	@GetMapping("/enroll")
	public String enrollForm(Model model) {
		// th:object 가 바인딩할 빈 폼 객체를 미리 넣어줌 (없으면 th:object 에러)
		model.addAttribute("enrollForm", new UserEnrollForm());
		return "user/enrollForm";
	}

	// 회원가입 처리
	@PostMapping("/enroll")
	public String enroll(@Valid @ModelAttribute("enrollForm") UserEnrollForm form,
						 BindingResult result) {
		// 서버 검증 실패 → 폼으로 되돌림 (th:errors 가 메시지 표시). JS 우회 공격도 여기서 막힘
		if (result.hasErrors()) {
			return "user/enrollForm";
		}
		service.enroll(form.toUser());   // 검증 통과분만 도메인으로 변환해 저장
		return "redirect:/user/login";   // 가입 후 로그인 페이지로
	}

	// 로그인 폼
	@GetMapping("/login")
	public String loginForm() {
		return "user/loginForm";
	}

	// 로그인 처리
	@PostMapping("/login")
	public String login(User user, HttpSession session, Model model) {

	    User dto = null;
	    try {
	        dto = service.login(user);
	    } catch (Exception e) {
	        log.info("login : ", e);
	    }

	    if (dto == null) {
	        model.addAttribute("errorMessage", "아이디 또는 비밀번호가 올바르지 않습니다.");
	        return "user/loginForm";
	    }

	    String role = service.findRole(dto.getUserId());
	    session.setAttribute("loginUser", dto);
	    session.setAttribute("role", role);
	    return "redirect:/";
	}

	// 로그아웃
	@GetMapping("/logout")
	public String logout(HttpSession session) {
		session.invalidate();
		return "redirect:/user/login";
	}

	// 회원탈퇴 폼
	@GetMapping("/withdraw")
	public String withdrawForm(HttpSession session) {
		if (session.getAttribute("loginUser") == null) return "redirect:/user/login";
		return "user/withdrawForm";
	}

	// 회원탈퇴
	@PostMapping("/withdraw")
	public String withdraw(@RequestParam(name = "password") String password, HttpSession session, Model model) {
		User loginUser = (User) session.getAttribute("loginUser");
		if (loginUser == null) return "redirect:/user/login";
		if (!service.verifyPassword(loginUser.getUserId(), password)) {
			model.addAttribute("errorMessage", "비밀번호가 일치하지 않습니다.");
			return "user/withdrawForm";
		}
		try {
			service.withdraw(loginUser.getUserId());
		} catch (Exception e) {
			log.info("withdraw : ", e);
			return "redirect:/mypage/record";
		}
		session.invalidate();
		return "redirect:/";
	}

}
