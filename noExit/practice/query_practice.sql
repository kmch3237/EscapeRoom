-- ============================================================
--  복잡 쿼리 짜기 연습 (조인 / 서브쿼리 / 동적조건 / 페이징)
--  6단계 사고법: ① 결과 ② 출처 ③ 연결(JOIN) ④ 요약(서브쿼리) ⑤ 조건 ⑥ 정렬/제한
-- ============================================================

-- 테이블:
--   PARTY(PARTY_ID,PARTY_NAME,USER_ID) / PARTY_ROOM(PARTY_ID,RES_OPEN_ID)
--   RES_OPEN(RES_OPEN_ID,ROOM_ID,OPEN_AT) / ROOM(ROOM_ID,ROOM_NAME,ROOM_IMG)
--   VW_PARTY_ACTIVE_MEMBER(PARTY_ID, AGE, TEMP)  -- 멤버 1명=1행


-- ===== STEP 1~3 완료: 조인 + 멤버요약 서브쿼리 조립 =====
-- 핵심: STEP1이 뼈대(메인). STEP2는 ()로 감싼 파생테이블 E 로 "JOIN 한 줄" 추가.
-- 세미콜론은 문장 끝에 딱 하나. ON 뒤엔 조건만.
SELECT P.PARTY_ID   AS partyId,
       P.PARTY_NAME AS partyName,
       R.ROOM_NAME  AS themeName,
       R.ROOM_IMG   AS themeImg,
       TO_CHAR(RO.OPEN_AT, 'YYYY-MM-DD') AS resDate,
       TO_CHAR(RO.OPEN_AT, 'HH24:MI')    AS resTime,
       E.avgAge, E.avgTemp, E.memberCount
FROM PARTY P
JOIN PARTY_ROOM PR ON P.PARTY_ID = PR.PARTY_ID
JOIN RES_OPEN RO   ON PR.RES_OPEN_ID = RO.RES_OPEN_ID
JOIN ROOM R        ON RO.ROOM_ID = R.ROOM_ID
JOIN (
        SELECT PARTY_ID, AVG(AGE) AS avgAge, AVG(TEMP) AS avgTemp, COUNT(*) AS memberCount
        FROM VW_PARTY_ACTIVE_MEMBER
        GROUP BY PARTY_ID
     ) E
ON P.PARTY_ID = E.PARTY_ID
WHERE RO.OPEN_AT > SYSDATE + 1
AND LIKE {kwd}
ORDER BY PARTY_ID DESC FETCH FIRST 4 ROWS ONLY


-- ===== STEP 4: 조건(WHERE) + 정렬/페이징 (네가 작성) =====
-- 위 STEP1~3 쿼리에 아래 조건들을 추가해라 (⑤ 조건 ⑥ 정렬/제한):
--
--   (a) 미래 파티만: 슬롯 일시가 지금+1일 이후          RO.OPEN_AT > SYSDATE + 1
--   (b) 검색: 파티명(PARTY_NAME)에 특정 키워드 포함      (힌트: LIKE '%' || ? || '%'  또는  INSTR)
--   (c) 커서 페이징: 마지막으로 본 파티id 보다 작은 것부터  P.PARTY_ID < (지난 마지막 id)
--   (d) 최신순 정렬 후 4개만                             ORDER BY partyId DESC / FETCH FIRST 4 ROWS ONLY
--
-- [사고] ⑤ WHERE 는 어느 위치에? (FROM~JOIN 다 끝난 뒤, ORDER BY 앞)
--        여러 조건은 AND 로 연결.
-- [주의] 지금은 순수 SQL 연습이라 검색어/마지막id 는 실제 값으로 써봐 (예: '공포', 100).
--
-- STEP1~3 을 복사해서 아래에 WHERE / ORDER BY / FETCH 를 붙여 완성해봐:



