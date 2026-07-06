


public class PaginUtil{

    private static final int numPerBlock =10;

    public int totalPage(int dataCount, int size){
        return dataCount / size + (dataCount % size > 0 ? 1 : 0);
    }

    public int startPage(int currentPage){
        return((currentPage -1)/numPerBlock) * numPerBlock + 1;
    }

    public int endPage(int startPage, int totalPage){
        int endPage = startPage + numPerBlock -1;
        if(endPage > totalPage){
            endPage = totalPage;
        }
        return endPage;
    }
}