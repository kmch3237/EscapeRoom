package com.noexit.app.controller;

import com.noexit.app.common.AuthUtil;
import com.noexit.app.common.PaginateUtil;
import com.noexit.app.model.AttendCrew;
import com.noexit.app.model.AttendForm;
import com.noexit.app.model.AttendanceListDTO;
import com.noexit.app.model.User;
import com.noexit.app.service.AttendanceService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/owner/attendance")
public class OwnerAttendanceController {

    private final AttendanceService attendanceService;
    private final PaginateUtil paginateUtil;

    @GetMapping("") // 출석체크
    public String attendance(@RequestParam(name = "page", defaultValue = "1") int currentPage,
                             HttpSession session, HttpServletRequest req, Model model) {
        String redirect = AuthUtil.checkStaff(session);
        if (redirect != null)
        	return redirect;

        User loginUser = (User) session.getAttribute("loginUser");
        String role = (String) session.getAttribute("role");

        try {
            int size = 10; 
            Map<String, Object> map = new HashMap<>();
            Long userId = loginUser.getUserId();
            map.put("ownerUserId", userId);
            map.put("managerUserId", userId);

            // 데이터 개수 파악
            int dataCount = attendanceService.dataCountByRole(map, role);
            String listUrl = req.getContextPath() + "/owner/attendance";
            currentPage = paginateUtil.preparePage(currentPage, size, dataCount, map, listUrl, model);
            // 리스트 가져오기
            List<AttendanceListDTO> attendList = attendanceService.selectAttendListByRole(map, role);

            Map<String, List<Long>> status = attendanceService.checkStatus(session, attendList);

            model.addAttribute("doneList", status.get("done"));
            model.addAttribute("partialList", status.get("partial"));
            model.addAttribute("attendList", attendList);

        } catch (Exception e) {
            log.info("attendance : ", e);
        }

        return "owner/attendance";
    }

    @GetMapping("/check")
    public String check(@RequestParam(name = "reservationId") Long reservationId
    				  , HttpSession session, Model model) {
        
    	String redirect = AuthUtil.checkStaff(session);
        
    	if (redirect != null) 
        	return redirect;
        
        // reservationId 는 주소창에서 바꿀 수 있는 값이다. 세션의 userId 로 소유를 확인한다.
        User loginUser = (User) session.getAttribute("loginUser");

        List<AttendCrew> crewList;
        try {
            // 이전 선택값 복원
            crewList = attendanceService.getCrewDraftStatus(reservationId, session, loginUser.getUserId());
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("check 접근 거부 : ", e);
            return "redirect:/owner/attendance";
        }

        model.addAttribute("reservationId", reservationId);
        model.addAttribute("crewList", crewList);
        return "owner/attendanceCheck";
    }

    // 개별 출석체크 임시저장 (세션 누적)
    @PostMapping("/saveDraft")
    public String saveDraft(AttendForm form, HttpSession session) {
        String redirect = AuthUtil.checkStaff(session);
        if (redirect != null) 
        	return redirect;

        User loginUser = (User) session.getAttribute("loginUser");

        try {
            attendanceService.saveDraft(form, session, loginUser.getUserId());
        } catch (Exception e) {
            log.info("saveDraft : ", e);
        }
        return "redirect:/owner/attendance";
    }

    // 최종확인버튼 : ATTENDANCE + ATTENDANCE_DETAIL INSERT
    @PostMapping("/finalize")
    public String finalizeAttend(HttpSession session) {
        String redirect = AuthUtil.checkStaff(session);
        if (redirect != null) return redirect;

        try {
            User loginUser = (User) session.getAttribute("loginUser");
            attendanceService.finalizeAttendance(session, loginUser.getUserId());
        } catch (Exception e) {
            log.info("finalize : ", e);
        }
        return "redirect:/owner/attendance";
    }

    @GetMapping("/history") // 출석기록
    public String history(@RequestParam(name = "page", defaultValue = "1") int currentPage
                        , HttpSession session, HttpServletRequest req, Model model) {
        String redirect = AuthUtil.checkStaff(session);
        if (redirect != null)
        	return redirect;

        User loginUser = (User) session.getAttribute("loginUser");
        String role = (String) session.getAttribute("role");

        try {
            int size = 10; // 한 화면에 보여주는 기록 수
            Map<String, Object> map = new HashMap<>();
            Long userId = loginUser.getUserId();
            map.put("ownerUserId", userId);
            map.put("managerUserId", userId);

            // 데이터 개수 파악
            int dataCount = attendanceService.dataCountHistoryByRole(map, role);
            String listUrl = req.getContextPath() + "/owner/attendance/history";
            // 페이지 번호 보정
            currentPage = paginateUtil.preparePage(currentPage, size, dataCount, map, listUrl, model);
            // 리스트 가져오기
            List<AttendanceListDTO> historyList = attendanceService.selectHistoryByRole(map, role);
            
            model.addAttribute("historyList", historyList);


        } catch (Exception e) {
            log.info("history : ", e);
        }

        return "owner/attendanceHistory";
    }
    
    // 출석기록 상세 (모달용 AJAX)
    @ResponseBody
    @GetMapping("/history/detail")
    public List<AttendCrew> historyDetail(@RequestParam(name = "reservationId") Long reservationId,
                                          HttpSession session) {
        String redirect = AuthUtil.checkStaff(session);
        if (redirect != null)
            return null;

        User loginUser = (User) session.getAttribute("loginUser");

        try {
            return attendanceService.selectHistoryDetail(reservationId, loginUser.getUserId());
        } catch (IllegalStateException | IllegalArgumentException e) {
            log.warn("historyDetail 접근 거부 : ", e);
            return List.of();
        }
    }

}