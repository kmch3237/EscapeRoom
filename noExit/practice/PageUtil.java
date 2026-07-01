public class PageUtil{

// offset, fech
// size, totalPage,  currentPage?
// startPage, endPage, 

    // 뭐를 받아서 뭐를 넘겨주는거 였드라..
    // 이 클래스가 왜 필요한지를 몰라서 감이 안오는거같아
    // 이 클래스를 만들어서 서비스에서 어떻게 써먹는지 머릿속에 안떠오르니..
    public int page(int size, int totalPage){
        // size = 게시물을 몇개씩 보여줄지, totalPage = 전체 페이지 수
        // 이 메소드에서 이 두 매개변수를 받고 뭘 줄까
        // 아 전체 페이지수와 게시물을 몇개씩 보여줄지를 받으면
        // ex) 1~10은 1~10페이지, 11~20페이지가 보이게
        // 그러니깐 dataCount의 값에 따라 몇페이지부터 몇페이지까지를 보여줄지를 알 수 있게.. 이거다!
        
        int 보여줄페이지수 = 0;

        // 1차 dataCount = (size -1) / totalPage;
        보여줄페이지수 = totalPage % size  + (totalPage/size >= 0 ? 1 : 0);  // 2차 몇페이지가 나옴 ex 23/10 = 3페이지 

        return 보여줄페이지수;
    }
    // 1~10, 11~20, 21~30 이 표시를 해줄 변수를 구해야함
    // 전체페이지수 / size 
    public int numberPage(int size, int totalPage)
    {
        int paging = 0;

        paging = totalPage / size; 
        //offset = 

        return paging;
    }

    // 시작 페이지, 끝 페이지
    //public int ?? (int )
}