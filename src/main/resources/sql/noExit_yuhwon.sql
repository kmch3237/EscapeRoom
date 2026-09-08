SELECT USER
FROM DUAL;




--==============================================================================
-- FUNCTION(함수) 생성
--==============================================================================
-- 1. 파티인원수 구하는 함수
CREATE OR REPLACE FUNCTION FN_MEMBER_COUNT
( P_PARTY_ID IN PARTY.PARTY_ID%TYPE
)
RETURN NUMBER
IS
    V_TOTAL_COUNT   NUMBER := 0;
BEGIN

    SELECT COUNT(*) + 1 INTO V_TOTAL_COUNT
    FROM PARTY_MEMBER PM 
        JOIN PARTY_APPLY PA 
        ON PM.APPLY_ID = PA.APPLY_ID
        JOIN PARTY P 
        ON PA.PARTY_ID = P.PARTY_ID
    WHERE P.PARTY_ID = P_PARTY_ID 
        AND NOT EXISTS(
                    SELECT PK.MEMBER_ID
                    FROM PARTY_KICK PK
                    WHERE PK.MEMBER_ID = PM.MEMBER_ID
                    
                    );
                    
    RETURN V_TOTAL_COUNT;
    
    EXCEPTION 
        WHEN OTHERS THEN
            RETURN 0;

END;
/


-- 2. 예약오픈아이디 유저아이디를 보고 사장인지, 매니저인지, 일반사용자인지 판별하는 함수
-- 취소일자도 넘겨줘야함 
CREATE OR REPLACE FUNCTION FN_GET_USER_ROLE
(
     P_RES_OPEN_ID  RES_OPEN.RES_OPEN_ID%TYPE
    , P_USER_ID     RESERVATION_CANCEL.USER_ID%TYPE
    , P_CANCELD_AT  RESERVATION_CANCEL.CANCEL_AT%TYPE DEFAULT SYSDATE
)
RETURN VARCHAR2
IS
    V_CAFE_ID       CAFE.CAFE_ID%TYPE;
    V_OWNER_ID      CAFE.USER_ID%TYPE;
    V_ROLE_NAME     VARCHAR2(20) := 'USER';
    V_EVENT_ID      NUMBER;

BEGIN
    
    -- 입력한 예약오픈아이디가 어느 카페건지 확인 
    SELECT R.CAFE_ID INTO V_CAFE_ID
    FROM RES_OPEN RO  
        JOIN ROOM R
        ON RO.ROOM_ID = R.ROOM_ID
    WHERE RES_OPEN_ID = P_RES_OPEN_ID;
    
    -- 카페아이디로 카페 사장아이디 얻어내기 
    SELECT USER_ID INTO V_OWNER_ID
    FROM CAFE
    WHERE CAFE_ID = V_CAFE_ID;
    
    IF (V_OWNER_ID = P_USER_ID) THEN
        V_ROLE_NAME := 'OWNER';
        RETURN V_ROLE_NAME;
    END IF;
    
    -- 카페아이디로 매니저아이디 얻어내기
    BEGIN
        SELECT REG_EVENT_ID INTO V_EVENT_ID
        FROM 
        (
            SELECT REG_EVENT_ID 
            FROM MANAGER_HISTORY
            WHERE CAFE_ID = V_CAFE_ID 
                AND USER_ID = P_USER_ID
                AND CREATED_AT <= P_CANCELD_AT
            ORDER BY CREATED_AT DESC
        )
        WHERE ROWNUM = 1;
        
        IF (V_EVENT_ID = 1) THEN
            V_ROLE_NAME := 'MANAGER';
        END IF;

        
        EXCEPTION 
            WHEN NO_DATA_FOUND THEN
                V_ROLE_NAME := 'USER';
    END;
    
    RETURN V_ROLE_NAME;

    EXCEPTION
        WHEN OTHERS THEN
            RETURN 'USER';
END;
/



-- 3. 출석상태 판별함수(사용자예약아이디, 사용자아이디)
CREATE OR REPLACE FUNCTION FN_GET_ATTEND_STATUS
(
    P_RESERVATION_ID ATTENDANCE.RESERVATION_ID%TYPE
    , P_USER_ID        ATTENDANCE_DETAIL.USER_ID%TYPE
)
RETURN VARCHAR2
IS
    V_VALID_RESERVATION NUMBER;
    V_ATTEND_STATUS NUMBER;

BEGIN

    -- 출석체크테이블에 사용자예약아이디가 존재하는지 체크
        SELECT COUNT(*) INTO V_VALID_RESERVATION
        FROM ATTENDANCE
        WHERE RESERVATION_ID = P_RESERVATION_ID;
        
        IF (V_VALID_RESERVATION < 1) THEN
            RETURN '출석 미등록';
        END IF;
    
    BEGIN
        -- 파라미터로 넘겨받은 사용자예약아이디와 사용자아이디로 출석상태 반환 
        SELECT AD.ATTEND_STATUS_ID INTO V_ATTEND_STATUS
        FROM ATTENDANCE A
            JOIN ATTENDANCE_DETAIL AD
            ON A.ATTENDANCE_ID = AD.ATTENDANCE_ID
        WHERE AD.USER_ID = P_USER_ID
            AND A.RESERVATION_ID = P_RESERVATION_ID;
            
            IF (V_ATTEND_STATUS = 1)  THEN
                RETURN '출석 완료';
            ELSIF (V_ATTEND_STATUS = 2) THEN 
                RETURN '노쇼';
            ELSE
                RETURN '출석 미등록';
            END IF;
            
        EXCEPTION
            WHEN NO_DATA_FOUND THEN
                RETURN '출석 미등록';
    END;
    
    EXCEPTION
        WHEN OTHERS THEN
            RETURN '상태를 찾을 수 없습니다.';
    
END;
/


-- 4. 사용자예약번호와 사용자아이디로 사장/매니저/예약자/해당없음 판별하는 함수
CREATE OR REPLACE FUNCTION FN_GET_RESERVATION_ROLE
(
  P_RESERVATION_ID  RESERVATION.RESERVATION_ID%TYPE
, P_USER_ID         USER_INFO.USER_ID%TYPE
)
RETURN NUMBER
IS
    V_RES_USER_ID   USER_INFO.USER_ID%TYPE;
    V_RES_OPEN_ID   RES_OPEN.RES_OPEN_ID%TYPE;
    V_CAFE_ID       CAFE.CAFE_ID%TYPE;
    V_OWNER_ID      CAFE.USER_ID%TYPE;
    V_EVENT_ID      REG_EVENT.REG_EVENT_ID%TYPE;

BEGIN
    
    -- 파라미터로 넘겨 받은 예약아이디로 예약한 사람 찾기
    SELECT P.USER_ID, PR.RES_OPEN_ID INTO V_RES_USER_ID, V_RES_OPEN_ID
    FROM RESERVATION RV
        JOIN PARTY P
        ON RV.PARTY_ID = P.PARTY_ID
        JOIN PARTY_ROOM PR
        ON PR.PARTY_ID = P.PARTY_ID
    WHERE RV.RESERVATION_ID = P_RESERVATION_ID;
    
    IF (V_RES_USER_ID = P_USER_ID)  THEN
        RETURN 1;   --  1: 예약 당사자
    END IF;
    
    -- 예약오픈아이디가 어느 카페건지 확인 
    SELECT R.CAFE_ID INTO V_CAFE_ID
    FROM RES_OPEN RO  
        JOIN ROOM R
        ON RO.ROOM_ID = R.ROOM_ID
    WHERE RES_OPEN_ID = V_RES_OPEN_ID;
    
    -- 카페아이디로 카페 사장아이디 얻어내기 
    SELECT USER_ID INTO V_OWNER_ID
    FROM CAFE
    WHERE CAFE_ID = V_CAFE_ID;
    
    IF (V_OWNER_ID = P_USER_ID) THEN
        RETURN 2;   -- 2: 카페 사장
    END IF;
    

    -- 카페아이디로 매니저 찾기
    BEGIN
        SELECT REG_EVENT_ID INTO V_EVENT_ID
        FROM 
        (
            SELECT REG_EVENT_ID 
            FROM MANAGER_HISTORY
            WHERE CAFE_ID = V_CAFE_ID 
                AND USER_ID = P_USER_ID
                AND CREATED_AT <= SYSDATE
            ORDER BY CREATED_AT DESC
        )
        WHERE ROWNUM = 1;
        
        IF (V_EVENT_ID = 1) THEN
            RETURN 3; -- 3: 매니저 
        ELSIF (V_EVENT_ID !=1) THEN
            RETURN 0;
        END IF;

        
        EXCEPTION 
            WHEN NO_DATA_FOUND THEN
                RETURN 0;  -- 0: 해당없음
    END;

    RETURN -1;

    EXCEPTION
        WHEN OTHERS THEN
            RETURN -1;
END;
/



--==============================================================================
-- VIEW 생성
--==============================================================================


-- 1. 활성 파티(비활성파티 제외) 뷰
-- 파티아이디, 파티명, 파티장아이디, 성별조건, 방장한마디, 파티생성일자, 예약오픈예정번호, 예약오픈변경일 
CREATE OR REPLACE VIEW VW_ACTIVE_PARTY
AS
SELECT P.PARTY_ID, P.PARTY_NAME, P.USER_ID AS LEADER_ID
,PR.RES_OPEN_ID
, PR.CREATED_AT AS UPDATED_AT  
,P.GENDER_ID, P.MESSAGE
,P.CREATED_AT
FROM PARTY P JOIN PARTY_ROOM PR
    ON P.PARTY_ID = PR.PARTY_ID
WHERE NOT EXISTS(
                SELECT PD.PARTY_ID
                FROM PARTY_DROP PD
                WHERE P.PARTY_ID = PD.PARTY_ID
                );
                
                
-- 2. 활성 예약 오픈(비활성 예약 오픈 제외) 뷰
-- 예약오픈아이디, 테마(방)아이디, 테마명, 카페아이디, 카페명, 예약오픈일시, 등록일시,
CREATE OR REPLACE VIEW VW_ACTIVE_RES_OPEN
AS
SELECT RO.RES_OPEN_ID, R.CAFE_ID, C.CAFE_NAME
, RO.ROOM_ID, R.ROOM_NAME
, RO.OPEN_AT
, RO.CREATED_AT
FROM RES_OPEN RO JOIN ROOM R
    ON RO.ROOM_ID = R.ROOM_ID
    JOIN CAFE C
    ON R.CAFE_ID = C.CAFE_ID
WHERE NOT EXISTS (
    SELECT RD.RES_OPEN_ID 
    FROM RES_DROP RD 
    WHERE RO.RES_OPEN_ID = RD.RES_OPEN_ID
);




-- 3. 모든 예약 이력 뷰
-- 취소된거 포함 모든 예약 이력 조회
-- 사용자예약아이디, 파티아이디, 파티명, 파티장 아이디, 파티장이름, 파티장연락처, 파티원인원수(파티장포함)
-- ,예약오픈아이디, 카페아이디, 카페명, 테마아이디, 테마명, 예약오픈일시, 예약한일자 
CREATE OR REPLACE VIEW VW_RESERVATION_ALL
AS
SELECT RV.RESERVATION_ID, RC.CANCEL_ID, RC.USER_ID AS CANCEL_USER, RC.CANCEL_AT
, RO.RES_OPEN_ID, RO.OPEN_AT
, C.CAFE_ID, C.CAFE_NAME
, CASE 
    -- 휴대폰 (010, 011 등 11자리)
    WHEN LENGTH(C.PHONE) = 11 
        THEN SUBSTR(C.PHONE,1,3)||'-'||SUBSTR(C.PHONE,4,4)||'-'||SUBSTR(C.PHONE,8,4)
    -- 서울 (02로 시작, 10자리)
    WHEN SUBSTR(C.PHONE,1,2) = '02' AND LENGTH(C.PHONE) = 10
        THEN SUBSTR(C.PHONE,1,2)||'-'||SUBSTR(C.PHONE,3,4)||'-'||SUBSTR(C.PHONE,7,4)
    -- 서울 (02로 시작, 9자리)
    WHEN SUBSTR(C.PHONE,1,2) = '02' AND LENGTH(C.PHONE) = 9
        THEN SUBSTR(C.PHONE,1,2)||'-'||SUBSTR(C.PHONE,3,3)||'-'||SUBSTR(C.PHONE,6,4)
    -- 지역번호 3자리 (031 등, 11자리)
    WHEN LENGTH(C.PHONE) = 11
        THEN SUBSTR(C.PHONE,1,3)||'-'||SUBSTR(C.PHONE,4,4)||'-'||SUBSTR(C.PHONE,8,4)
    -- 지역번호 3자리 (031 등, 10자리)
    WHEN LENGTH(C.PHONE) = 10
        THEN SUBSTR(C.PHONE,1,3)||'-'||SUBSTR(C.PHONE,4,3)||'-'||SUBSTR(C.PHONE,7,4)
    ELSE C.PHONE
END AS PHONE
, R.ROOM_ID, R.ROOM_NAME
, P.PARTY_ID, P.PARTY_NAME
, P.USER_ID  AS LEADER_ID
, UI.NAME AS LEADER_NAME
, UI.PHONE AS LEADER_PHONE
, FN_MEMBER_COUNT(RV.PARTY_ID) AS TOTAL_MEMBER
, RV.CREATED_AT AS BOOKED_AT
FROM RESERVATION RV
    LEFT JOIN RESERVATION_CANCEL RC
        ON RV.RESERVATION_ID = RC.RESERVATION_ID
    JOIN PARTY_ROOM PR
        ON PR.PARTY_ID = RV.PARTY_ID
    JOIN RES_OPEN RO 
        ON PR.RES_OPEN_ID = RO.RES_OPEN_ID
    JOIN PARTY P
        ON PR.PARTY_ID = P.PARTY_ID
    JOIN USER_INFO UI
        ON P.USER_ID = UI.USER_ID
    JOIN ROOM R
        ON R.ROOM_ID = RO.ROOM_ID
    JOIN CAFE C
        ON R.CAFE_ID = C.CAFE_ID
ORDER BY RV.RESERVATION_ID;
        

SELECT *
FROM VW_RESERVATION_ALL
ORDER BY BOOKED_AT DESC;


-- 4. 예약 된 오픈(슬롯) 뷰
-- 어떤 예약오픈아이디가 어떤 파티에 예약이 되었는가
-- 예약오픈아이디, 카페아이디, 카페명, 테마아이디, 테마명, 예약오픈일시
-- 사용자예약아이디, 파티아이디, 파티명, 파티장, 파티장이름, 파티장연락처, 인원수(파티원+파티장), 예약된 일자 
-- 활성 사용자 예약 뷰에서 매칭 개설 번호를 찾아서
-- 매칭 예약(PARTY_ROOM)테이블과 조인으로 예약 오픈 예정 번호 찾기 
CREATE OR REPLACE VIEW VW_RES_OPEN_BOOKED
AS
SELECT VRA.RESERVATION_ID
, VRA.RES_OPEN_ID, VRA.OPEN_AT
, VRA.CAFE_ID, VRA.CAFE_NAME
, VRA.ROOM_ID, VRA.ROOM_NAME
, VRA.PARTY_ID, VRA.PARTY_NAME
, VRA.LEADER_ID
, VRA.LEADER_NAME
, VRA.LEADER_PHONE
, VRA.TOTAL_MEMBER
, VRA.BOOKED_AT
FROM VW_RESERVATION_ALL VRA
WHERE NOT EXISTS (
         SELECT 1
        FROM RESERVATION_CANCEL RC
        WHERE RC.RESERVATION_ID = VRA.RESERVATION_ID
);

SELECT RESERVATION_ID, CAFE_ID, CAFE_NAME, ROOM_ID, ROOM_NAME, OPEN_AT
FROM VW_RESERVATION_ALL
WHERE CANCEL_ID IS NOT NULL
AND OPEN_AT>SYSDATE;




SELECT *
FROM VW_RES_OPEN_BOOKED
ORDER BY BOOKED_AT DESC;

SELECT USER_ID
FROM CAFE
WHERE CAFE_ID=4;



-- 5. 예약 가능 목록 뷰
-- 예약오픈아이디, 예약오픈일시, 카페아이디, 카페명, 테마아이디, 테마명, 등록일
CREATE OR REPLACE VIEW VW_RES_OPEN_UNBOOKED
AS
SELECT VRO.RES_OPEN_ID, VRO.CAFE_ID, VRO.CAFE_NAME
    , VRO.ROOM_ID, VRO.ROOM_NAME
    , VRO.OPEN_AT
    , VRO.CREATED_AT
FROM VW_ACTIVE_RES_OPEN VRO
WHERE NOT EXISTS(
                    SELECT VRB.RES_OPEN_ID
                    FROM VW_RES_OPEN_BOOKED VRB
                    WHERE VRO.RES_OPEN_ID = VRB.RES_OPEN_ID
                )
ORDER BY OPEN_AT;




-- 6. 예약 취소 목록 뷰
-- 예약을 했었는데 취소했던 목록
-- 어떤 예약아이디가 어떤 카페, 테마, 일시였는데
-- 어떤 파티에서 언제 예약했다가 언제 예약취소를 했다
-- 혹은 카페관계자가 강제로 예약 취소를 언제 했다
-- 사용자예약아이디, 파티아이디, 파티명, 예약오픈아이디, 카페아이디, 카페명, 테마아이디, 테마명, 예약오픈일시, 예약한 날짜, 취소한 날짜, 취소타입, 취소한 사용자의 역할, 취소한 사용자의 아이디 
CREATE OR REPLACE VIEW VW_RES_OPEN_CANCELED
AS
SELECT VRA.RESERVATION_ID, VRA.PARTY_ID, VRA.PARTY_NAME
, VRA.RES_OPEN_ID, VRA.CAFE_ID, VRA.CAFE_NAME, VRA.ROOM_ID, VRA.ROOM_NAME
, VRA.OPEN_AT, VRA.BOOKED_AT
, VRA.CANCEL_AT AS CANCELD_AT
, CASE
    WHEN VRA.CANCEL_USER = VRA.LEADER_ID THEN 'USER_CANCEL'
    WHEN FN_GET_USER_ROLE(VRA.RES_OPEN_ID, VRA.CANCEL_USER, VRA.CANCEL_AT) IN ('OWNER', 'MANAGER') THEN 'CAFE_CANCEL'
    ELSE 'SYSTEM_CANCEL' END AS CANCEL_TYPE
, FN_GET_USER_ROLE(VRA.RES_OPEN_ID, VRA.CANCEL_USER, VRA.CANCEL_AT) AS ROLE
, VRA.CANCEL_USER
FROM VW_RESERVATION_ALL VRA
WHERE VRA.CANCEL_AT IS NOT NULL;


-- 7. 카페목록과 테마목록 뷰
-- 카페운영자 확인 
-- 수정필요 카페 목록 다시 잘 가져오기 - 매니저가 없으면 안나오는거를 처리해야
CREATE OR REPLACE VIEW VW_CAFE_ROOM_INFO
AS
SELECT C.CAFE_ID, C.CAFE_NAME
, C.USER_ID AS CAFE_OWNER
, VAM.USER_ID AS CAFE_MANAGER
, C.POSTAL_CODE||' '||C.ADDRESS||' '||C.ADDRESS_DETAIL AS CAFE_ADDR
, CASE 
    WHEN LENGTH(C.PHONE) = 11 
        THEN SUBSTR(C.PHONE,1,3)||'-'||SUBSTR(C.PHONE,4,4)||'-'||SUBSTR(C.PHONE,8,4)
    WHEN SUBSTR(C.PHONE,1,2) = '02' AND LENGTH(C.PHONE) = 10
        THEN SUBSTR(C.PHONE,1,2)||'-'||SUBSTR(C.PHONE,3,4)||'-'||SUBSTR(C.PHONE,7,4)
    WHEN SUBSTR(C.PHONE,1,2) = '02' AND LENGTH(C.PHONE) = 9
        THEN SUBSTR(C.PHONE,1,2)||'-'||SUBSTR(C.PHONE,3,3)||'-'||SUBSTR(C.PHONE,6,4)
    WHEN LENGTH(C.PHONE) = 11
        THEN SUBSTR(C.PHONE,1,3)||'-'||SUBSTR(C.PHONE,4,4)||'-'||SUBSTR(C.PHONE,8,4)
    WHEN LENGTH(C.PHONE) = 10
        THEN SUBSTR(C.PHONE,1,3)||'-'||SUBSTR(C.PHONE,4,3)||'-'||SUBSTR(C.PHONE,7,4)
    ELSE C.PHONE
END AS CAFE_TEL
, R.ROOM_ID, R.ROOM_NAME
, RG.GENRE_NAME
, R.DURATION
, R.MIN_PLAYERS ,R.MAX_PLAYERS
, R.ROOM_DESC
, R.ROOM_IMG
, R.PRICE
, CM.COMMON_NAME AS IS_ADULT
FROM CAFE C
    JOIN ROOM R
    ON C.CAFE_ID = R.CAFE_ID
    JOIN ROOM_GENRE RG
    ON R.GENRE_ID = RG.GENRE_ID
    JOIN COMMON CM
    ON R.IS_ADULT = CM.COMMON_ID
    LEFT JOIN VW_ACTIVE_MANAGER VAM
    ON C.CAFE_ID = VAM.CAFE_ID
;
    
    
    
    
    
    
    
    
-- 파티원 아이디별 예약된 시간대 조회 뷰
CREATE OR REPLACE VIEW VW_MEMBER_BOOKED_TIME
AS
-- 파티장의 예약 내역
SELECT UA.USER_ID 
, P.PARTY_ID, PR.RES_OPEN_ID
, RV.RESERVATION_ID, RO.ROOM_ID
, RO.OPEN_AT AS START_AT
, RO.OPEN_AT + R.DURATION /1440 AS END_AT
FROM USER_ACCOUNT UA
    JOIN PARTY P
    ON UA.USER_ID = P.USER_ID
    JOIN PARTY_ROOM PR
    ON P.PARTY_ID = PR.PARTY_ID
    JOIN RESERVATION RV
    ON P.PARTY_ID = RV.PARTY_ID
    JOIN RES_OPEN RO
    ON PR.RES_OPEN_ID = RO.RES_OPEN_ID
    JOIN ROOM R
    ON RO.ROOM_ID =R.ROOM_ID
WHERE RV.RESERVATION_ID NOT IN(SELECT RESERVATION_ID
                                FROM RESERVATION_CANCEL
) AND P.PARTY_ID NOT IN(SELECT PARTY_ID
                        FROM PARTY_DROP
) AND RO.RES_OPEN_ID NOT IN (SELECT RES_OPEN_ID
                            FROM RES_DROP
) AND R.ROOM_ID NOT IN (SELECT ROOM_ID
                        FROM ROOM_DROP
) AND UA.USER_ID NOT IN (SELECT USER_ID
                        FROM USER_DROP
) AND R.CAFE_ID NOT IN(SELECT CAFE_ID
                        FROM CAFE_DROP
)
UNION
-- 파티원의 예약 내역
SELECT  UA.USER_ID, P.PARTY_ID, PR.RES_OPEN_ID
, RV.RESERVATION_ID, RO.ROOM_ID
, RO.OPEN_AT AS START_AT
, RO.OPEN_AT + R.DURATION /1440 AS END_AT
FROM USER_ACCOUNT UA
    JOIN PARTY_APPLY PA
    ON UA.USER_ID = PA.USER_ID
    JOIN PARTY_MEMBER PM
    ON PA.APPLY_ID = PM.APPLY_ID
    JOIN PARTY P
    ON PA.PARTY_ID = P.PARTY_ID
    JOIN PARTY_ROOM PR
    ON P.PARTY_ID = PR.PARTY_ID
    JOIN RES_OPEN RO
    ON RO.RES_OPEN_ID = PR.RES_OPEN_ID
    JOIN ROOM R
    ON RO.ROOM_ID = R.ROOM_ID
    JOIN RESERVATION RV 
    ON RV.PARTY_ID = PR.PARTY_ID
WHERE RV.RESERVATION_ID NOT IN(SELECT RESERVATION_ID
                                FROM RESERVATION_CANCEL
) AND P.PARTY_ID NOT IN(SELECT PARTY_ID
                        FROM PARTY_DROP
) AND RO.RES_OPEN_ID NOT IN (SELECT RES_OPEN_ID
                            FROM RES_DROP
) AND R.ROOM_ID NOT IN (SELECT ROOM_ID
                        FROM ROOM_DROP
) AND UA.USER_ID NOT IN (SELECT USER_ID
                        FROM USER_DROP
) AND R.CAFE_ID NOT IN(SELECT CAFE_ID
                        FROM CAFE_DROP
) AND PM.MEMBER_ID  NOT IN (SELECT MEMBER_ID
                            FROM PARTY_KICK
)
;


--* VW_MEMBER_BOOKED_TIME 리팩토링
-- LEFT JOIN, NOT EXISTS, IS NULL 활용
-- NOT IN 은 직관적인지만 서브쿼리를 매번 실행하며, null위험이 있음
-- LEFT JOIN + IS NULL은 대용량에서 가장 효율적이며 실무에서 선호됨
-- NOT EXISTS는 Null에 안전하며 조건에 맞으면 조기 종료함








SELECT PA.USER_ID AS MEMBER, PR.RES_OPEN_ID, RO.OPEN_AT 
FROM PARTY_MEMBER PM
    JOIN PARTY_APPLY PA
    ON PM.APPLY_ID=PA.APPLY_ID
    JOIN PARTY P
    ON PA.PARTY_ID = P.PARTY_ID
    JOIN PARTY_ROOM PR
    ON PR.PARTY_ID = P.PARTY_ID
    JOIN RES_OPEN RO
    ON PR.RES_OPEN_ID = RO.RES_OPEN_ID
WHERE PM.MEMBER_ID NOT IN
        (   SELECT MEMBER_ID
            FROM PARTY_KICK
        );

SELECT *
FROM VW_RES_OPEN_BOOKED
WHERE OPEN_AT > SYSDATE;




--==============================================================================
-- PROCEDURE 생성
--==============================================================================


-- 예약 취소
-- 예약아이디가 예약취소가능한 상태인지 체크(아직 24시간 안지났는지 이미 완료된건지 이미 예약취소가 되어있는지 모두 확인 필요)
-- 사용자아이디(로그인아이디)와 예약취소할 예약아이디를 보고 예약한 아이디를 찾아 같거나(사용자)
-- 예약아이디가 물고 있는 테마의 사장이나 매니저인지 체크
-- 예약오픈시간을 가져와서 24시간 체크
-- 이건 예약당사자만 24시간 체크를 하면되는거고
-- 카페관계자는 직전까지는 예약 취소 가능함
-- 예약취소이력(RESERVATION_CANCEL) 테이블에 insert -> 예약취소이력번호(cancel_id) 사용자예약번호(reservation_id), user_id(취소한 사용자 번호), 취소 일자(sysdate)
-- 예약취소가 되면 동시에 예약번호가 물고 있던 파티를 찾아 매칭비활성화이력(party_drop)테이블 insert(party_drop_id, party_id, created_at) -- 트리거 사용
-- 예약번호가 물고 있던 파티장과 파티원들에게 이메일 전송 필요 -> 파티원, 파티장의 사용자아이디, 닉네임, 이름, 전화번호, 이메일 찾아놓기
CREATE OR REPLACE PROCEDURE PRC_RESERVATION_CANCEL
( P_USER_ID         IN  USER_ACCOUNT.USER_ID%TYPE
, P_RESERVATION_ID  IN  RESERVATION.RESERVATION_ID%TYPE
)
IS
    V_HAS_ID        NUMBER;
    V_RES_OPEN_ID   NUMBER;
    V_ROLE          NUMBER;
    V_AVAILABLE     NUMBER;
    
    ERR_INVALID_USER    EXCEPTION;
    ERR_INVALID_RESERV  EXCEPTION;
    ERR_INVALID_CANCEL  EXCEPTION;
    ERR_INVALID_RIGHT   EXCEPTION;

BEGIN

    -- 파라미터 체크
    IF P_USER_ID IS NULL THEN
        RAISE ERR_INVALID_USER;
    END IF;
    
    IF P_RESERVATION_ID IS NULL THEN
        RAISE ERR_INVALID_RESERV; 
    END IF;
    
    -- 사용자 아이디가 유효한지 검사
    SELECT COUNT(*) INTO V_HAS_ID
    FROM USER_INFO
    WHERE USER_ID = P_USER_ID;
    
    IF (V_HAS_ID<1) THEN
        RAISE ERR_INVALID_USER;
    END IF;
    
    -- 권한 체크
    SELECT FN_GET_RESERVATION_ROLE(P_RESERVATION_ID, P_USER_ID) INTO V_ROLE
    FROM DUAL;
    
    IF (V_ROLE<1) THEN
        RAISE ERR_INVALID_RIGHT;
    END IF;
    
    -- 예약 취소 가능 시간 체크(예약자만 제한)
    IF(V_ROLE=1) THEN
        SELECT COUNT(*) INTO V_AVAILABLE
        FROM VW_RES_OPEN_BOOKED
        WHERE RESERVATION_ID = P_RESERVATION_ID
        AND OPEN_AT-1 > SYSDATE;
        
        IF (V_AVAILABLE < 1) THEN
            RAISE ERR_INVALID_CANCEL;
        END IF;
    ELSE 
        SELECT COUNT(*) INTO V_AVAILABLE
        FROM VW_RES_OPEN_BOOKED
        WHERE RESERVATION_ID = P_RESERVATION_ID
        AND OPEN_AT > SYSDATE;
        
        IF (V_AVAILABLE < 1) THEN
            RAISE ERR_INVALID_CANCEL;
        END IF;
        
    END IF;
    
    -- 예약취소이력테이블 인서트
    INSERT INTO RESERVATION_CANCEL
    VALUES (RESERVATION_CANCEL_SEQ.NEXTVAL, P_RESERVATION_ID, P_USER_ID, SYSDATE);
    
    -- 커밋
    COMMIT;
    
    EXCEPTION
        WHEN ERR_INVALID_USER THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20001, '회원 정보가 유효하지 않습니다.');
        WHEN ERR_INVALID_RESERV THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20002, '예약 정보가 유효하지 않습니다.');
        WHEN ERR_INVALID_CANCEL THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20003, '예약 취소가 불가능한 시간입니다.');
        WHEN ERR_INVALID_RIGHT THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20004, '권한이 없습니다.');
END;
/ 



-- 예약 오픈 등록 프로시저 생성
-- user_id, cafe_id, room_id,
-- 날짜, 시, 분 --> 날짜 타입으로 치환해서 합치기(애플리케이션단에서 처리)
-- 로그인한 아이디 확인(사장 혹은 매니저)
-- 입력한 날짜가 오늘+24시간 이후인지 확인(등록일 기준으로 24시간 이후만 등록 가능)
-- 동일한 카페, 룸, 일시가 있는지 확인
-- 이미 오픈된 것 중에 같은 카페, 룸, 날짜를 비교하고
-- 그 룸의 +소요시간이후만 등록 가능 처리
-- 없으면 예약 오픈 등록 가능(res_open) insert

-- 
CREATE OR REPLACE PROCEDURE PRC_RES_OPEN
(
P_USER_ID   IN  USER_ACCOUNT.USER_ID%TYPE
, P_ROOM_ID IN  ROOM.ROOM_ID%TYPE
, P_OPEN_AT IN  DATE
)
IS

    V_HAS_ID    USER_ACCOUNT.USER_ID%TYPE;
    V_ROLE      NUMBER;
    V_DURATION  ROOM.DURATION%TYPE;
    V_CONFLICT  NUMBER;

    ERR_INVALID_USER    EXCEPTION;
    ERR_INVALID_ROOM    EXCEPTION;
    ERR_INVALID_DATE    EXCEPTION;
    ERR_INVALID_RIGHT   EXCEPTION;
    ERR_INVALID_OPEN_AT EXCEPTION;
    ERR_TIME_CONFLICT   EXCEPTION;

BEGIN
       -- 파라미터 체크
    IF P_USER_ID IS NULL THEN
        RAISE ERR_INVALID_USER;
    END IF;

    IF P_ROOM_ID IS NULL THEN
        RAISE ERR_INVALID_ROOM; 
    END IF;
    
    IF P_OPEN_AT IS NULL THEN
        RAISE ERR_INVALID_DATE; 
    END IF;
    
    
    -- 사용자 아이디가 유효한지 검사
    SELECT COUNT(*) INTO V_HAS_ID
    FROM USER_INFO
    WHERE USER_ID = P_USER_ID;
    
    IF (V_HAS_ID<1) THEN
        RAISE ERR_INVALID_USER;
    END IF;
    
    -- 권한 체크
    -- 입력한 카페의 매니저와 사장을 조회해서 있는 지 확인
   
        SELECT COUNT(*) INTO V_ROLE
        FROM VW_CAFE_ROOM_INFO
        WHERE ROOM_ID = P_ROOM_ID
         AND (CAFE_OWNER= P_USER_ID OR CAFE_MANAGER= P_USER_ID);
         
        IF (V_ROLE<1) THEN
            RAISE ERR_INVALID_RIGHT;
        END IF;

    -- 입력 날짜 검증
       IF (P_OPEN_AT <= SYSDATE+1)  THEN
           RAISE ERR_INVALID_OPEN_AT;
       END IF;

    
    -- 이미 오픈된 것 중에 같은 룸아이디, 날짜를 비교, 
    -- 해당 룸의 소요시간 이후만 가능하도록 처리
    SELECT COUNT(*) INTO V_CONFLICT
    FROM RES_OPEN RO
        JOIN ROOM R 
        ON RO.ROOM_ID = R.ROOM_ID
    WHERE RO.ROOM_ID = P_ROOM_ID
        AND RO.RES_OPEN_ID NOT IN 
                        (SELECT RES_OPEN_ID 
                        FROM RES_DROP)
        AND P_OPEN_AT < RO.OPEN_AT + (R.DURATION+10) / 1440
        AND P_OPEN_AT > RO.OPEN_AT - (R.DURATION+10) / 1440;
    
    IF V_CONFLICT > 0 THEN
        RAISE ERR_TIME_CONFLICT;
    END IF;
    

        
    -- 예약 오픈 등록
    INSERT INTO RES_OPEN 
    VALUES (RES_OPEN_SEQ.NEXTVAL, P_ROOM_ID, P_OPEN_AT, P_USER_ID, SYSDATE);
    
    COMMIT;

    EXCEPTION
        WHEN ERR_INVALID_USER THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20001, '회원 정보가 유효하지 않습니다.');
        WHEN ERR_INVALID_ROOM THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20005, '테마 정보가 유효하지 않습니다.');
        WHEN ERR_INVALID_DATE THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20006, '날짜 정보가 유효하지 않습니다.');
        WHEN ERR_INVALID_RIGHT THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20004, '권한이 없습니다.');
        WHEN ERR_INVALID_OPEN_AT THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20007, '예약은 현재 시간으로부터 24시간 이후부터 가능합니다.');
        WHEN ERR_TIME_CONFLICT THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20008, '해당 시간 대에 이미 등록된 슬롯이 있습니다.');
END;
/





-- 예약 오픈 비활성화 프로시저
-- user_id, res_open_id
-- 파라미터 체크
-- 권한 검증
-- 예약 오픈 아이디 존재 여부 및 활성 상태인지 체크
-- 예약 오픈 아이디에 걸려있는 예약이 있는지 체크
-- 걸려 있는 예약이 있다면 이미 예약이 있으니 예약 취소를 먼저하라고 안내하고 종료
-- 걸려 있는 예약이 없다면 res_drop INSERT 

CREATE OR REPLACE PROCEDURE PRC_RES_DROP
( P_USER_ID         IN USER_ACCOUNT.USER_ID%TYPE
, P_RES_OPEN_ID     IN RES_OPEN.RES_OPEN_ID%TYPE
)
IS

    V_CAFE_ID               CAFE.CAFE_ID%TYPE;
    V_HAS_RES               NUMBER;
    V_IS_RES                NUMBER;
    V_HAS_ID                NUMBER;
    V_ROLE                  NUMBER;
    
    ERR_INVALID_USER        EXCEPTION;
    ERR_INVALID_RES_OPEN    EXCEPTION;
    ERR_INVALID_RIGHT       EXCEPTION;
    ERR_HAS_RES             EXCEPTION;

BEGIN

     -- 파라미터 체크
    IF P_USER_ID IS NULL THEN
        RAISE ERR_INVALID_USER;
    END IF;

    IF P_RES_OPEN_ID IS NULL THEN
        RAISE ERR_INVALID_RES_OPEN; 
    END IF;

    -- 사용자 아이디가 유효한지 검사
    SELECT COUNT(*) INTO V_HAS_ID
    FROM USER_INFO
    WHERE USER_ID = P_USER_ID;
    
    IF (V_HAS_ID<1) THEN
        RAISE ERR_INVALID_USER;
    END IF;
    
    -- 예약 오픈 아이디 존재 여부 및 활성 상태인지 체크
    SELECT COUNT(*) INTO V_HAS_RES
    FROM RES_OPEN
    WHERE RES_OPEN_ID = P_RES_OPEN_ID
        AND RES_OPEN_ID NOT IN( SELECT RES_OPEN_ID
                                FROM RES_DROP
                                WHERE RES_OPEN_ID = P_RES_OPEN_ID
                            );
    IF (V_HAS_RES<1) THEN
        RAISE ERR_INVALID_RES_OPEN;
    END IF;

    -- 권한 체크
    -- 입력한 예약등록아이디의 매니저와 사장을 조회해서 있는 지 확인
    SELECT R.CAFE_ID INTO V_CAFE_ID
    FROM RES_OPEN RO
        JOIN ROOM R 
        ON RO.ROOM_ID = R.ROOM_ID
    WHERE RES_OPEN_ID = P_RES_OPEN_ID;
   
    SELECT COUNT(*) INTO V_ROLE
    FROM VW_CAFE_ROOM_INFO
    WHERE CAFE_ID = V_CAFE_ID
        AND (CAFE_OWNER=P_USER_ID OR CAFE_MANAGER=P_USER_ID);
    
    IF (V_ROLE<1) THEN
        RAISE ERR_INVALID_RIGHT;
    END IF;
    
    -- 예약 오픈 아이디에 걸려있는 예약이 있는지 체크
    SELECT COUNT(*) INTO V_IS_RES
    FROM VW_RES_OPEN_BOOKED
    WHERE RES_OPEN_ID = P_RES_OPEN_ID;
    
    IF(V_IS_RES >0) THEN
        RAISE ERR_HAS_RES;
    END IF;
    
   INSERT INTO RES_DROP
   VALUES(RES_DROP_SEQ.NEXTVAL, P_RES_OPEN_ID, SYSDATE);
   
   COMMIT;
   
   EXCEPTION
        WHEN ERR_INVALID_USER THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20001, '회원 정보가 유효하지 않습니다.'); 
        WHEN ERR_INVALID_RES_OPEN THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20009, '예약 오픈 번호가 유효하지 않습니다.'); 
        WHEN ERR_INVALID_RIGHT THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20004, '권한이 없습니다.'); 
        WHEN ERR_HAS_RES THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20010, '진행 중인 예약 건이 있어 삭제할 수 없습니다. 예약 취소 처리 후 다시 시도해 주세요.'); 
END;
/


-- 예약 등록 프로시저
-- PARTY, PARTY_ROOM 조회해서
-- 파라미터 존재 확인
-- 파라미터로 받은 파티아이디랑 유저아이디 검증
-- 파티 활성화 조회
-- 예약오픈아이디 예약 가능한지 조회
-- 인원수 체크
-- 동시 등록 시도 차단
-- 유저아이디가 동시간대에 예약이 잡힌 것이 있는 지 확인?
-- 파티원이 동시간대에 예약이 잡힌게 있는지 확인 ? 
-- 파티조회해서 파티 활성화되어있는지 확인(이 유저가 파티장인지 확인, 파티 아이디 매칭 확인)
-- PARTY_MEMBER 테이블 조회해서 모두 레디 상태인지 체크
-- 인원수 체크해서 파티아이디에 묶여있는 RES_OPEN_ID로 테마 찾아서 최소인원, 최대인원 비교
-- 파티아이디로 RES_OPEN_ID가 예약 가능한 상태인지 체크
-- 여러 사람이 동시에 예약 시도시 처리할 방법(묶어두기)

    
    
    -- 파티아이디로 예약오픈아이디를 찾아서 예약이 가능한 상태인지 확인
    -- 새 예약 시간 구하기
    -- 여기서 LOCK 걸기
    -- 락을 건 테이블에서 직접 상태를 확인 하기(뷰는 구조에 따라 반영이 안될 수도 있음)
    -- FOR UPDATE는 내가 점유하고 있는 특정 행을 보호하는 것
    -- 뷰를 통해 조회를 하면 뷰 안에서 사용하는 테이블들이 내가 락을 건 테이블과
    -- 완전히 일치하지 않을 수 있고 그러면 락을 건 의미가 사라짐 -> 동시성 제어 깨짐
    -- 데이터 정합성을 100% 보장하기 위해 락이 걸린 바로 그 물리적인 테이블을 직접 조회해야 함
    -- 실제로 락의 지배를 받는 곳에서 판별을 해야 정확해짐
    
    -- 예약 등록된 res_open_id 찾기
    -- 예약 테이블에서 party_id 를 찾아서
    -- 그 파티아이디를 가지고 잇는 파티룸 테이블에서
    -- res_open_id를 찾고
    -- reservation_cancel에 같은 reservation_id가 없는거 중에서 res_open_id를 찾아야 함
    -- 그게 예약 등록된 RES_OPEN_ID인데 그게 존재하지 않는거를 찾아야 하는거고
-- 
--RESERVATION INSERT(P_PARTY_ID, P_USER_ID)


-- 설정된 테마가 성인테마인지 확인
SELECT *
FROM ROOM
WHERE IS_ADULT = 1;

-- 파티원들의 생년월일을 담아서 나이 확인



CREATE OR REPLACE PROCEDURE PRC_RESERVATION
( P_USER_ID USER_ACCOUNT.USER_ID%TYPE
, P_PARTY_ID   PARTY.PARTY_ID%TYPE
)
IS
    V_HAS_ID    NUMBER;
    V_PARTY     NUMBER;
    V_LEADER    NUMBER;
    V_RES_OPEN      NUMBER;  
    V_ROOM          NUMBER;  
    V_MEMBER_CNT    NUMBER;   
    V_MIN           NUMBER;   
    V_MAX           NUMBER;  
    V_NOT_READY     NUMBER;  
    V_OVERLAP       NUMBER;
    V_NEW_START     DATE;
    V_NEW_END       DATE;
    V_IS_ADULT      NUMBER;
    V_UNDERAGE      NUMBER;
    
    ERR_INVALID_USER    EXCEPTION;
    ERR_INVALID_PARTY   EXCEPTION;
    ERR_INVALID_RIGHT   EXCEPTION;  
    ERR_INVALID_CNT     EXCEPTION;  
    ERR_NOT_READY       EXCEPTION;  
    ERR_OVERLAP         EXCEPTION;  
    ERR_ALREADY_RES     EXCEPTION;  
    ERR_UNDERAGE        EXCEPTION;
    
BEGIN

    -- 파라미터 체크
    IF P_USER_ID IS NULL THEN
        RAISE ERR_INVALID_USER;
    END IF;

    IF P_PARTY_ID IS NULL THEN
        RAISE ERR_INVALID_PARTY; 
    END IF;

    -- 사용자 아이디가 유효한지 검사
    SELECT COUNT(*) INTO V_HAS_ID
    FROM USER_INFO
    WHERE USER_ID = P_USER_ID;
    
    IF (V_HAS_ID<1) THEN
        RAISE ERR_INVALID_USER;
    END IF;
    
    -- 파티 아이디 존재 여부 확인
    SELECT COUNT(*)  INTO V_PARTY
    FROM PARTY
    WHERE PARTY_ID NOT IN  (
                            SELECT PARTY_ID
                            FROM PARTY_DROP
                            ) 
        AND PARTY_ID=P_PARTY_ID;
    
   IF (V_PARTY<1) THEN
        RAISE ERR_INVALID_PARTY;
    END IF;
    
     -- 입력한 유저아이디가 입력한 파티아이디의 파티장인지 확인
    SELECT COUNT(*) INTO V_LEADER
    FROM PARTY
    WHERE PARTY_ID = P_PARTY_ID
        AND USER_ID = P_USER_ID;
        
    IF (V_LEADER < 1) THEN
        RAISE ERR_INVALID_RIGHT;
    END IF;
    
    -- 예약 가능 체크 및 Lock 걸기  
    BEGIN 
        SELECT RO.RES_OPEN_ID, R.ROOM_ID
        , RO.OPEN_AT, RO.OPEN_AT + (R.DURATION / 1440) 
        INTO V_RES_OPEN, V_ROOM , V_NEW_START, V_NEW_END
        FROM RES_OPEN RO
            JOIN ROOM R
            ON RO.ROOM_ID = R.ROOM_ID
            JOIN PARTY_ROOM PR
            ON PR.RES_OPEN_ID = RO.RES_OPEN_ID
            JOIN PARTY P
            ON PR.PARTY_ID = P.PARTY_ID
        WHERE P.PARTY_ID = P_PARTY_ID
            AND NOT EXISTS (
            -- 예약이 등록된 RES_OPEN_ID 찾기
                SELECT 1
                FROM PARTY_ROOM PR2
                    JOIN RESERVATION RV
                    ON PR2.PARTY_ID = RV.PARTY_ID
                WHERE PR2.RES_OPEN_ID = RO.RES_OPEN_ID
                 AND NOT EXISTS(
                                SELECT 1
                                FROM RESERVATION_CANCEL RC
                                WHERE RV.RESERVATION_ID = RC.RESERVATION_ID
                                )
                )
            AND NOT EXISTS(
                        SELECT 1
                        FROM RES_DROP RD
                        WHERE  RD.RES_OPEN_ID = RO.RES_OPEN_ID
                   )
        FOR UPDATE OF RO.RES_OPEN_ID;
        
        EXCEPTION
             WHEN NO_DATA_FOUND THEN
             RAISE ERR_ALREADY_RES;
    END;

    -- 파티 인원 수와 테마 최소인원 최대인원 수 확인
    SELECT FN_MEMBER_COUNT(P_PARTY_ID) INTO V_MEMBER_CNT
    FROM DUAL;
    
    SELECT MIN_PLAYERS, MAX_PLAYERS, IS_ADULT INTO V_MIN, V_MAX, V_IS_ADULT
    FROM ROOM
    WHERE ROOM_ID = V_ROOM;
    
    IF(V_MEMBER_CNT < V_MIN OR V_MEMBER_CNT > V_MAX) THEN
        RAISE ERR_INVALID_CNT;
    END IF;
    
    
    -- 성인테마인지 확인 후 파티원의 나이 조회해서 모두 성인인지 확인
    IF (V_IS_ADULT = 1) THEN
    
       -- 파티장과 파티원의 생년월일 받아서 FN_GET_USER_AGE에 담아서 나이 판별하기
       SELECT COUNT(*) INTO V_UNDERAGE
       FROM VW_PARTY_ACTIVE_MEMBER VPAM
        JOIN USER_INFO UI
        ON VPAM.USER_ID = UI.USER_ID
       WHERE PARTY_ID = P_PARTY_ID
        AND FN_GET_USER_AGE(UI.BIRTHDATE) < 19;
        
        IF (V_UNDERAGE > 0) THEN
            RAISE ERR_UNDERAGE;
        END IF;
    END IF;
        
   -- 파티아이디의 모든 멤버가 레디 상태인지 확인
   SELECT COUNT(*) INTO V_NOT_READY
   FROM VW_PARTY_ACTIVE_MEMBER
    WHERE PARTY_ID = P_PARTY_ID
        AND READY != 'READY';
    
    IF (V_NOT_READY > 0) THEN
        RAISE ERR_NOT_READY;
    END IF;

    -- 파티 구성원 중 시간 겹치는 예약 있는지 확인
    SELECT COUNT(*) INTO V_OVERLAP
    FROM VW_MEMBER_BOOKED_TIME
    WHERE USER_ID IN(
        SELECT USER_ID
        FROM PARTY
        WHERE PARTY_ID = P_PARTY_ID
        UNION
        SELECT PA.USER_ID
        FROM PARTY_APPLY PA
            JOIN PARTY_MEMBER PM
            ON PA.APPLY_ID = PM.APPLY_ID
        WHERE PM.MEMBER_ID NOT IN (SELECT MEMBER_ID
                                   FROM PARTY_KICK
            ) AND PA.PARTY_ID = P_PARTY_ID
        )
    AND V_NEW_START < END_AT
    AND V_NEW_END > START_AT;
    
    IF (V_OVERLAP > 0) THEN
        RAISE ERR_OVERLAP;
    END IF;
    
    -- 예약 등록 INSERT
    INSERT INTO RESERVATION (RESERVATION_ID, PARTY_ID, CREATED_AT)
    VALUES(RESERVATION_SEQ.NEXTVAL, P_PARTY_ID, SYSDATE);
    
    COMMIT;
    
    EXCEPTION
        WHEN NO_DATA_FOUND THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20098,'데이터를 찾을 수 없습니다.');
        WHEN ERR_INVALID_USER THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20001, '회원 정보가 유효하지 않습니다.'); 
        WHEN ERR_INVALID_PARTY THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20011, '파티 정보가 유효하지 않습니다.'); 
        WHEN ERR_INVALID_RIGHT THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20004, '권한이 없습니다.'); 
        WHEN ERR_ALREADY_RES THEN
             ROLLBACK;
             RAISE_APPLICATION_ERROR(-20012, '이미 예약된 테마입니다.'); 
        WHEN ERR_INVALID_CNT THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20013, '인원 설정 오류: 해당 테마는 ' || V_MIN ||'명부터 ' || V_MAX|| '명까지 예약 가능합니다.'); 
        WHEN ERR_NOT_READY THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20014, '일부 인원이 준비되지 않았습니다. 모든 팀원이 준비 완료된 후 다시 시도해 주세요.'); 
        WHEN ERR_OVERLAP THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20015, '파티원의 기존 예약 일정과 겹치는 시간이 있어 예약이 불가능합니다.'); 
        WHEN ERR_UNDERAGE THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20016, '성인 테마입니다. 파티원의 연령을 확인해 주세요.');
        WHEN OTHERS THEN
            ROLLBACK;
            RAISE_APPLICATION_ERROR(-20099, '예약 처리 중 오류가 발생했습니다.');
        
END;
/




-- FK 인덱스 처리 

-- PARTY_ROOM
CREATE INDEX IDX_PARTY_ROOM_RES_OPEN ON PARTY_ROOM(RES_OPEN_ID);
CREATE INDEX IDX_PARTY_ROOM_PARTY ON PARTY_ROOM(PARTY_ID);

-- RES_DROP
CREATE INDEX IDX_RES_DROP_RES_OPEN ON RES_DROP(RES_OPEN_ID);

-- MANAGER_HISTORY (뷰에서 자주 쓰임)
CREATE INDEX IDX_MGR_HISTORY_CAFE ON MANAGER_HISTORY(CAFE_ID);
CREATE INDEX IDX_MGR_HISTORY_USER ON MANAGER_HISTORY(USER_ID);




--==============================================================================
-- TRIGGER 생성
--==============================================================================
-- 1. RESERVATION_CANCEL 인서트 후 실행되는 PARTY_DROP 인서트 트리거
CREATE OR REPLACE TRIGGER TRG_RES_CANCEL_PARTY_DROP
AFTER INSERT ON RESERVATION_CANCEL
FOR EACH ROW
DECLARE
    V_PARTY_ID PARTY.PARTY_ID%TYPE;
BEGIN

    SELECT PARTY_ID INTO V_PARTY_ID
    FROM RESERVATION 
    WHERE RESERVATION_ID = :NEW.RESERVATION_ID;
    
    INSERT INTO PARTY_DROP
    VALUES(PARTY_DROP_SEQ.NEXTVAL, V_PARTY_ID, SYSDATE);
END;
/




SELECT *
FROM VW_RES_OPEN_BOOKED
WHERE OPEN_AT > SYSDATE;

SELECT UA.USER_ID, UA.NICKNAME
     , SUM(CASE WHEN AD.ATTEND_STATUS_ID = 1 THEN 1 ELSE 0 END) AS 출석완료
     , SUM(CASE WHEN AD.ATTEND_STATUS_ID = 2 THEN 1 ELSE 0 END) AS 노쇼
     , SUM(CASE WHEN AD.DETAIL_ID IS NULL THEN 1 ELSE 0 END) AS 출석미완료
FROM USER_ACCOUNT UA
    JOIN PARTY P ON UA.USER_ID = P.USER_ID
    JOIN RESERVATION RV ON P.PARTY_ID = RV.PARTY_ID
    JOIN ATTENDANCE A ON RV.RESERVATION_ID = A.RESERVATION_ID
    LEFT JOIN ATTENDANCE_DETAIL AD ON A.ATTENDANCE_ID = AD.ATTENDANCE_ID
                                   AND AD.USER_ID = UA.USER_ID
WHERE RV.RESERVATION_ID NOT IN (SELECT RESERVATION_ID FROM RESERVATION_CANCEL)
GROUP BY UA.USER_ID, UA.NICKNAME
HAVING SUM(CASE WHEN AD.DETAIL_ID IS NULL THEN 1 ELSE 0 END) > 0
ORDER BY 출석완료 DESC, 노쇼 DESC;


DELETE FROM ATTENDANCE_DETAIL
WHERE DETAIL_ID = (
    SELECT MIN(DETAIL_ID) FROM ATTENDANCE_DETAIL
);


-- 지울 DETAIL_ID 먼저 확인
SELECT MIN(DETAIL_ID) FROM ATTENDANCE_DETAIL;

-- MUTUAL_EVALUATION 먼저 삭제
DELETE FROM MUTUAL_EVALUATION
WHERE DETAIL_ID = (SELECT MIN(DETAIL_ID) FROM ATTENDANCE_DETAIL);

-- 그 다음 ATTENDANCE_DETAIL 삭제
DELETE FROM ATTENDANCE_DETAIL
WHERE DETAIL_ID = (SELECT MIN(DETAIL_ID) FROM ATTENDANCE_DETAIL);

rollback;
select *

from ATTENDANCE_detail;

from attend_status_id;


-- 지우기 전에 먼저 값 확인 및 복사
SELECT ME.* 
FROM MUTUAL_EVALUATION ME
WHERE ME.DETAIL_ID = (SELECT MIN(DETAIL_ID) FROM ATTENDANCE_DETAIL);
--==>>
/*
   EVAL_ID  DETAIL_ID  TARGET_ID QUESTION_ID  ANSWER_ID CREATED_
---------- ---------- ---------- ----------- ---------- --------
         1          1         11           1          1 26/03/22
         2          1         11           2          1 26/03/22
         3          1         12           1          1 26/03/22
         4          1         12           2          2 26/03/22
*/

SELECT AD.*
FROM ATTENDANCE_DETAIL AD
WHERE AD.DETAIL_ID = (SELECT MIN(DETAIL_ID) FROM ATTENDANCE_DETAIL);
--==>>
/*
 DETAIL_ID ATTENDANCE_ID    USER_ID ATTEND_STATUS_ID
---------- ------------- ---------- ----------------
         1             1         20                1
*/


SELECT USER_ID FROM CAFE WHERE CAFE_ID = 1;

-- 1. RES_OPEN (과거 슬롯)
INSERT INTO RES_OPEN (RES_OPEN_ID, ROOM_ID, OPEN_AT, USER_ID, CREATED_AT)
VALUES (RES_OPEN_SEQ.NEXTVAL, 1,
        TO_DATE('2026-05-01 14:00','YYYY-MM-DD HH24:MI'),
        8,  -- 사장 USER_ID (카페 1번 사장 확인 후 맞춰줘요)
        TO_DATE('2026-04-01 10:00','YYYY-MM-DD HH24:MI'));

-- 2. PARTY (파티장 USER_ID=5)
INSERT INTO PARTY (PARTY_ID, USER_ID, PARTY_NAME, GENDER_ID, CREATED_AT)
VALUES (PARTY_SEQ.NEXTVAL, 5, '출석미완료테스트', 1,
        TO_DATE('2026-04-20 10:00','YYYY-MM-DD HH24:MI'));

-- 3. PARTY_ROOM
INSERT INTO PARTY_ROOM (PARTY_ROOM_ID, RES_OPEN_ID, PARTY_ID, CREATED_AT)
VALUES (PARTY_ROOM_SEQ.NEXTVAL, RES_OPEN_SEQ.CURRVAL, PARTY_SEQ.CURRVAL,
        TO_DATE('2026-04-20 11:00','YYYY-MM-DD HH24:MI'));

-- 4. RESERVATION (USER_ID 없는 구조)
INSERT INTO RESERVATION (RESERVATION_ID, PARTY_ID, CREATED_AT)
VALUES (RESERVATION_SEQ.NEXTVAL, PARTY_SEQ.CURRVAL,
        TO_DATE('2026-04-30 10:00','YYYY-MM-DD HH24:MI'));

-- ATTENDANCE 안 넣음 → 출석미완료
COMMIT;

SELECT * FROM GENDER_CONDITION;


SELECT RV.RESERVATION_ID, P.USER_ID, UA.NICKNAME
FROM RESERVATION RV
    JOIN PARTY P ON RV.PARTY_ID = P.PARTY_ID
    JOIN USER_ACCOUNT UA ON P.USER_ID = UA.USER_ID
    JOIN PARTY_ROOM PR ON RV.PARTY_ID = PR.PARTY_ID
    JOIN RES_OPEN RO ON PR.RES_OPEN_ID = RO.RES_OPEN_ID
WHERE RO.OPEN_AT < SYSDATE
    AND RV.RESERVATION_ID NOT IN (SELECT RESERVATION_ID FROM RESERVATION_CANCEL)
    AND RV.RESERVATION_ID NOT IN (SELECT RESERVATION_ID FROM ATTENDANCE);


SELECT * FROM VW_RESERVATION_ALL WHERE ROWNUM = 1;




SELECT *
FROM VW_RES_OPEN_BOOKED;







SELECT *
FROM USER_ACCOUNT
ORDER BY 1 DESC;



select *
from user_info
order by 1 desc;
desc user_info;

select *
from user_drop;




select *
from VW_RES_OPEN_unbooked;

select *
from party
order by 1 desc;

select *
from room
order by 1 desc;

39

resopen117

select *
from res_open
order by 1 desc;

INSERT INTO RES_OPEN
VALUES (RES_OPEN_SEQ.NEXTVAL, '39', TO_DATE('2026-06-14 17:00','YYYY-MM-DD HH24:MI'), '45', SYSDATE );

select *
from party
ORDER BY 1 DESC;


PARTY_ID = 52


INSERT INTO PARTY
VALUES(PARTY_SEQ.NEXTVAL, '55', '파티 들어오세요', 0, '아무나 오세요',SYSDATE );

SELECT *
FROM PARTY_ROOM;


INSERT INTO PARTY_ROOM
VALUES(PARTY_ROOM_SEQ.NEXTVAL, '117', '52', SYSDATE);

SELECT *
FROM PARTY_APPLY
order by 1 desc;

apply_id 76

INSERT INTO PARTY_APPLY
VALUES(PARTY_APPLY_SEQ.NEXTVAL, '52', '51', '신청합니다.', sysdate);

select *
from party_MEMBER;

INSERT INTO PARTY_MEMBER
VALUES(PARTY_MEMBER_SEQ.NEXTVAL, '76', TO_DATE('2026-06-14 20:00', 'YYYY-MM-DD HH24:MI'), TO_DATE('2026-06-14 17:02','YYYY-MM-DD HH24:MI'));

SELECT *
FROM RESERVATION
ORDER BY 1 DESC
;

INSERT INTO RESERVATION
VALUES(RESERVATION_SEQ.NEXTVAL, '52', SYSDATE);



		SELECT P.PARTY_ID
		, C.CAFE_NAME
		, R.ROOM_NAME
		, TO_CHAR(RO.OPEN_AT, 'YYYY-MM-DD HH24:MI') AS OPEN_AT
		, P.USER_ID AS BOOKER_ID
		, UI.NAME AS BOOKER_NAME
		, SUBSTR(UI.PHONE,1,3)||'-'||SUBSTR(UI.PHONE,4,4)||'-'||SUBSTR(UI.PHONE,8,4) AS BOOKER_TEL
		, R.ROOM_IMG, R.PRICE
		, FN_MEMBER_COUNT(53) AS TOTAL_MEMBER
		FROM PARTY P 
		    JOIN PARTY_ROOM PR
		    ON P.PARTY_ID = PR.PARTY_ID
		    JOIN RES_OPEN RO
		    ON PR.RES_OPEN_ID = RO.RES_OPEN_ID
		    JOIN ROOM R
		    ON RO.ROOM_ID = R.ROOM_ID
		    JOIN CAFE C
		    ON R.CAFE_ID = C.CAFE_ID
		    JOIN USER_INFO UI
		    ON P.USER_ID = UI.USER_ID
		WHERE P.PARTY_ID = 53
		AND P.USER_ID = 55



COMMIT;


