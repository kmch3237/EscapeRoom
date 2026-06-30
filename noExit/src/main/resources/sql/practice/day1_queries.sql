-- =====================================================================
--  Day 1 연습 — 회원가입 / 로그인 쿼리 (팀 3-테이블 구조 기준)
--   대상 테이블: USER_ACCOUNT(USER_ID PK, LOGIN_ID, PASSWORD, NICKNAME, CREATED_AT)
--              USER_INFO(USER_ID PK+FK, EMAIL, NAME, PHONE, GENDER, BIRTHDATE)
--   PK 채번: SEQ_USER_ID.NEXTVAL
--
--  다 쓰면 Claude 에게 "실행해줘"
-- =====================================================================


-- ① USER_ACCOUNT INSERT (PK 는 SEQ_USER_ID.NEXTVAL 로)
--    여기에 작성:




-- ② USER_INFO INSERT (①과 같은 USER_ID 값으로. 연습이니 일단 1 같은 숫자 직접 입력)
--    여기에 작성:




-- ③ 로그인 SELECT (LOGIN_ID 로 USER_ACCOUNT 에서 찾기, 필요한 컬럼만)
--    여기에 작성:



INSERT INTO USER_ACCOUNT (USER_ID, LOGIN_ID, NICKNAME, CREATED_AT)
VALUES(SEQ_USER_ID.NEXTVAL, 'HHONG1', 'GHDRLF', SYSDATE);


SELECT USER_ID
FROM USER_INFO UI JOIN USER_ ACCOUNT UA
ON UI.USER_ID = UA.USER_ID;

조인하는 방법을 모르겟어 다른사람들이 짠 조인을 보면 이해하느ㅜㄴ데
내가 직접 작성을하려니깐 
on을 언제쓰고 join을 언제쓰고 leftjoin은 언제쓰고
서브쿼리는 또 언제쓰는지.


mapper, xml 연결 통로
<mapper namespace="com.noexit.escaperoom.mapper.Usermapper">
    <select id="findByLoginId">
            resultType="com.noexit.escaperoom.model.UserAccount">

            SELECT USER_ID, LOGINT_ID, PASSWORD, NICKNAME
            FROM USER_ACCOUNT
            WHERE LOGINT_ID=${LOGINID}
    </select>
</mapper>