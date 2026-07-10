/*
 * ============================================================
 * PagingUtil — Thymeleaf 전용 페이징 계산기 (내 버전)
 * ============================================================
 *
 * 팀 코드(common/PaginateUtil)는 <div><a>...</a></div> HTML 문자열을
 * 자바가 직접 조립해서 넘긴다(JSP용). 화면 모양을 자바가 정함 = 관심사 분리 위반.
 *
 * 이 클래스는 그 대신 "숫자만" 계산한다.
 *   totalPage / offset / startPage / endPage
 * 서비스가 이 숫자들을 model 에 담아주면, 화면(th:each)이 링크를 직접 그린다.
 *
 * [서비스에서 쓰는 법]
 *   int totalPage = util.totalPage(dataCount, size);   // 전체 페이지 수
 *   int offset    = util.offset(currentPage, size);    // SQL 에 넘길 시작 행
 *   int startPage = util.startPage(currentPage);       // 하단 블록 시작 번호
 *   int endPage   = util.endPage(startPage, totalPage);// 하단 블록 끝 번호
 *   // → model 에 totalPage, currentPage, startPage, endPage 담아 화면으로
 */
public class PagingUtil {

    // 하단 페이지 번호를 한 블록에 몇 개 보여줄지 (1~10, 11~20 ...)
    private static final int NUM_PER_BLOCK = 10;

    /*
     * ① 전체 페이지 수
     *    데이터 개수 ÷ 페이지당 개수, 나머지가 있으면 +1 (올림).
     *    예: 23건, 10개씩 → 23/10=2, 나머지 3 있음 → 2+1 = 3페이지
     */
    public int totalPage(int dataCount, int size) {
        if (dataCount <= 0 || size <= 0) {
            return 0;
        }
        return dataCount / size + (dataCount % size > 0 ? 1 : 0);
    }

    /*
     * ② SQL OFFSET — 현재 페이지 앞에서 건너뛸 행 수
     *    예: 3페이지, 10개씩 → (3-1)*10 = 20 (21번째 행부터 조회)
     *    OFFSET #{offset} ROWS FETCH FIRST #{size} ROWS ONLY 에 씀.
     */
    public int offset(int currentPage, int size) {
        int offset = (currentPage - 1) * size;
        return offset < 0 ? 0 : offset;
    }

    /*
     * ③ 블록 시작 페이지 번호
     *    예: 15페이지 → ((15-1)/10)*10 + 1 = 1*10 + 1 = 11  → 블록 [11~20]
     */
    public int startPage(int currentPage) {
        return ((currentPage - 1) / NUM_PER_BLOCK) * NUM_PER_BLOCK + 1;
    }

    /*
     * ④ 블록 끝 페이지 번호 (단, 전체 페이지 수를 넘지 않음)
     *    예: startPage=11 → 11+10-1 = 20, 전체가 13페이지면 min(20,13)=13
     */
    public int endPage(int startPage, int totalPage) {
        int endPage = startPage + NUM_PER_BLOCK - 1;
        return Math.min(endPage, totalPage);
    }
}
