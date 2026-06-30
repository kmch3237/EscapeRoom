-- ============================================================
--  getThemeList 직접 재작성 연습 (2026-06-30)
--  - 한 단계씩 쌓아간다. 테스트할 SELECT 하나만 남기고 나머지는 주석처리.
--  - 주의: 줄 끝 인라인 주석은 파이프 실행시 에러나니 피하기.
-- ============================================================


-- ──────────────────────────────────────────────
-- [1~3단계 완료] ROOM + ROOM_GENRE + CAFE 조인
--   방 이름/가격 + 카페 이름 + 장르 이름을 한 줄로 합치기
--   JOIN ON = "두 테이블을 공통 컬럼(FK=PK)으로 연결"  (= 양쪽이 서로 다른 테이블)
-- ──────────────────────────────────────────────
SELECT R.ROOM_NAME, R.PRICE, C.CAFE_NAME, G.GENRE_NAME
FROM ROOM R
JOIN ROOM_GENRE G ON R.GENRE_ID = G.GENRE_ID
JOIN CAFE C ON R.CAFE_ID = C.CAFE_ID;


-- ──────────────────────────────────────────────
-- [4단계 완료] 페이징: 2페이지(4개씩) = offset (2-1)*4 = 4
--   ORDER BY 필수(정렬 없으면 페이지 의미 없음)
--   OFFSET n ROWS = 앞 n개 건너뛰기 / FETCH NEXT m ROWS ONLY = m개만
-- ──────────────────────────────────────────────
SELECT R.ROOM_ID, R.ROOM_NAME, R.PRICE, C.CAFE_NAME, G.GENRE_NAME
FROM ROOM R
JOIN ROOM_GENRE G ON R.GENRE_ID = G.GENRE_ID
JOIN CAFE C ON R.CAFE_ID = C.CAFE_ID
ORDER BY R.ROOM_ID
OFFSET 4 ROWS FETCH NEXT 4 ROWS ONLY;


-- ──────────────────────────────────────────────
-- [최종] 오늘 조각들을 MyBatis 매퍼 형태로 합친 getThemeList (내 버전)
--   * 아래는 XML 문법이라 sqlplus로는 안 돌아감. 매퍼에 들어갈 모양.
--   * 컬럼 AS 별칭(themeId 등) = resultType DTO 필드와 매핑
--   * #{offset} #{size} = 값 → 안전한 바인드변수
--   * <where>+<if> = 검색어 있을 때만 조건 붙이고 맨앞 AND 자동제거
-- ──────────────────────────────────────────────
-- <select id="getThemeList" parameterType="map" resultType="com.noexit.app.model.ThemeDTO">
--     SELECT R.ROOM_ID   AS themeId,  R.ROOM_NAME AS themeName, R.PRICE   AS price,
--            R.ROOM_IMG  AS imagePath, C.CAFE_NAME AS cafeName,  G.GENRE_NAME AS genre
--     FROM ROOM R
--     JOIN ROOM_GENRE G ON R.GENRE_ID = G.GENRE_ID
--     JOIN CAFE C       ON R.CAFE_ID  = C.CAFE_ID
--     <where>
--         <if test="kwd != null and kwd != ''">
--             AND G.GENRE_NAME LIKE '%' || #{kwd} || '%'
--         </if>
--     </where>
--     ORDER BY R.ROOM_ID DESC
--     OFFSET #{offset} ROWS FETCH NEXT #{size} ROWS ONLY
-- </select>
