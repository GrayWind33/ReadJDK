package test;

public class ConcurentMapTest {
    private static final int tableSizeFor(int c) {
        int n = c - 1;
        n |= n >>> 1;
        n |= n >>> 2;
        n |= n >>> 4;
        n |= n >>> 8;
        n |= n >>> 16;
        return n + 1;
    }
    
    public static void main(String args[]) {
        int c = 12;
        c = tableSizeFor(c);
        System.out.println(c);
    }
}
