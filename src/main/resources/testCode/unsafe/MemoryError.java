import java.util.ArrayList;
import java.util.List;
public class Main {

    public static void main(String[] args) {

        // 规定一个存放整型数组List
        List<int[]> ints = new ArrayList<>();

        // 无限创建数组存入List中
        while (true) {
            ints.add(new int[1000]);
        }
    }
}
