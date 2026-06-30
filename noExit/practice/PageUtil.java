public class PageUtil{

    public int pagingCount(int size, int pageData){

    int pageCount = 0;
    pageCount = pageData/size + ((pageData%size>0) ? 1 : 0);
    

    return pageCount;
    }


    // ────────────────────────────────────────────────
    // [2단계] offset 구하기 = "앞에서 몇 개 건너뛰고 시작?"
    //   - 받는 것: page(몇 페이지를 보려나), size(한 페이지 개수)
    //   - 내보내는 것: 건너뛸 데이터 개수 (int)
    //   - 로직: (page - 1) * size
    // ────────────────────────────────────────────────
    public int offset(int size, int page){

    int offset = 0;

    offset = (page-1) * size;

    return offset;

    }


    // ── 검산용 main (여기는 손대지 마) ──
    // 두 메서드 다 (size, ...) 순서다: pagingCount(size, 데이터수), offset(size, page)
    public static void main(String[] args) {
        PageUtil util = new PageUtil();
        System.out.println("데이터23 / 10개씩 -> " + util.pagingCount(10, 23) + "  (정답 3)");
        System.out.println("데이터20 / 10개씩 -> " + util.pagingCount(10, 20) + "  (정답 2)");
        System.out.println("데이터5  / 10개씩 -> " + util.pagingCount(10, 5)  + "  (정답 1)");
        System.out.println("데이터0  / 10개씩 -> " + util.pagingCount(10, 0)  + "  (정답 0)");

        System.out.println("--- offset (size 10) ---");
        System.out.println("1페이지 -> " + util.offset(10, 1) + "  (정답 0)");
        System.out.println("2페이지 -> " + util.offset(10, 2) + "  (정답 10)");
        System.out.println("3페이지 -> " + util.offset(10, 3) + "  (정답 20)");
    }
}