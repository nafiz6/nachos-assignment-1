package nachos.threads;

import java.util.ArrayList;
import java.util.List;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {

    private class ThreadTime {
        KThread thread;
        long wakeTime;

        ThreadTime(KThread thread, long wakeTime) {
            this.thread = thread;
            this.wakeTime = wakeTime;
        }
    }

    List<ThreadTime> waitingThreads;

    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p>
     * <b>Note</b>: Nachos will not function correctly with more than one alarm.
     */
    public Alarm() {
        waitingThreads = new ArrayList<>();
        Machine.timer().setInterruptHandler(new Runnable() {
            public void run() {
                timerInterrupt();
            }
        });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current thread
     * to yield, forcing a context switch if there is another thread that should be
     * run.
     */
    public void timerInterrupt() {
        

        for (int i = 0; i < waitingThreads.size(); i++) {
            ThreadTime t = waitingThreads.get(i);
            if (Machine.timer().getTime() > t.wakeTime) {
                waitingThreads.remove(i);
                Machine.interrupt().disable();
                t.thread.ready();
                Machine.interrupt().enable();
            }
        }
        
        
        KThread.yield();
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks, waking it up in
     * the timer interrupt handler. The thread must be woken up (placed in the
     * scheduler ready set) during the first timer interrupt where
     *
     * <p>
     * <blockquote> (current time) >= (WaitUntil called time)+(x) </blockquote>
     *
     * @param x the minimum number of clock ticks to wait.
     *
     * @see nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
        // for now, cheat just to get something working (busy waiting is bad)

        // while (wakeTime > Machine.timer().getTime()) KThread.yield();

        // need a list of threads that are currently waiting, along with wakeup times of
        // each thread
        // i guess iterate this list and check which threads can be put to ready state
        // (do this checking inside interrupt handler)
        long wakeTime = Machine.timer().getTime() + x;
        waitingThreads.add(new ThreadTime(KThread.currentThread(), wakeTime));
        Machine.interrupt().disable();
        KThread.sleep();
        Machine.interrupt().enable();

    }
}
