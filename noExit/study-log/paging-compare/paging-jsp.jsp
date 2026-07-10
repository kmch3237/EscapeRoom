<%--
  ============================================================
  [방식 1] JSP 페이징 — "자바가 HTML을 다 그린다"
  ============================================================

  흐름:
    PaginateUtil.paging(currentPage, totalPage, listUrl)
      → 자바가 StringBuilder로 <div><a>...</a></div> 문자열을 통째로 조립
      → controller/service가 model에 "paging" 이라는 이름으로 담음
      → JSP는 그 문자열을 그냥 출력만 함 (${paging})

  즉 "그리는 일"을 자바(PaginateUtil)가 하고,
  화면은 결과 HTML을 받아 뿌리기만 한다.
--%>
<%@ page contentType="text/html; charset=UTF-8"%>

<%--
  자바가 만들어 넘겨준 HTML 문자열을 그대로 출력.
  ${paging} 안에는 이미 이런 완성된 HTML이 들어있음:

    <div class='paginate'>
      <a href='/mypage/reservations?tab=1&page=1' title='처음'>&laquo;</a>
      <a href='/mypage/reservations?tab=1&page=10' title='이전'>&lt;</a>
      <span class='active'>11</span>
      <a href='/mypage/reservations?tab=1&page=12'>12</a>
      ...
      <a href='/mypage/reservations?tab=1&page=20' title='다음'>&gt;</a>
    </div>
--%>
${paging}


<%--
  ★ 이 방식의 특징 / 단점 (면접 포인트)
  ------------------------------------------------------------
  1) 관심사 분리(SoC) 위반:
     화면(HTML 모양)을 "자바 코드"가 결정한다.
     디자인을 바꾸려면 자바 파일(PaginateUtil)을 고쳐야 함 → 화면/로직이 엉킴.

  2) ${paging} 은 EL 이라 HTML을 escape 하지 않고 그대로 출력한다.
     그래서 태그가 살아서 렌더되지만, 만약 저 문자열에 사용자 입력이 섞이면 XSS 위험.

  3) 자바에서 문자열 + 문자열로 태그를 이어붙이므로 가독성이 떨어지고 실수 잦음.

  → 그래서 Thymeleaf에서는 "자바는 숫자만 주고, 화면이 그린다"로 바꾼다. (방식 2 참고)
--%>
