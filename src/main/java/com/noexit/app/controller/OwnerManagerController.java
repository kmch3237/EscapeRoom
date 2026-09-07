package com.noexit.app.controller;

import com.noexit.app.common.AuthUtil;
import com.noexit.app.common.PaginateUtil;
import com.noexit.app.model.Manager;
import com.noexit.app.model.User;
import com.noexit.app.service.CafeService;
import com.noexit.app.service.ManagerService;
import com.noexit.app.service.UserService;
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
@RequestMapping("/owner/manager")
public class OwnerManagerController {

    private final ManagerService managerService;
    private final UserService userService;
    private final CafeService cafeService;
    private final PaginateUtil paginateUtil;

    @GetMapping("") // 매니저 관리
    public String manager(@RequestParam(name = "page", defaultValue = "1") int currentPage
                         , HttpSession session, HttpServletRequest req, Model model) {
        String redirect = AuthUtil.checkOwner(session);
        if (redirect != null)
        return redirect;

        User loginUser = (User)session.getAttribute("loginUser");

        try {
            int size = 5; // 한 화면에 보여주는 매니저 수

            Map<String, Object> map = new HashMap<>();
            map.put("ownerUserId", loginUser.getUserId());

            // 데이터 개수 파악
            int dataCount = managerService.dataCount(map);         
            String listUrl = req.getContextPath() + "/owner/manager";
            
            currentPage = paginateUtil.preparePage(currentPage, size, dataCount, map, listUrl, model);

            // 리스트 가져오기
            List<Manager> managerList = managerService.selectActiveByOwnerUserId(map);

            model.addAttribute("managerList", managerList);
            
        } catch (Exception e) {
            log.info("manager : ", e);
        }

        return "owner/manager";
    }

    @GetMapping("/enroll")
    public String managerEnrollForm(HttpSession session, Model model) {
        String redirect = AuthUtil.checkOwner(session);
        if (redirect != null)
        	return redirect;

        User loginUser = (User) session.getAttribute("loginUser");
        model.addAttribute("cafeList", cafeService.selectByUserId(loginUser.getUserId()));
        return "owner/managerEnrollForm";
    }

    @PostMapping("/enroll")
    public String managerEnroll(@RequestParam("loginId") String loginId
                              , @RequestParam("cafeId")  Long cafeId
                              , HttpSession session, Model model) {
        String redirect = AuthUtil.checkOwner(session);
        if (redirect != null) 
        	return redirect;

        User loginUser = (User) session.getAttribute("loginUser");
        User target = userService.findByLoginId(loginId);

        if (target == null) {
            model.addAttribute("errorMessage", "해당 아이디의 회원이 없습니다.");
            model.addAttribute("cafeList", cafeService.selectByUserId(loginUser.getUserId()));
            return "owner/managerEnrollForm";
        }

        Manager manager = new Manager();
        manager.setCafeId(cafeId);
        manager.setUserId(target.getUserId());
        try {
            // cafeId 는 브라우저에서 온 값이므로 그대로 믿지 않는다.
            // 세션의 내 userId 를 함께 넘겨서 서비스가 소유권을 판정하게 한다.
            managerService.enroll(manager, loginUser.getUserId());
        } catch (Exception e) {
            log.info("managerEnroll : ", e);
        }
        return "redirect:/owner/manager";
    }

    @PostMapping("/deact")
    public String managerDeact(@RequestParam("cafeId") Long cafeId
                              , @RequestParam("userId") Long userId
                              , HttpSession session) {
        String redirect = AuthUtil.checkOwner(session);
        if (redirect != null) 
        	return redirect;

        User loginUser = (User) session.getAttribute("loginUser");

        Manager manager = new Manager();
        manager.setCafeId(cafeId);
        manager.setUserId(userId);
        try {
            managerService.deact(manager, loginUser.getUserId());
        } catch (Exception e) {
            log.info("managerDeact : ", e);
        }
        return "redirect:/owner/manager";
    }
}