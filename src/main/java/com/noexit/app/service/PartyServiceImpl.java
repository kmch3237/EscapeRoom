package com.noexit.app.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.stereotype.Service;

import com.noexit.app.common.PartyException;
import com.noexit.app.mapper.PartyMapper;
import com.noexit.app.model.Cafe;
import com.noexit.app.model.MyPartyDTO;
import com.noexit.app.model.PartyApplyDTO;
import com.noexit.app.model.PartyCommentDTO;
import com.noexit.app.model.PartyCommentDeleteDTO;
import com.noexit.app.model.PartyCrewDTO;
import com.noexit.app.model.PartyDTO;
import com.noexit.app.model.SearchFilterDTO;
import com.noexit.app.model.ThemeDTO;
import com.noexit.app.model.ThemeSlotDTO;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartyServiceImpl implements PartyService
{
	private final PartyMapper mapper;

	private static final int ADULT_MIN_AGE = 19;
	private static final int PARTY_NAME_MAX = 20;
	private static final int PARTY_COMMENT_MAX = 30;

	@Override
	public List<PartyDTO> getPartyList(Map<String, Object> map,SearchFilterDTO filter)
	{
		List<PartyDTO> list = null;
		
		try
		{
			if(filter.getMinDate() != null)
				map.put("minDate", filter.getMinDate());
			if(filter.getMaxDate() != null)
				map.put("maxDate", filter.getMaxDate());
			if(filter.getMinTime() != null)
				map.put("minTime", filter.getMinTime());
			if(filter.getMaxTime() != null)
				map.put("maxTime", filter.getMaxTime());
			
			list = mapper.getPartyList(map);
		} 
		catch (Exception e)
		{
			log.info("getPartyList : ",e);
		}
		
		return list;
	}

	@Override
	public PartyDTO getPartyById(long partyId)
	{
		PartyDTO dto = null;
		
		try
		{
			dto = mapper.getPartyById(partyId);
		} 
		catch (Exception e)
		{
			log.info("getPartyById : ",e);
		}

		return dto;
	}

	@Override
	public int hasApply(PartyApplyDTO dto)
	{
		int count = 0;
		
		try
		{
			count = mapper.hasApply(dto);
		}
		catch (Exception e)
		{
			log.info("hasApply : ",e);
		}
		
		return count;
	}
	
	@Override
	public int partyApply(PartyApplyDTO dto)
	{
		int result = 0;
		
		try
		{
			result = mapper.partyApply(dto);
		} 
		catch (Exception e)
		{
			log.info("partyApply : ",e);
		}
		
		return result;
	}

	@Override
	public ThemeSlotDTO getThemeSlotById(long themeId)
	{
		ThemeSlotDTO dto = null;
		
		try
		{
			dto = mapper.getThemeSlotById(themeId);
		} 
		catch (Exception e)
		{
			log.info("getThemeById :",e);
		}
		
		return dto;
	}

	@Override
	public int partyInsert(PartyDTO dto)
	{
		// 슬롯 유효성
		ThemeSlotDTO slot = mapper.getThemeSlotById(dto.getSlotId());
		if (slot == null || slot.getStatus() != 1)
			throw new PartyException("유효하지 않은 슬롯입니다.");

		// 성인 테마 나이 제한
		if (slot.getAdult() > 0 && getUserAge(dto.getUserId()) < ADULT_MIN_AGE)
			throw new PartyException("미성년자는 성인 테마 개설이 불가합니다.");

		// 길이 제한
		if (dto.getPartyComment().length() >= PARTY_COMMENT_MAX
				|| dto.getPartyName().length() >= PARTY_NAME_MAX)
			throw new PartyException("파티 제약 조건을 위반했습니다.");

		return mapper.partyInsert(dto);
	}

	@Override
	public int partyUpdate(PartyDTO dto)
	{
		int result = 0;
		
		try
		{
			result = mapper.partyUpdate(dto);
		} 
		catch (Exception e)
		{
			log.info("partyUpdate : ",e);
		}
		
		return result;
	}

	@Override
	public int partyDelete(long partyId)
	{
		int result = 0;
		
		try
		{
			result = mapper.partyDelete(partyId);
		} 
		catch (Exception e)
		{
			log.info("partyDelete : ",e);
		}
		
		return result;
	}

	@Override
	public List<PartyCommentDTO> getPartyCommentList(Map<String, Object> map)
	{
		List<PartyCommentDTO> list = null;
		
		try
		{
			list = mapper.getPartyCommentList(map);
		} 
		catch (Exception e)
		{
			log.info("getPartyCommentList : ",e);
		}
		
		return list;
	}
	
	@Override
	public List<PartyCommentDeleteDTO> getCommentDeleteList(Map<String, Object> map)
	{
		List<PartyCommentDeleteDTO> list = null;
		
		try
		{
			list = mapper.getCommentDeleteList(map);
		} 
		catch (Exception e)
		{
			log.info("getCommentDeleteList : ",e);
		}
		
		return list;
	}
	
	@Override
	public int partyCommentInsert(PartyCommentDTO dto)
	{
		int result = 0;
		
		try
		{
			result = mapper.partyCommentInsert(dto);
		}
		catch (Exception e)
		{
			log.info("partyCommentInsert : ",e);
		}
		
		return result;
	}

	@Override
	public PartyCommentDTO getCommentById(long commentId)
	{
		PartyCommentDTO result = null;
		
		try
		{
			result = mapper.getCommentById(commentId);
		} 
		catch (Exception e)
		{
			log.info("hasComment : ",e);
		}
		
		return result;
	}
	
	@Override
	public int partyCommentDelete(long commentId)
	{
		int result = 0;
		
		try
		{
			result = mapper.partyCommentDelete(commentId);
		}
		catch (Exception e)
		{
			log.info("partyCommentDelete : ",e);
		}
		
		return result;
	}

	@Override
	public List<PartyCrewDTO> getPartyCrewList(long partyId)
	{
		List<PartyCrewDTO> list = null;
		
		try
		{
			list = mapper.getPartyCrewList(partyId);
		} 
		catch (Exception e)
		{
			log.info("getPartyCrewList : ",e);
		}
		
		return list;
	}

	@Override
	public int partyReady(Map<String, Long> map)
	{
		int result = 0;
		
		try
		{
			long partyId = map.get("partyId");
			long userId = map.get("userId");
			
			log.error("partyId : " + partyId + " / userId : " + userId);
			
			List<PartyCrewDTO> crewList = mapper.getPartyCrewList(partyId);
			
			Optional<PartyCrewDTO> crewOp = crewList.stream().filter(c->c.getUserId() == userId).findFirst();
			
			if(!crewOp.isEmpty())
			{
				result = mapper.partyReady(crewOp.get().getApplyId());
			}
			
			//result = mapper.partyReady(applyId);
		} 
		catch (Exception e)
		{
			log.info("partyReady : ",e);
		}
		
		return result;
	}

	@Override
	public int partyOut(long applyId)
	{
		int result = 0;
		
		try
		{
			result = mapper.partyOut(applyId);
		} 
		catch (Exception e)
		{
			log.info("partyOut : ",e);
		}
		
		return result;
	}

	@Override
	public int partyKick(long crewId)
	{
		int result = 0;
		
		try
		{
			result = mapper.partyKick(crewId);
		} 
		catch (Exception e)
		{
			log.info("partyKick : ",e);
		}
		
		return result;
	}

	@Override
	public PartyCrewDTO getPartyCrewById(long crewId)
	{
		PartyCrewDTO result = null;
		
		try
		{
			result = mapper.getPartyCrewById(crewId);
		}
		catch (Exception e)
		{
			log.info("getPartyCrewById : ",e);
		}
		
		return result;
	}
	
	@Override
	public List<PartyApplyDTO> getPartyApplyList(long partyId)
	{
		List<PartyApplyDTO> list = null;
		
		try
		{
			list = mapper.getPartyApplyList(partyId);
		}
		catch (Exception e)
		{
			log.info("getPartyApplyList : ",e);
		}
		
		return list;
	}

	@Override
	public PartyApplyDTO getPartyApplyById(long applyId)
	{
		PartyApplyDTO result = null;
		
		try
		{
			result = mapper.getPartyApplyById(applyId);
		}
		catch (Exception e)
		{
			log.info("getPartyApplyById : ",e);
		}
		
		return result;
	}
	
	@Override
	public int aprvApply(long applyId)
	{
		int result = 0;
		
		try
		{
			result = mapper.aprvApply(applyId);
		} 
		catch (Exception e)
		{
			log.info("aprvApply : ",e);
		}
		
		return result;
	}

	@Override
	public int rejectApply(long applyId)
	{
		int result = 0;
		
		try
		{
			result = mapper.rejectApply(applyId);
		} 
		catch (Exception e)
		{
			log.info("rejectApply : ",e);
		}
		
		return result;
	}
	
	@Override
	public int isSameGender(Map<String, Object> map)
	{
		int result = 0;
		
		try
		{
			result = mapper.isSameGender(map);
		} 
		catch (Exception e)
		{
			log.info("isSameGender : ",e);
		}
		
		return result;
	}
	
	@Override
	public Integer getUserAge(long userId)
	{
		Integer result = null;
		
		try
		{
			result = mapper.getUserAge(userId);
		}
		catch (Exception e)
		{
			log.info("getUserId : ",e);
		}
		
		return result;
	}
	
	@Override
	public List<MyPartyDTO> getMyPartyApplyList(long userId)
	{
		List<MyPartyDTO> result = null;
		
		try
		{
			result = mapper.getMyPartyApplyList(userId);
		}
		catch (Exception e)
		{
			log.info("getMyPartyApplyList : ",e);
		}
		
		return result;
	}
	
	@Override
	public List<MyPartyDTO> getMyPartyList(long userId)
	{
		List<MyPartyDTO> result = null;

		try
		{
			result = mapper.getMyPartyList(userId);
		}
		catch (Exception e)
		{
			log.info("getMyPartyList : ",e);
		}
	
		
		return result;
	}
	
	@Override
	public List<MyPartyDTO> getMyPartyKickList(long userId)
	{
		List<MyPartyDTO> result = null;
		
		try
		{
			result = mapper.getMyPartyKickList(userId);
		}
		catch (Exception e)
		{
			log.info("getMyPartyKickList : ",e);
		}
	
		
		return result;
	}
	
	@Override
	public List<Cafe> getCafeList()
	{
		List<Cafe> result = null;
		
		try
		{
			result = mapper.getCafeList();
		} 
		catch (Exception e)
		{
			log.info("getCafeList : ",e);
		}
		
		return result;
	}
	
	@Override
	public List<ThemeDTO> getThemeList(long cafeId)
	{
		List<ThemeDTO> result = null;
		
		try
		{
			result = mapper.getThemeList(cafeId);
		} 
		catch (Exception e)
		{
			log.info("getThemeList : ",e);
		}
		
		return result;
	}
	
	@Override
	public List<ThemeSlotDTO> getSlotList(long themeId)
	{
		List<ThemeSlotDTO> result = null;
		
		try
		{
			result = mapper.getSlotList(themeId);
		}
		catch (Exception e)
		{
			log.info("getSlotList : ",e);
		}
		
		return result;
	}
	
	@Override
	public List<ThemeSlotDTO> getTimeList(long slotId)
	{
		List<ThemeSlotDTO> result = null;
		
		try
		{
			result = mapper.getTimeList(slotId);
		} 
		catch (Exception e)
		{
			log.info("getTimeList : ",e);
		}
		
		return result;
	}
	
	@Override
	public int partyRoomUpdate(Map<String, Object> map)
	{
		int result = 0;
		
		try
		{
			result = mapper.partyRoomUpdate(map);
		} 
		catch (Exception e)
		{
			log.info("partyRoomUpdate : ",e);
		}
		
		return result;
	}
	
	@Override
	public int hasReservation(Map<String, Object> map)
	{
		int result = 0;
		
		try
		{
			result = mapper.hasReservation(map);
		} 
		catch (Exception e)
		{
			log.info("hasReservation : ",e);
		}
		
		return result;
	}
}
