package com.noexit.app.common;

import org.springframework.stereotype.Service;

/*
 * PagingUtil — Thymeleaf 전용 페이징 계산기 (팀 PaginateUtil과 별개)
 *
 * 팀 PaginateUtil.paging()은 <div><a>...</a></div> HTML 문자열을 자바가 조립한다(JSP용).
 * 이 클래스는 그 대신 "숫자만" 계산하고, 화면(th:each)이 링크를 직접 그린다.
 *
 * 서비스가 이 계산기를 불러 startPage/endPage 등을 얻어 model 에 담는다.
 */
@Service
public class PagingUtil {

	// 하단 페이지 번호를 한 블록에 몇 개 보여줄지 (1~10, 11~20 ...)
	private static final int NUM_PER_BLOCK = 10;

	// 블록 시작 페이지 번호 (예: 15페이지 -> 11)
	public int startPage(int currentPage) {
		return ((currentPage - 1) / NUM_PER_BLOCK) * NUM_PER_BLOCK + 1;
	}

	// 블록 끝 페이지 번호 (단, 전체 페이지 수를 넘지 않음)
	public int endPage(int startPage, int totalPage) {
		int endPage = startPage + NUM_PER_BLOCK - 1;
		return Math.min(endPage, totalPage);
	}
}
