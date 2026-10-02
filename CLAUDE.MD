# NoExit 프로젝트 작업 규칙

## 기술 스택
- Spring Boot 3.5.14 / Java 21 / Gradle / war 패키징 + JSP
- MyBatis, 메일 발송(환경변수 MAIL_USER / MAIL_PASSWORD)
- DB: 원래 Oracle(ojdbc11) → MariaDB로 변환 중

## 배포 서버
- Rocky Linux 9.7, MariaDB 설치 완료 (DB/계정: noexit)
- 빌드: ./gradlew clean bootWar
- 실행 포트: 8080 (MariaDB가 3306을 쓰므로 server.port=3306 사용 금지)

## 현재 목표
- Oracle → MariaDB 변환
  - 매퍼 XML의 Oracle 전용 문법 (NEXTVAL, SYSDATE, NVL, ROWNUM, DECODE, TO_CHAR, TO_DATE, DUAL 등)
  - src/main/resources/sql/ 의 DDL (VARCHAR2, NUMBER, 시퀀스 등)
  - PL/SQL 암호화/복호화 함수 대체 방법

## 작업 방식
- 한국어로 답변
- 수정 전에 변경 계획을 먼저 보여주고 확인받을 것
- 한 번에 전부 바꾸지 말고 파일 단위로 나눠서 진행
- 비밀번호, 계정 정보는 코드에 직접 쓰지 말 것 (환경변수 사용)
- 왜 바꾸는지 한 줄씩 설명
