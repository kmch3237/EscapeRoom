# JavaScript 마스터 치트시트 (외우는 용)

> 핵심 3가지만 먼저:
> 1. `.`(점) = "~의" → 왼→오로 파고듦 (document.querySelector(x).value.trim())
> 2. `()` = 값 전달(파라미터) / `{}` = 코드 묶음(몸통) → 완전 다름
> 3. `let 이름 = 값` = 상자 만들어 값 담기

---

## 1. 기호(symbol) 사전

| 기호 | 이름 | 하는 일 |
|---|---|---|
| `//` | 한 줄 주석 | 이 줄 실행 안 함(설명용) |
| `/* */` | 여러 줄 주석 | 감싼 부분 실행 안 함 (th:inline 에선 서버값 숨기는 용도 /*[[${x}]]*/) |
| `()` | 소괄호 | 함수 호출 / 값 전달(파라미터) |
| `{}` | 중괄호 | 코드 묶음(함수·if 몸통) / 객체 |
| `[]` | 대괄호 | 배열(목록) / 속성 선택자 [name='x'] |
| `""` `''` | 따옴표 | 글자(문자열). 안은 그냥 텍스트 |
| `` ` ` `` | 백틱 | 문자열 + 값 섞기 `${x}원` |
| `.` | 점 | "~의" (A.B = A의 B). 왼→오로 파고듦 |
| `=` | 대입 | 오른쪽 값을 왼쪽에 넣기 |
| `==` `===` | 비교 | 같은가? (===는 타입까지 엄격 비교) |
| `!` | 부정 | 반대로 (!x = x가 없으면/거짓이면) |
| `!=` `!==` | 비교 | 다른가? |
| `&&` `||` | 그리고 / 또는 | 조건 연결 |
| `+` | 더하기/잇기 | 숫자 더하기 or 글자 이어붙이기 |
| `? :` | 삼항 | 조건 ? 참일때 : 거짓일때 |
| `;` | 세미콜론 | 문장 끝 |
| `=>` | 화살표 | 함수 축약 (item => item.name) |

---

## 2. 단어(keyword) 사전

### 변수/함수
| 단어 | 하는 일 |
|---|---|
| `let` | 변수 만들기 (값 바뀔 수 있음) |
| `const` | 변수 만들기 (값 안 바뀜) |
| `function` | 함수 선언 ("이런 동작을 만든다") |
| `return` | 함수 끝내고 값 돌려주기/나가기 |

### 요소 찾기 (DOM)
| 단어 | 하는 일 |
|---|---|
| `document` | 웹페이지 전체(문서 객체) |
| `document.querySelector("선택자")` | 요소 하나 찾기 (CSS 선택자) |
| `document.getElementById("id")` | id로 요소 찾기 |
| `.value` | input 에 입력된 값 |
| `.innerText` | 요소 안의 글자 (읽기/쓰기) |
| `.innerHTML` | 요소 안의 HTML (태그 포함) |
| `.trim()` | 앞뒤 공백 제거 |
| `.checked` | 체크박스/라디오 선택 여부 |

### 조건/반복
| 단어 | 하는 일 |
|---|---|
| `if (조건) { }` | 만약 ~라면 |
| `else { }` | 아니면 |
| `list.forEach(function(item){ })` | 목록 하나씩 반복 |

### 자주 쓰는 함수
| 단어 | 하는 일 |
|---|---|
| `alert("글")` | 경고창 |
| `confirm("글")` | 확인/취소 창 (true/false 돌려줌) |
| `parseInt(값, 10)` | 문자 → 정수 (10 = 10진수) |
| `console.log(값)` | 개발자도구에 출력(디버깅) |
| `location.href = "주소"` | 페이지 이동 |
| `location.reload()` | 새로고침 |

### 서버 통신 (fetch = 순수 JS AJAX)
```javascript
fetch('/url', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(데이터)   // JS 객체 → JSON 문자열
})
.then(res => res.json())   // 응답을 JSON 으로
.then(data => { ... })     // 그 data 로 처리
.catch(() => { ... });     // 에러 시
```
- `fetch` = 서버에 요청 (jQuery의 $.ajax 순수JS 버전)
- `.then(콜백)` = 성공하면 이거 실행 (비동기라 순서 보장용)
- `JSON.stringify` = JS 객체를 JSON 문자열로 (보낼 때)
- `res.json()` = 받은 JSON 을 JS 객체로 (받을 때)

---

## 3. 객체 {} (key-value 보따리)

```javascript
const reviewData = {
    detailId: 5,           // key: value
    difficulty: 3,
    reviewComment: "재밌음"
};
reviewData.detailId        // 5 (점으로 꺼냄)
```
- `{}` 안에 `key: value` 여러 개 = 객체
- 자바의 Map/DTO 랑 같은 발상 (묶어서 다룸)

---

## 4. 한 줄 해부 예시 (읽는 법)

```javascript
let comment = document.querySelector("[name='partyComment']").value.trim();
```
```
let comment  → comment 상자 만들고
=            → 오른쪽 결과 담아라
document     → 웹페이지에서
.querySelector("[name='partyComment']")  → name이 partyComment인 요소 찾기
.value       → 그 요소의 입력값
.trim()      → 앞뒤 공백 제거
```
→ 점(.)을 따라 왼→오로 파고들며 읽는다.

---

## 외우는 순서
1. 기호: `. () {} [] = ! +` 부터
2. 변수: `let / const`
3. 찾기: `document.querySelector / getElementById / .value / .innerText`
4. 조건: `if / forEach`
5. 통신: `fetch ... .then`

> 핵심: "`.`은 ~의, `()`는 값전달, `{}`는 몸통" — 이 셋이 JS 읽기의 90%.
