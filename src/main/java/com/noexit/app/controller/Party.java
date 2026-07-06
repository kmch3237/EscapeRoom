package com.noexit.app.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import com.noexit.app.common.PartyException;
import com.noexit.app.model.Cafe;
import com.noexit.app.model.PartyApplyDTO;
import com.noexit.app.model.PartyCommentDTO;
import com.noexit.app.model.PartyCommentDeleteDTO;
import com.noexit.app.model.PartyCrewDTO;
import com.noexit.app.model.PartyDTO;
import com.noexit.app.model.SearchFilterDTO;
import com.noexit.app.model.ThemeDTO;
import com.noexit.app.model.ThemeSlotDTO;
import com.noexit.app.model.User;
import com.noexit.app.service.PartyService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Controller
@RequestMapping("/party/*")
@RequiredArgsConstructor
public class Party
{
	private final PartyService service;

	@GetMapping("reservation/{partyId}")
	public String doReservation(@PathVariable(name="partyId") long partyId
							  , HttpSession session
							  , RedirectAttributes reModel)
	{
		try
		{
			// 로그인 체크
			User user = (User)session.getAttribute("loginUser");
			
			if(user == null)
			{
				reModel.addAttribute("errorMsg", "로그인 후 이용 가능합니다.");
				return "redirect:/err/login";
			}
			
			PartyDTO party = service.getPartyById(partyId);
			// 파티 유효성 검사
			if(party == null || "close".equals(party.getPartyStatus()))
			{
				reModel.addAttribute("errorMsg", "유효하지 않은 파티입니다.");
				return "redirect:/err/error";
			}
			if("confirm".equals(party.getPartyStatus()))
			{
				reModel.addAttribute("errorMsg", "이미 예약한 파티입니다.");
				return "redirect:/err/error";
			}
			
			// 파티장 검사
			if(user.getUserId() != party.getUserId())
			{
				reModel.addAttribute("errorMsg", "파티장만 예약할 수 있습니다.");
				return "redirect:/err/error";
			}
			
			ThemeSlotDTO slot = service.getThemeSlotById(party.getSlotId());
			// 슬롯 유효성 검사
			if(slot == null || 1 != slot.getStatus())
			{
				reModel.addAttribute("errorMsg", "유효하지 않은 예약 슬롯입니다.");
				return "redirect:/err/error";
			}
			
			List<PartyCrewDTO> crewList = service.getPartyCrewList(partyId);
			
			// 파티원 레디 검사
			if(crewList.stream().anyMatch(c->"UNREADY".equals(c.getReady())))
			{
				reModel.addAttribute("errorMsg", "준비하지 않은 파티원이 있습니다.");
				return "redirect:/err/error";
			}
				
			// 인원 수 검사
			if(crewList == null || slot.getMinPlayers() > crewList.size() || slot.getMaxPlayers() < crewList.size())
			{
				reModel.addAttribute("errorMsg", "인원 수가 맞지 않습니다.");
				return "redirect:/err/error";
			}
			
			// 성인 테마 검사
			if(slot.getAdult() == 1)
			{
				if(crewList.stream().anyMatch(c->c.getAge() < 19))
				{
					reModel.addAttribute("errorMsg", "미성년자가 포함된 파티는 성인 테마 예약이 불가합니다.");
					return "redirect:/err/error";
				}
			}
			
			Map<String, Object> map = new HashMap<>();
			
			map.put("startAt", slot.getStartAt());
			map.put("endAt", slot.getEndAt());
			
			for(PartyCrewDTO crew : crewList)
			{
				map.put("userId", crew.getUserId());
				
				if(service.hasReservation(map) > 0)
				{
					reModel.addAttribute("errorMsg", crew.getNickName() +  "님이 해당 시간에 예약이 존재합니다.");
					return "redirect:/err/error";
				}
			}
			
			reModel.addAttribute("party", partyId);
			return "redirect:/reservation";
		} 
		catch (Exception e)
		{
			log.info("doReservation : ",e);
		}
		
		reModel.addAttribute("errorMsg", "서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요");
		return "redirect:/err/error";
	}
	
	@PostMapping("theme/{cafeId}")
	@ResponseBody
	public List<ThemeDTO> getThemeList(@PathVariable(name="cafeId") long cafeId)
	{
		List<ThemeDTO> result = null;
		
		try
		{
			result = service.getThemeList(cafeId);
			
			if(result == null)
			{
				throw new ResponseStatusException(HttpStatus.NOT_FOUND, "테마 목록이 없습니다.");
			}
			
			return result;
		} 
		catch (ResponseStatusException e)
		{
			log.info("getThemeList : ",e );
			throw e;
		}
		catch (Exception e)
		{
			log.info("getThemeList : ",e);
		}
		
		throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류 발생");
	}
	
	@PostMapping("slot/{themeId}")
	@ResponseBody
	public List<ThemeSlotDTO> getSlotList(@PathVariable(name="themeId") long themeId)
	{
		List<ThemeSlotDTO> result = null;
		
		try
		{
			result = service.getSlotList(themeId);
			
			if(result == null)
			{
				throw new ResponseStatusException(HttpStatus.NOT_FOUND,"테마 목록이 없습니다.");
			}
			
			return result;
		} 
		catch (ResponseStatusException e)
		{
			log.info("getSlotList : ",e);
			throw e;
		}
		catch (Exception e)
		{
			log.info("getSlotList : ",e);
		}
		
		throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"서버 오류 발생");
	}
	
	@PostMapping("time/{slotId}")
	@ResponseBody
	public List<ThemeSlotDTO> getTimeList(@PathVariable(name="slotId") long slotId)
	{
		List<ThemeSlotDTO> result = null;
		
		try
		{
			result = service.getTimeList(slotId);
			
			if(result == null)
			{
				throw new ResponseStatusException(HttpStatus.NOT_FOUND,"예약 시간을 불러올 수 없습니다.");
			}
			
			return result;
		}
		catch(ResponseStatusException e)
		{
			log.info("getTimeList : ",e);
			throw e;
		}
		catch (Exception e)
		{
			log.info("getTimeList : ",e);
		}
		
		throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"서버 오류 발생");
	}
	
	/*
	 * 파티 개설 폼으로 이동
	 */
	@GetMapping("write")
	public String partyWrite(@RequestParam(name = "slotId", defaultValue = "0") long slotId, Model model,
			HttpSession session, RedirectAttributes reModel)
	{
		/*
		 * 유효성 검사 목록
		 *
		 * 로그인한 사용자인가?
		 * 존재하는 slotId 인가?
		 * 예약가능한 slodId 인가?
		 * 
		 * mode = "write" 전달
		 * 
		 */

		/*
		 * 가져와야 하는 데이터
		 * 
		 * 슬롯 아이디 카페명 테마아이디 테마명 날짜 시간 최소 인원 최대 인원 가격
		 */

		try
		{
			User user = (User)session.getAttribute("loginUser");
			
			// 로그인 체크
			if(user == null)
			{
				reModel.addAttribute("errorMsg", "로그인 후 이용 가능합니다");
				return "redirect:/err/login";
			}
			
			ThemeSlotDTO dto = service.getThemeSlotById(slotId);

			// 테마 슬롯 유효성 체크
			if (dto == null || dto.getStatus() != 1)
			{
				reModel.addAttribute("errorMsg", "유효하지 않은 슬롯입니다");
				return "redirect:/err/error";
			}

			if(dto.getAdult() > 0)
			{
				if(service.getUserAge(user.getUserId()) < 19)
				{
					reModel.addAttribute("errorMsg", "미성년자는 성인 테마 개설이 불가합니다.");
					return "redirect:/err/error";
				}
			}
			
			model.addAttribute("dto", dto);
			model.addAttribute("mode", "write");
			
			return "party/partywrite";
		} 
		catch (Exception e)
		{
			log.info("partyWrite : ", e);
		}

		reModel.addAttribute("errorMsg", "서버 오류로 인해 파티 생성에 실패했습니다. 잠시후 다시 시도해 주세요.");
		return "redirect:/err/error";
	}

	/*
	 * 파티 insert 액션 처리 PartyDTO 를 파라미터로 받음
	 */
	@PostMapping("write")
	public String partyInsert(PartyDTO dto, HttpSession session, RedirectAttributes reModel)
	{
		// slotId, partyName, genderCondition, partyComment

		/*
		 * 유효성 검사 목록
		 * 
		 * 로그인한 사용자인가?
		 * 존재하는 slotId 인가?
		 * 예약되지 않은 slodId 인가?
		 * 파티명 길이
		 * 파티코멘트 길이
		 */

		/*
		 * 가져올 데이터 없음
		 */

		User user = (User) session.getAttribute("loginUser");

		// 로그인 체크
		if(user == null)
		{
			reModel.addAttribute("errorMsg", "로그인 후 이용 가능합니다");
			return "redirect:/err/login";
		}

		try
		{
			// 파티장으로 설정
			dto.setUserId(user.getUserId());

			service.partyInsert(dto);

			return "redirect:/party/board/" + dto.getPartyId();
		}
		catch (PartyException e)
		{
			reModel.addAttribute("errorMsg", e.getMessage());
			return "redirect:/err/error";
		}
		catch (Exception e)
		{
			log.info("partyInsert : ", e);
			reModel.addAttribute("errorMsg", "서버 오류로 파티 개설에 실패했습니다. 잠시후 다시 시도해 주세요.");
			return "redirect:/err/error";
		}
	}

	/*
	 * 파티 리스트로 이동
	 */
	@GetMapping("list")
	public String partyListPage(@RequestParam(name = "schType", defaultValue = "themeName") String schType,
			@RequestParam(name = "kwd", defaultValue = "") String kwd, Model model
			, SearchFilterDTO filter)
	{
		if (!kwd.isBlank())
		{
			model.addAttribute("schType", schType);
			model.addAttribute("kwd", kwd);
		}

		if(filter.getMinDate() != null)
			model.addAttribute("minDate", filter.getMinDate());
		if(filter.getMaxDate() != null)
			model.addAttribute("maxDate", filter.getMaxDate());
		if(filter.getMinTime() != null)
			model.addAttribute("minTime", filter.getMinTime());
		if(filter.getMaxTime() != null)
			model.addAttribute("maxTime", filter.getMaxTime());
			
		return "party/partylist";
	}

	/*
	 * 파티 리스트 받아오기
	 * 
	 * AJAX 처리
	 */
	@ResponseBody
	@PostMapping("list")
	public List<PartyDTO> partyListData(@RequestParam(name = "schType", defaultValue = "themeName") String schType,
			@RequestParam(name = "kwd", defaultValue = "") String kwd, @RequestParam(name = "lastId") long lastId,
			SearchFilterDTO filter)
	{
		/*
		 * 유효성 검사 목록
		 * 
		 * 없음
		 */

		/*
		 * 가져와야 하는 데이터
		 * 
		 * 테마 번호 테마 이미지 경로 테마명 날짜 시간 파티명 평균 매너 온도 평균 나이 현재 인원 수
		 */
		try
		{
			Map<String, Object> map = new HashMap<>();
			
			map.put("lastId", lastId);
			
			// 키워드가 존재하면 \
			if (!kwd.isBlank())
			{
				map.put("kwd", kwd);
				map.put("schType", schType);
			}

			List<PartyDTO> list = new ArrayList<>();
	
			list = service.getPartyList(map,filter);
			
			if(list == null)
				throw new ResponseStatusException(HttpStatus.NOT_FOUND);
			
			return list;
		} 
		catch (Exception e)
		{
			log.info("partyListData :",e);
		}
		
		throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,"서버 오류 발생");
	}

	/*
	 * 파티 상세 창으로 이동
	 */
	@GetMapping("info/{partyid}")
	public String partyInfo(@PathVariable(name = "partyid") long partyId, Model model
							, HttpSession session, RedirectAttributes reModel)
	{
		/*
		 * 유효성 검사 목록
		 * 
		 * 존재하는 partId 인가?
		 * active 상태인 파티인가?
		 * 
		 * 상태 : anonymous / crew / matching / idle
		 * 로그인 / 비로그인 비로그인이면 신청 버튼 비활성화 파티원 / 비파티원 파티원이면 신청 버튼 비활성화
		 * 신청자 / 비신청자 신청자면 취소 버튼 활성화 비신청자면 신청 버튼 활성화
		 * 구분해서 바인딩
		 */

		/*
		 * 가져와야 하는 데이터
		 * 
		 * 파티번호 파티상태 파티명 카페명 테마명 날짜 시간 최소 인원 최대 인원 성별 조건 파티 코멘트
		 * 파티원 정보 닉네임 나이 성별 매너온도 포지션(파티장/파티원)
		 */

		try
		{
			PartyDTO dto = service.getPartyById(partyId);

			// 파티 검사
			if(dto == null || "close".equals(dto.getPartyStatus()))
			{
				reModel.addAttribute("errorMsg", "유효하지 않은 파티입니다.");
				return "redirect:/err/error";
			}
			
			List<PartyCrewDTO> crewList = service.getPartyCrewList(partyId);
			
			// 파티원 유효성 검사
			if(crewList == null)
			{
				reModel.addAttribute("errorMsg", "파티원 정보를 불러오지 못했습니다.");
				return "redirect:/err/error";
			}
			
			model.addAttribute("dto", dto);
			model.addAttribute("crewList", crewList);

			return "party/partyinfo";
		}
		catch (NullPointerException e)
		{
			log.error("partyInfo : ", e);
		}

		catch (Exception e)
		{
			log.info("partyInfo : ", e);
		}

		reModel.addAttribute("errorMsg", "서버 오류로 파티 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
		return "redirect:/err/error";
	}

	/*
	 * 파티 보드 창으로 이동
	 */
	@GetMapping("board/{partyid}")
	public String partyBoard(@PathVariable(name = "partyid") long partyId, Model model
			  		, HttpSession session, RedirectAttributes reModel)
	{
		/*
		 * 유효성 검사 목록
		 * 
		 * 로그인 했는지? 비로그인이면 로그인창으로
		 * 존재하는 파티인가?
		 * hidden 상태가 아닌 파티인가?
		 * 파티장/파티원 인지? 아니라면 partyList 로
		 * 파티장/파티원 구분해서 바인딩
		 * 파티 보드 활성화/비활성화 구분
		 */

		try
		{
			User user = (User)session.getAttribute("loginUser");
			
			// 로그인 검사
			if(user == null)
			{
				reModel.addAttribute("errorMsg", "로그인 후 이용 가능합니다");
				return "redirect:/err/login";
			}
			
			PartyDTO party = service.getPartyById(partyId);
			
			// 존재하는 파티인지 검사 / 파티 상태가 hidden 인지 검사
			if(party == null || party.getPartyStatus().equals("close"))
			{
				reModel.addAttribute("errorMsg", "유효하지 않은 파티입니다.");
				return "redirect:/err/error";
			}
			
			long userId = user.getUserId();
			
			List<PartyCrewDTO> crew = service.getPartyCrewList(partyId);
			
			Optional<PartyCrewDTO> me = crew.stream()
					                        .filter(c->c.getUserId() == userId)
					                        .findFirst();
			
			// 파티장 / 파티원인지 검사
			if(me.isEmpty())
			{
				reModel.addAttribute("errorMsg", "파티원이 아닙니다.");
				return "redirect:/err/error";
			}
			
			// 파티장 / 파티원 여부 등록
			model.addAttribute("position", me.get().getPosition());
			
			// 파티 상태 등록
			if("close".equals(party.getPartyStatus()))
				model.addAttribute("status", "close");
			else
				model.addAttribute("status", "open");
			
			model.addAttribute("partyId", partyId);
			model.addAttribute("userId", userId);
			
			return "party/partyboard";
		}
		catch (Exception e)
		{
			log.info("partyBoard : ",e);
		}
		
		reModel.addAttribute("errorMsg", "서버 오류로 파티 보드 정보를 불러오지 못했습니다. 잠시 후 다시 시도해 주세요.");
		return "redirect:/err/error";
	}

	/*
	 * 파티 신청 insert 액션 처리
	 */
	@PostMapping("apply/{partyid}")
	public String partyApplyInsert(@PathVariable(name = "partyid") long partyId,
			@RequestParam(name = "applyComment") String applyComment, HttpSession session, RedirectAttributes reModel)
	{
		/*
		 * 유효성 검사 목록
		 * 
		 * 로그인 했는지? 비로그인이라면 로그인창으로
		 * 존재하는 파티인가?
		 * open 상태인 파티인가?
		 * 이미 신청내역이 있는지? 있다면 파티정보창으로
		 * 파티 조건에 부합하는 지?
		 * 코멘트 길이 제한
		 */

		/*
		 * 파티 신청 insert 이후 마이 페이지 파티 내역으로
		 */
		
		try
		{
			User user = (User) session.getAttribute("loginUser");
			
			if(user == null)
			{
				reModel.addAttribute("errorMsg", "로그인 후 이용 가능합니다");
				return "redirect:/err/login";
			}
		
			PartyDTO party = service.getPartyById(partyId);

			// 파티 존재 및 파티 상태 확인
			if (party == null || !"open".equals(party.getPartyStatus()))
			{
				reModel.addAttribute("errorMsg", "유효하지 않은 파티입니다.");
				return "redirect:/err/error";
			}
			
			PartyApplyDTO apply = new PartyApplyDTO();
			
			long userId = user.getUserId();
			
			apply.setUserId(userId);
			apply.setPartyId(partyId);

			// 신청 내역이 있는지 확인
			if (service.hasApply(apply) > 0)
			{
				reModel.addAttribute("errorMsg", "이미 신청한 파티입니다.");
				return "redirect:/err/error";
			}
			
			// 1 이면 동성만 받음
			if(party.getGenderId() == 1)
			{
				Map<String, Object> map = new HashMap<>();
				map.put("applyUserId", userId);
				map.put("partyHostId", party.getUserId());
				
				// 같은 성별이면 1 다른 성별이면 2 
				if(service.isSameGender(map) > 1)
				{
					reModel.addAttribute("errorMsg", "성별 조건에 부합하지 않습니다.");
					return "redirect:/err/error";
				}
			}
			
			ThemeSlotDTO slot = service.getThemeSlotById(party.getSlotId());
			
			// 1 성인 / 0 전체
			if(slot.getAdult() > 0)
			{
				if(service.getUserAge(userId) < 19)
				{
					reModel.addAttribute("errorMsg", "미성년자는 성인 테마 신청이 불가 합니다.");
					return "redirect:/err/error";
				}
			}
			
			if(applyComment.length() >= 30)
			{
				reModel.addAttribute("errorMsg", "신청 메시지 길이 제한을 초과했습니다.");
				return "redirect:/err/error";
			}
			
			apply.setApplyComment(applyComment);
			
			if(service.partyApply(apply) > 0)
				return "redirect:/mypage/myparty";
		} 
		catch (Exception e)
		{
			log.info("partyApplyInsert : ", e);
		}

		reModel.addAttribute("errorMsg", "서버 오류로 파티 신청에 실패했습니다. 잠시 후 다시 시도해 주세요.");
		return "redirect:/err/error";
	}
	
	@GetMapping("apply/cancel/{applyId}")
	public String partyApplyDelete(@PathVariable(name="applyId") long applyId
			, HttpSession session, RedirectAttributes reModel)
	{
		try
		{
			User user = (User)session.getAttribute("loginUser");
			// 로그인 검사
			if(user == null)
			{
				reModel.addAttribute("errorMsg", "로그인 후 이용 가능합니다");
				return "redirect:/err/login";
			}
			
			PartyApplyDTO apply = service.getPartyApplyById(applyId);
			// 파티 신청 유효성 검사
			if(apply == null)
			{
				reModel.addAttribute("errorMsg","존재하지 않는 신청입니다.");
				return "redirect:/err/error";
			}
			// 작성자와 취소자 일치 검사
			if(user.getUserId() != apply.getUserId())
			{
				reModel.addAttribute("errorMsg","신청자 본인이 아닙니다.");
				return "redirect:/err/error";
			}
			// delete 액션
			if(service.rejectApply(applyId) > 0)
				return "redirect:/mypage/myparty";
		} 
		catch (Exception e)
		{
			log.info("partyApplyDelete : ", e);
		}
		
		reModel.addAttribute("errorMsg","서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.");
		return "redirect:/err/error";
	
	}
	
	/*
	 * 파티 정보 수정 폼 이동
	 */
	@GetMapping("update/{partyid}")
	public String partyUpdate(@PathVariable(name = "partyid") long partyId
			, Model model, HttpSession session, RedirectAttributes reModel)
	{
		/*
		 * 유효성 검사 목록
		 * 
		 * 로그인 했는가?
		 * 존재하는 파티인가?
		 * active/close 상태인 파티인가?
		 * 파티장인가?
		 * mode="update" 전달
		 * 파티 정보 바인딩해서 전달
		 */

		/*
		 * 받아와야 하는 데이터
		 * 
		 * 카페명 테마명 날짜 시간 최소 인원 수 최대 인원 수 가격 파티명 성별 조건 파티코멘트
		 */
		
		try
		{
			User user = (User)session.getAttribute("loginUser");
			// 로그인 체크
			if(user == null)
			{
				reModel.addAttribute("errorMsg", "로그인 후 이용 가능합니다");
				return "redirect:/err/login";
			}
			
			PartyDTO party = service.getPartyById(partyId);
			// 파티 존재 및 상태 검사
			if(party == null || "close".equals(party.getPartyStatus()) || "confirm".equals(party.getPartyStatus()))
			{
				reModel.addAttribute("errorMsg", "유효하지 않은 파티입니다.");
				return "redirect:/err/error";
			}
			
			if(user.getUserId() != party.getUserId())
			{
				reModel.addAttribute("errorMsg", "파티장만 수정할 수 있습니다");
				return "redirect:/err/error";
			}
			
			List<Cafe> cafeList = service.getCafeList();
			
			//log.info("party : slodId : " +  party.getSlotId());
			
			ThemeSlotDTO dto = service.getThemeSlotById(party.getSlotId());
			
			if(dto != null)
			{
				model.addAttribute("mode", "update");
				model.addAttribute("party", party);
				model.addAttribute("dto",dto);
				model.addAttribute("cafeList", cafeList);
				
				return "party/partyupdate";
			}
		} 
		catch (Exception e)
		{
			log.info("partyUpdate : ",e);
		}
		
		reModel.addAttribute("errorMsg", "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
		return "redirect:/err/error";
	}

	@PostMapping("update/{partyid}")
	public String partyUpdate(@PathVariable(name="partyid") long partyId
			,@RequestParam(name="partyComment") String partyComment
			,@RequestParam(name="partyName") String partyName
			,@RequestParam(name="genderId", defaultValue = "0") int genderId
			,@RequestParam(name="slotId") long slotId
			, RedirectAttributes reModel, HttpSession session)
	{
		try
		{
			/*
			 * 유효성 검사 목록
			 * 
			 * 로그인한 사용자인가?
			 * 파티장인가?
			 * 존재하는 slotId 인가?
			 * 예약되지 않은 slodId 인가?
			 * 파티명 길이
			 * 파티코멘트 길이
			 */
			
			User user = (User) session.getAttribute("loginUser");
			
			// 로그인 체크
			if(user == null)
			{
				reModel.addAttribute("errorMsg", "로그인 후 이용 가능합니다");
				return "redirect:/err/login";
			}
			
			PartyDTO party = service.getPartyById(partyId);
			
			if(party == null || "close".equals(party.getPartyStatus()) || "confirm".equals(party.getPartyStatus()))
			{
				reModel.addAttribute("errorMsg", "유효하지 않은 파티입니다.");
				return "redirect:/err/error";
			}
			
			if(user.getUserId() != party.getUserId())
			{
				reModel.addAttribute("errorMsg", "파티장만 수정 가능합니다.");
				return "redirect:/err/error";
			}
			
			ThemeSlotDTO slot = service.getThemeSlotById(party.getSlotId());
			
			// 슬롯 유효성 체크
			if (slot == null || slot.getStatus() != 1)
			{
				reModel.addAttribute("errorMsg", "유효하지 않은 슬롯입니다.");
				return "redirect:/err/error";
			}

			// 길이 체크
			if (partyComment.length() >= 30 || partyName.length() >= 20)
			{
				reModel.addAttribute("errorMsg", "파티 제약 조건을 위반했습니다.");
				return "redirect:/err/error";
			}
			
			party.setPartyName(partyName);
			party.setPartyComment(partyComment);
			party.setGenderId(genderId);
			
			if(service.partyUpdate(party) > 0)
			{
				
				if(party.getSlotId() != slotId)
				{
					Map<String, Object> updateMap = new HashMap<>();
					updateMap.put("partyId", party.getPartyId());
					updateMap.put("slotId", slotId);
					
					if(service.partyRoomUpdate(updateMap) > 0)
					{
						return "redirect:/party/board/" + party.getPartyId();
					}
				}
				else
				{	
					return "redirect:/party/board/" + party.getPartyId();
				}
			}
		} 
		catch (Exception e)
		{
			log.info("partyUpdate : ",e);
		}
		
		reModel.addAttribute("errorMsg", "서버 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.");
		return "redirect:/err/error";
	}
	
	
	/*
	 * 파티 해산 메소드 해산 이후 파티 리스트로 이동
	 */
	@GetMapping("delete/{partyid}")
	public String partyDelete(@PathVariable(name = "partyid") long partyId
			, HttpSession session, RedirectAttributes reModel)
	{
		/*
		 * 유효성 검사 목록
		 * 
		 * 로그인 했는가?
		 * 존재하는 파티 인가?
		 * active/close 상태인 파티 인가?
		 * 파티장인가?
		 * 파티 해산 이후 리스트로 리다이렉트
		 */
		
		try
		{
			User user = (User)session.getAttribute("loginUser");
			// 로그인 검사
			if(user == null) 
			{
				reModel.addAttribute("errorMsg", "로그인 후 이용 가능합니다");
				return "redirect:/err/login";
			}
			
			PartyDTO party = service.getPartyById(partyId);
			// 파티 존재 / 상태 검사
			if(party == null || "confirm".equals(party.getPartyStatus()) || "close".equals(party.getPartyStatus()))
			{
				reModel.addAttribute("errorMsg","유효하지 않은 파티입니다.");
				return "redirect:/err/error";
			}
			
			// 파티장 검사
			if(user.getUserId() != party.getUserId())
			{
				reModel.addAttribute("errorMsg","파티 해산은 파티장만 할 수 있습니다.");
				return "redirect:/err/error";
			}
			
			if(service.partyDelete(partyId) > 0)
				return "redirect:/mypage/myparty";
		
		} 
		catch (Exception e)
		{
			log.info("partyDelete : ",e);
		}

		reModel.addAttribute("errorMsg","서버 오류로 파티 해산에 실패했습니다. 잠시 후 다시 시도해주세요.");
		return "redirect:/err/error";
	}

	/*
	 * 댓글 insert 액션 처리 AJAX 처리
	 */
	@ResponseBody
	@PostMapping("comment/insert")
	public Map<String, Object> commentInsert(@RequestParam(name = "partyId") long partyId,
			@RequestParam(name = "partyComment") String partyComment
			, HttpSession session)
	{
		/*
		 * 유효성 검사 목록
		 * 
		 * 로그인 했는가?
		 * 존재하는 파티 인가?
		 * close 상태가 아닌 파티인가?
		 * 파티장/파티원 인가?
		 */
		
		try
		{
			Map<String, Object> result = new HashMap<>();
			result.put("status", false);
			
			User user = (User)session.getAttribute("loginUser");
			
			// 로그인 검사
			if(user == null){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);}
	
			long userId = user.getUserId();
			
			PartyDTO party = service.getPartyById(partyId);
			
			// 파티 존재 / 파티 상태 검사
			if(party == null || "close".equals(party.getPartyStatus())){throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
			
			Optional<PartyCrewDTO> crew = service.getPartyCrewList(partyId)
												 .stream()
												 .filter(c->c.getUserId() == userId)
												 .findFirst();
			
			// 파티장 / 파티원 검사
			if(crew.isEmpty()){throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
			
			PartyCommentDTO comment = new PartyCommentDTO();
			comment.setPartyId(partyId);
			comment.setUserId(userId);
			comment.setPartyComment(partyComment);
			
			// 댓글 insert 액션
			if(service.partyCommentInsert(comment) > 0)
			{
				result.put("status", true);
				return result;
			}
		}
		catch (ResponseStatusException e)
		{
			log.info("commentInsert : ",e);
			throw e;
		}
		catch (Exception e)
		{
			log.info("commentInsert : ",e);
		}

		throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/*
	 * 댓글 삭제 메소드 AJAX 처리
	 */
	@ResponseBody
	@PostMapping("comment/delete/{commentid}")
	public Map<String, Object> commentDelete(@PathVariable(name = "commentid") long commentId, HttpSession session)
	{
		/*
		 * 유효성 검사 목록
		 * 
		 * 로그인 했는가?
		 * 댓글이 존재하는가?
		 * 댓글이 삭제되지 않았는가?
		 * 댓글 작성자인가?
		 */

		/*
		 * 받아올 데이터 없음
		 */
		try
		{
			Map<String, Object> result = new HashMap<>();
			
			result.put("status", false);
			
			User user = (User)session.getAttribute("loginUser");
			
			// 로그인 확인
			if(user == null){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);}
		
			PartyCommentDTO comment = service.getCommentById(commentId);
			
			// 댓글 존재 및 삭제 여부 확인
			if(comment == null){throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
			
			// 작성자 확인
			if(user.getUserId() != comment.getUserId()){throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
				
			if(service.partyCommentDelete(commentId) > 0)
			{
				result.put("status", true);
				return result;
			}
		} 
		catch (ResponseStatusException e)
		{
			log.info("commentDelete : ",e);
			throw e;
		}
		catch (Exception e)
		{
			log.info("commentDelete : ",e);
		}
		
		throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/*
	 * 레디 액션 처리 AJAX 처리
	 */
	@ResponseBody
	@PostMapping("ready/{partyId}")
	public Map<String, Object> setReady(@PathVariable(name="partyId") long partyId
			,HttpSession session, RedirectAttributes reModel)
	{
		/*
		 * 유효성 검사 목록
		 * 
		 * 로그인 했는가?
		 * 존재하는 파티 인가?
		 * open/full 상태인 파티 인가?
		 * 파티원 인가?
		 */

		try
		{
			User user = (User)session.getAttribute("loginUser");
			// 로그인 검사
			if(user == null) {throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);}
			
			//System.out.println("userId : " + user.getUserId() + " / partyId : " + partyId );
			
			PartyDTO party = service.getPartyById(partyId);
			// 파티 검사
			if(party == null || "close".equals(party.getPartyStatus()) || "confirm".equals(party.getPartyStatus())) {throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
			
			List<PartyCrewDTO> crewList = service.getPartyCrewList(partyId);
			// 파티원 검사
			if(crewList.stream().filter(c->"CREW".equals(c.getPosition())).noneMatch(c->c.getUserId() == user.getUserId()))
			{
				throw new ResponseStatusException(HttpStatus.FORBIDDEN);
			}
			
			Map<String, Long> map = new HashMap<>();
			map.put("userId", user.getUserId());
			map.put("partyId", partyId);
			
			Map<String, Object> result = new HashMap<>();
			
			if(service.partyReady(map) > 0)	
			{
				result.put("status", true);
				return result;
			}
		}
		catch (ResponseStatusException e)
		{
			log.info("setReady : ",e);
			throw e;
		}
		catch (Exception e)
		{
			log.info("setReady : ",e);
		}
		
		throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/*
	 * 파티 강퇴 액션 처리 AJAX 처리
	 */
	@ResponseBody
	@PostMapping("kick/{crewid}")
	public Map<String, Object> crewKick(@PathVariable(name = "crewid") long crewId, HttpSession session)
	{
		/*
		 * 유효성 검사 목록
		 * 
		 * 로그인 했는가?
		 * 존재하는 멤버 id 인가?
		 * open/full 상태인 파티인가?
		 * 현재 파티원인가?
		 * 파티장인가?
		 */

		/*
		 * 받아올 데이터 없음
		 */
		try
		{
			Map<String, Object> map = new HashMap<>();
			map.put("status", false);
			
			User user = (User)session.getAttribute("loginUser");
			
			// 로그인 확인
			if(user == null){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);}
			
			PartyCrewDTO crew = service.getPartyCrewById(crewId);
			// 파티원 여부 검사
			if(crew == null){throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
			
			PartyDTO party = service.getPartyById(crew.getPartyId());
			// 파티 존재 / 파티 상태 검사
			if(party == null || "close".equals(party.getPartyStatus()) || "confirm".equals(party.getPartyStatus())){throw new ResponseStatusException(HttpStatus.NOT_FOUND);}

			//log.info("userId : " + user.getUserId() + " / partyUserId : " + party.getUserId());
			
			// 파티장 검사
			if(user.getUserId() != party.getUserId()){throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
			
			// 파티 강퇴 실행
			if(service.partyKick(crewId) > 0)
			{
				map.put("status", true);
				return map;
			}
		}
		catch(ResponseStatusException e)
		{
			log.info("crewKick : ",e);
			throw e;
		}
		catch (Exception e)
		{
			log.info("crewKick : ",e);
		}
		
		throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/*
	 * 파티 탈퇴 액션 처리
	 * 
	 * 파티원 delete 파티신청 delete
	 * 
	 * AJAX 처리
	 */
	@ResponseBody
	@PostMapping("out/{applyid}")
	public Map<String,Object> crewOut(@PathVariable(name = "applyid") long applyId, HttpSession session)
	{
		/*
		 * 유효성 검사 목록
		 * 
		 * 로그인 했는가?
		 * 존재하는 신청 id 인가?
		 * active/close 상태인 파티인가?
		 * 현재 파티원인가?
		 * 자기자신인가?
		 */

		try
		{
			Map<String, Object> map = new HashMap<>();
			
			map.put("status", false);
			
			User user = (User)session.getAttribute("loginUser");
			// 로그인 검사
			if(user == null) {throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);}
			
			//log.info("userId : " + user.getUserId());
			
			PartyApplyDTO apply = service.getPartyApplyById(applyId);
			// 신청 존재 여부 검사
			if(apply == null) {throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
			
			//log.info("applyId : " + apply.getApplyId());
			
			PartyDTO party = service.getPartyById(apply.getPartyId());
			// 파티 존재 및 상태 검사
			if(party == null || "confirm".equals(party.getPartyStatus()) || "close".equals(party.getPartyStatus())){throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
			
			//log.info("partyId : " + party.getPartyId());
			
			List<PartyCrewDTO> crewList = service.getPartyCrewList(apply.getPartyId());
			// 현재 파티원 여부 검사
			if(crewList.stream().filter(c->"CREW".equals(c.getPosition())).noneMatch(c->c.getUserId() == user.getUserId())) {throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
			
			// 신청자 본인인지 확인
			if(user.getUserId() != apply.getUserId()) {throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
			
			// 파티 탈퇴 처리
			service.partyOut(applyId);
			map.put("status", true);
			return map;
		}
		catch (Exception e)
		{
			log.info("crewOut : ",e);
		}
		
		throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/*
	 * 파티 신청 승인 액션 처리
	 * 
	 * 파티원 insert
	 * 
	 * AJAX 처리
	 */
	@ResponseBody
	@PostMapping("aprvapply/{applyid}")
	public Map<String, Object> aprvApply(@PathVariable(name = "applyid") long applyId, HttpSession session)
	{
		/*
		 * 유효성 검사 목록
		 * 
		 * 로그인 했는가?
		 * 존재하는 신청 id 인가?
		 * open 상태인 파티인가?
		 * 현재 파티원이 아닌가?
		 * 파티장인가?
		 */
		
		try
		{
			Map<String, Object> map = new HashMap<>();
			map.put("status", false);
			
			User user = (User)session.getAttribute("loginUser");
			
			// 로그인 확인
			if(user == null){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);}
			
			PartyApplyDTO apply = service.getPartyApplyById(applyId);
			
			// 존재하는 신청 id 확인
			if(apply == null ){throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
			
			PartyDTO party = service.getPartyById(apply.getPartyId());
			
			// 존재하는 파티 / 파티 상태 확인
			if(party == null || !"open".equals(party.getPartyStatus())){throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
			
			List<PartyCrewDTO> crewList = service.getPartyCrewList(apply.getPartyId());
			
			// 현재 파티장 / 파티원 여부 확인
			if(crewList.stream().anyMatch(c->c.getUserId() == apply.getUserId())){throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
			
			Optional<PartyCrewDTO> crew = crewList.stream().filter(c->"HOST".equals(c.getPosition())).findFirst(); 
			
			// 파티장 존재 확인 (이론상 발생 안함)
			if(crew.isEmpty()){throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
			
			// 내가 파티장 인지 확인
			if(crew.get().getUserId() != user.getUserId()){throw new ResponseStatusException(HttpStatus.FORBIDDEN,"파티장만 승인 가능합니다.");}
			
			// 1 이면 동성만 받음
			if(party.getGenderId() == 1)
			{
				Map<String, Object> emp = new HashMap<>();
				emp.put("applyUserId", apply.getUserId());
				emp.put("partyHostId", user.getUserId());
				
				// 같은 성별이면 1 다른 성별이면 2 
				if(service.isSameGender(emp) > 1)
				{
					// 바꿔야함
					throw new ResponseStatusException(HttpStatus.FORBIDDEN,"성별 조건을 만족하지 않는 신청입니다.");
				}
			}
			
			ThemeSlotDTO slot = service.getThemeSlotById(party.getSlotId());
			
			// 1 성인 / 0 전체
			if(slot.getAdult() > 0)
			{
				if(service.getUserAge(apply.getUserId()) < 19)
				{
					throw new ResponseStatusException(HttpStatus.FORBIDDEN,"미성년자는 성인 테마 승인이 불가합니다.");
				}
			}
			
			// 파티 승인 액션
			if(service.aprvApply(applyId) > 0)
			{
				map.put("status", true);
				return map;
			}
		}
		catch(ResponseStatusException e)
		{
			log.info("aprvApply : ",e);
			throw e;
		}
		catch (Exception e)
		{
			log.info("aprvApply : ",e);
		}
		
		throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/*
	 * 파티 신청 거절 액션 처리
	 * 
	 * apply delete
	 * 
	 * AJAX 처리
	 */
	@ResponseBody
	@PostMapping("rejectapply/{applyid}")
	public Map<String, Object> rejectApply(@PathVariable(name = "applyid") long applyId, HttpSession session)
	{
		/*
		 * 유효성 검사 목록
		 * 
		 * 로그인 했는가?
		 * 존재하는 신청 id 인가?
		 * 현재 파티원이 아닌가?
		 * 파티장인가?
		 */

		try
		{
			Map<String, Object> map = new HashMap<>();
			map.put("status", false);
			
			User user = (User)session.getAttribute("loginUser");
			// 로그인 확인
			if(user == null){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);}
			
			PartyApplyDTO apply = service.getPartyApplyById(applyId);
			// 존재하는 신청 id 확인
			if(apply == null ){throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
			
			PartyDTO party = service.getPartyById(apply.getPartyId());
			// 존재하는 파티 확인
			if(party == null){throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
			
			List<PartyCrewDTO> crewList = service.getPartyCrewList(apply.getPartyId());
			// 현재 파티장 / 파티원 여부 확인
			if(crewList.stream().anyMatch(c->c.getUserId() == apply.getUserId())){throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
			
			Optional<PartyCrewDTO> crew = crewList.stream().filter(c->"HOST".equals(c.getPosition())).findFirst();
			// 파티장 존재 확인
			if(crew.isEmpty()){throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
			
			// 내가 파티장 인지 확인
			if(crew.get().getUserId() != user.getUserId()){throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
			
			// 거절 액션
			if(service.rejectApply(applyId) > 0)
			{
				map.put("status", true);
				return map;
			}
		}
		catch(ResponseStatusException e)
		{
			log.info("rejectApply : ",e);
			throw e;
		}
		catch (Exception e)
		{
			log.info("rejectApply : ",e);
		}
		
		throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
	}

	/*
	 * 파티 정보를 바인딩해서 전달하는 메소드 ㅈㄴ 바쁜 메소드
	 * 
	 * AJAX 처리
	 */
	@ResponseBody
	@PostMapping("data/{partyid}")
	public Map<String, Object> partyData(@PathVariable(name = "partyid") long partyId
										,@RequestParam(name="lastCommentId", defaultValue = "0") long lastCommentId
										,@RequestParam(name="lastDeleteCommentId", defaultValue = "0") long lastDeleteCommentId
										,HttpSession session)
	{
		/*
		 * 유효성 검사 목록
		 * 
		 * 로그인 했는가?
		 * 존재하는 파티인가?
		 * 파티 상태가 hidden 이 아닌가?
		 * 파티장/파티원 인가?
		 */

		/*
		 * 받아올 데이터
		 * 
		 * 파티 정보
		 * 슬롯 상태 : 가능 / 불가능
		 * 파티 상태 파티명 카페명 테마명 날짜 시간 최소인원 최대인원 성별조건 파티코멘트
		 * 
		 * 파티원 정보
		 * 닉네임 나이 성별 매너온도 레디상태 포지션 상태
		 * 파티 댓글 정보
		 * 댓글 번호 작성자 작성 내용 삭제 유무
		 * 
		 * 파티 신청
		 * 닉네임 나이 성별 매너온도 신청 코멘트
		 */
		
		try
		{
			User user = (User)session.getAttribute("loginUser");
			
			// 로그인 검사
			if(user == null){throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);}
			
			long userId = user.getUserId();
			
			// 파티 정보 조회
			PartyDTO party = service.getPartyById(partyId);
			
			// 파티 존재 검사 / 파티 상태 검사
			if(party == null || "hidden".equals(party.getPartyStatus())){throw new ResponseStatusException(HttpStatus.NOT_FOUND);}
			
			// 파티원 조회
			List<PartyCrewDTO> crewList = service.getPartyCrewList(partyId);
			
			Optional<PartyCrewDTO> crew = crewList.stream().filter(c->c.getUserId() == userId).findFirst();
			
			// 파티장 / 파티원 검사
			if(crew.isEmpty()){throw new ResponseStatusException(HttpStatus.FORBIDDEN);}
			
			// 파티 신청 조회
			List<PartyApplyDTO> applyList = service.getPartyApplyList(partyId);
			
			Map<String, Object> commentMap = new HashMap<>();
			commentMap.put("partyId", partyId);
			commentMap.put("lastCommentId", lastCommentId);
			commentMap.put("lastDeleteCommentId", lastDeleteCommentId);
			
			// 파티 댓글 조회
			List<PartyCommentDTO> commentList = service.getPartyCommentList(commentMap);
			
			// 삭제 댓글 조회
			List<PartyCommentDeleteDTO> commentDeleteList = service.getCommentDeleteList(commentMap);
			
			// 데이터 바인딩
			Map<String, Object> map = new HashMap<>();
			
			map.put("partyInfo", party);
			map.put("crewList", crewList);
			map.put("applyList", applyList);
			map.put("commentList", commentList);
			map.put("commentDeleteList", commentDeleteList);
			
			return map;
		}
		catch (ResponseStatusException e)
		{
			log.info("partyData : ",e);
			throw e;
		}
		catch (Exception e)
		{
			log.info("partyData : ",e);
		}
		
		throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR);
	}
}
