public class Test4 {
    public static void main(String[] args) {
        int i = 0;
        int j = 0;
        while (i < 10) {
            if (j < 5) {
                j += 1;
                continue;
            }
            i += j;
        }
        i += 1;
    }
}