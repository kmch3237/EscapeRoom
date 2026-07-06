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




-- ============================================================
--  문제 #2 — 집계 (GROUP BY / 집계함수 / HAVING)   ★새 유형
-- ============================================================
-- 개념: 여러 행을 그룹으로 "접어서" 요약값 한 줄로.
--   GROUP BY 컬럼  = 같은 값끼리 한 상자로 묶기
--   COUNT/AVG/MAX  = 상자 안에서 계산하는 집계함수
--   WHERE          = 묶기 "전" 개별 행 필터
--   HAVING         = 묶은 "후" 그룹(집계결과) 필터   ← 면접단골
--   철칙: SELECT 엔 GROUP BY 컬럼 or 집계함수만 올 수 있다
--
-- 테이블: ROOM(ROOM_ID, GENRE_ID, PRICE, ...) / ROOM_GENRE(GENRE_ID, GENRE_NAME)
--
-- [목표] 장르별 방 개수와 평균 가격을 구해라.
--        단, 방이 2개 이상인 장르만, 방 개수 많은 순으로. 화면엔 장르 이름 표시.

SELECT  g.GENRE_NAME,
        ____(*)          AS ROOM_CNT,      -- (1) 개수 세는 집계함수
        ____(r.PRICE)    AS AVG_PRICE      -- (2) 평균 내는 집계함수
FROM    ROOM r
JOIN    ROOM_GENRE g  ON  r.________ = g.________   -- (3) 조인 조건
________ ____ g.GENRE_NAME       -- (4) 장르별로 묶는 절(2단어) + 기준
________ ____(*) >= 2            -- (5) 그룹 거르는 절(1단어) + 조건
ORDER BY ROOM_CNT ____ ;         -- (6) 많은 순 정렬 방향

--목표: 장르별로 방이 몇 개인지, 평균 가격은 얼마인지 구해라.
--단, 방이 2개 이상인 장르만 보여주고, 방 개수 많은 순으로 정렬해라.
--화면엔 장르 이름(GENRE_NAME)이 나와야 한다.

SELECT 장르 이름, 장르별 방 갯수, 장르 별 평균 가격,
FROM ROOM R JOIN 
WHERE 방이 2개 이상인 장르
GROUP BY GENRE G
ORDER BY 방 개수 DESC  
HAVING ROOM_CNT > 2;

-- 이용해야 하는 테이블 ROOM / ROOM_GENRE(범레 장르아이디, 장르이름)
-- 목표: 장르별로 방이 몇 개인지, 평균 가격은 얼마인지 구해라.
-- 단, 방이 2개 이상인 장르만 보여주고, 방 개수 많은 순으로 정렬해라.
-- 화면엔 장르 이름(GENRE_NAME)이 나와야 한다.

-- 사고 방식
-- 1.ROOM테이블, ROOM_GENGE테이블 조인

SELECT 장르이름, 장르별 방 갯수, 장르별 평균 가격
FROM ROOM R JOIN ROOM_GENRE RG ON R.ROOM_ID = RG.ROOM_ID
GROUP BY ROOM_GENRE
HAVING 장르별 방 갯수 > 2


SELECT RG.GENRE_NAME, COUNT(*), AVG(R.PRICE)
FROM ROOM R JOIN ROOM_GENRE RG ON R.GENRE_ID = RG.GENRE_ID
GROUP BY ROOM_GENRE
HAVING 장르별 방 갯수 > 2


--  GROUP BY ROOM_GENRE > RG.GENRE_NAME
--   SELECT에 RG.GENRE_NAME을 꺼냈으니 걔가 GROUP BY에도 있어야 함.


SELECT  RG.GENRE_NAME,                    -- 장르 이름 (GROUP BY 에도 있으니 SELECT 가능)
        COUNT(*)       AS ROOM_CNT,       -- 각 장르 상자 안의 행 수 = 방 개수
        AVG(R.PRICE)   AS AVG_PRICE       -- 각 장르 상자 안 PRICE 평균
FROM    ROOM R                            -- 메인 테이블: 방
JOIN    ROOM_GENRE RG                     -- 장르표를 붙임
        ON R.GENRE_ID = RG.GENRE_ID       -- ★연결: 방의 장르ID = 장르표 PK (이름 아니라 관계로)
GROUP BY RG.GENRE_NAME                    -- 장르 이름이 같은 행끼리 한 상자로 접기
HAVING   COUNT(*) >= 2                    -- ★묶은 "후" 필터: 방 2개 이상 상자만 (별칭X·집계함수 그대로 / 이상이니 >=)
ORDER BY ROOM_CNT DESC;                   -- 방 개수 많은 순 (ORDER BY 엔 별칭 OK)
--  절 순서: SELECT -> FROM/JOIN -> (WHERE) -> GROUP BY -> HAVING -> ORDER BY




-- ============================================================
--  문제 #3 — 동적 SQL (<if> / <where> / <include>)  ★MyBatis
-- ============================================================
-- 목표: 방탈출 테마(ROOM) 검색. 조건은 있을 수도/없을 수도 있다.
--   - genreId 있으면  → 그 장르만
--   - minPrice 있으면 → 그 가격 이상만
--   - 항상            → 성인 테마 제외 (IS_ADULT = 0)
-- 파라미터: Map { genreId, minPrice }  (둘 다 없을 수 있음)
--
-- 테이블 ROOM: ROOM_ID, ROOM_NAME, GENRE_ID, PRICE, IS_ADULT ...
--
-- ▼ L1: MyBatis XML 동적 SQL 빈칸 채우기 ------------------------
--   힌트: <if test="...">, <where>, #{...}, 항상 붙는 조건엔 AND 미리 붙이기
/*
<select id="searchRooms" parameterType="map" resultType="...ThemeDTO">

    SELECT ROOM_ID, ROOM_NAME, GENRE_ID, PRICE
    FROM ROOM
    <where____>                                     -- (1) WHERE 똑똑하게 붙이는 태그

        AND IS_ADULT = 0                        -- 항상 붙는 조건(성인 제외)

        <_if_ test="genreId != null">             -- (2) 태그 이름
            AND GENRE_ID = #{_genreId____}            -- (3) 꺼낼 값 이름
        </if__>

        <if test="minPrice!=null_______">          -- (4) minPrice 있을 때 조건 (null 체크)
            AND PRICE >= #{minPrice}
        </if>

    </where_____>                                     -- (5) 닫기
    ORDER BY PRICE ASC
</select>
*/

-- ▼ L2: 위 XML이 케이스별로 "조립된 최종 순수 SQL"을 직접 써서 DB에 실행해봐
--   (sapiens 스키마에 ROOM 12개 있음)

-- 케이스1) map = {}  (조건 없음) → 성인제외 전체
-- 여기에 순수 SQL 작성:


-- 케이스2) map = { genreId: 1 } → 장르1 + 성인제외
-- 여기에 순수 SQL 작성:


-- 케이스3) map = { genreId: 1, minPrice: 24000 } → 장르1 + 24000이상 + 성인제외
-- 여기에 순수 SQL 작성:

