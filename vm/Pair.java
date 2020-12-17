package nachos.vm;

public class Pair {
    int pid;
    int vpn;

    Pair(int p, int v) {
        pid = p;
        vpn = v;
    }

    @Override
    public boolean equals(Object obj) {
        Pair temp = (Pair) obj;
        return temp.pid == pid && temp.vpn == vpn;
    }

    @Override
    public int hashCode() {
        return (pid * 153 + vpn * 177) % Integer.MAX_VALUE;
    }
}
