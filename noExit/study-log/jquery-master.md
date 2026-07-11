# jQuery 마스터 치트시트 (외우는 용)

> 핵심 사고: jQuery는 딱 4가지만 한다.
> **① 요소를 고른다 → ② 값을 읽거나 바꾼다 → ③ 이벤트를 건다 → ④ 서버와 통신한다(AJAX)**
> 이 4단계 틀만 머리에 있으면 아무것도 안 보고 짤 수 있다.

---

## 0. 기본 틀 (항상 이걸로 시작)

```js
$(function(){
    // 여기 안에 코드. "HTML 다 그려진 뒤 실행"이라는 뜻.
    // = document.addEventListener("DOMContentLoaded", ...) 와 같음
});
```
`$` = jQuery의 시작. `$(...)` 안에 뭘 넣느냐로 갈린다:
- `$(함수)` → 준비되면 실행
- `$("선택자")` → 요소 고르기
- `$(document)`, `$(this)` → 특정 대상

---

## ① 요소 고르기 (CSS 선택자 그대로)

```js
$("#id")          // id로 (제일 많이 씀)
$(".class")       // class로
$("tag")          // 태그로 (예: $("tbody"))
$("[name='xx']")  // 속성으로
$(this)           // 지금 이벤트가 일어난 그 요소
```

---

## ② 값 읽기 / 바꾸기 (같은 함수, 인자 있으면 "쓰기")

```js
// 텍스트
$("#x").text()          // 읽기
$("#x").text("안녕")     // 쓰기

// HTML (태그 포함)
$("#x").html()          // 읽기
$("#x").html("<b>굵게</b>")  // 쓰기

// input 값
$("#x").val()           // 읽기
$("#x").val("값")        // 쓰기

// 속성
$("#x").attr("href")            // 읽기
$("#x").attr("href", "/new")    // 쓰기

// data-* 속성 (data-reservation-id 등)
$("#x").data("reservation-id")  // 읽기 (data- 뗀 이름)

// 클래스
$("#x").addClass("active")      // 추가
$("#x").removeClass("active")   // 제거
$("#x").toggleClass("active")   // 있으면 빼고 없으면 넣고

// 보이기/숨기기
$("#x").show()
$("#x").hide()

// 내용 비우기 / 붙이기
$("#x").empty()                 // 안쪽 다 지움
$("#x").append("<li>추가</li>") // 뒤에 붙임
```

> ★규칙: **인자 없으면 읽기, 인자 있으면 쓰기.** (text/html/val/attr 다 이 규칙)

---

## ③ 이벤트 걸기 (이런 일 생기면 → 이거 실행)

```js
$("#btn").on("click", function(){
    // 버튼 클릭되면 실행
});

// 자주 쓰는 이벤트
"click"   // 클릭
"change"  // select/체크박스 값 바뀜
"keydown" // 키 누름
"submit"  // 폼 제출
```

**★동적 요소는 위임(delegation)으로:**
```js
// AJAX로 나중에 생긴 요소는 위 방식이 안 먹음.
// 부모(document)에 걸고, 진짜 대상은 두번째 인자로.
$(document).on("click", ".detailBtn", function(){
    const id = $(this).data("reservation-id");  // 클릭된 그 버튼
});
```
> 이유: 이벤트 걸 때 없던 요소(나중에 append로 생김)라서, 항상 있는 부모에 걸어야 한다.

---

## ④ AJAX (서버와 통신 — 화면 새로고침 없이)

```js
$.ajax({
    url:  "/owner/resList/delete",  // 어디로
    type: "POST",                   // GET(조회) / POST(변경)
    data: { resId: 5 },             // 보낼 값 (key:value)
    dataType: "json",               // 받을 형식
    success: function(res){         // 성공하면 res = 서버 응답
        alert(res.message);
        if(res.success) $(".row").remove();
    },
    error: function(){              // 실패하면
        alert("오류");
    }
});
```

**축약형도 많이 씀:**
```js
$.get("/url", {param:1}, function(res){ ... });   // GET
$.post("/url", {param:1}, function(res){ ... });  // POST
```

**흐름:** 요청 보냄 → 서버가 JSON 돌려줌 → `success`의 res로 받음 → 화면 갱신.

---

## 실전 조립 패턴 (이 4개를 엮는 법)

### 패턴A: 버튼 누르면 목록 다시 그리기
```js
$("#searchBtn").on("click", function(){       // ③ 이벤트
    const keyword = $("#keyword").val();      // ② 값 읽기
    $.get("/list", {kwd: keyword}, function(res){  // ④ AJAX
        const tbody = $("tbody");             // ① 고르기
        tbody.empty();                        // ② 비우기
        res.forEach(function(item){
            tbody.append("<tr><td>"+item.name+"</td></tr>"); // ② 붙이기
        });
    });
});
```

### 패턴B: 모달에 값 채워 띄우기
```js
$(document).on("click", ".detailBtn", function(){  // ③ 위임 이벤트
    const id = $(this).data("id");                 // ② data 읽기
    $.post("/detail", {id:id}, function(item){     // ④ AJAX
        $("#modal-name").text(item.name);          // ② 값 쓰기
        $("#modal").modal("show");                 // (부트스트랩 모달)
    });
});
```

### 패턴C: 삭제 후 그 행 없애기
```js
function deleteOk(id){
    if(confirm("삭제?")){                    // 확인창
        $.post("/delete", {id:id}, function(res){  // ④ AJAX
            if(res.success) $(".row_"+id).remove(); // ② 그 행 제거
        });
    }
}
```

---

## 바닐라 JS ↔ jQuery 대응 (헷갈릴 때)

| 하는 일 | 바닐라 JS | jQuery |
|---|---|---|
| 요소 고르기 | `document.getElementById("x")` | `$("#x")` |
| 텍스트 바꾸기 | `el.innerText = "a"` | `$("#x").text("a")` |
| 값 읽기 | `el.value` | `$("#x").val()` |
| 클릭 이벤트 | `el.addEventListener("click", fn)` | `$("#x").on("click", fn)` |
| 준비되면 실행 | `DOMContentLoaded` | `$(function(){})` |

---

## 외우는 순서 (이대로 반복)
1. `$(function(){})` 틀 쓰기
2. **고르기** `$("#id")`
3. **읽기/쓰기** `.val() .text() .html() .attr() .data()`
4. **이벤트** `.on("click", fn)` (동적이면 `$(document).on("click",".cls",fn)`)
5. **AJAX** `$.ajax({url,type,data,success})`

> "① 고른다 → ② 읽고 쓴다 → ③ 이벤트 건다 → ④ 서버와 통신" — 이 문장만 외우면 나머지는 살이다.
