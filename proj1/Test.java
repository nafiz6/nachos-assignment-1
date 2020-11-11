package nachos.proj1;

import nachos.threads.*;
import nachos.machine.*;

public class Test {

    public static void initiateTest()
    {
        new JoinTest().performTest();
        //new Condition2Test().performTest();
        new AlarmTest().performTest();
    }
    
}

class JoinTest
{
    public JoinTest(){}

    public void performTest()
    {
        System.out.println("testing for task 1 initiated");
        System.out.println("-------------------------------");

        KThread t0 = new KThread(new PingTest(0)).setName("forked thread 0");
        System.out.println("forked thread 0 and joining....");
        t0.fork();
        t0.join();
        System.out.println("joined with thread 0");

        new KThread(new PingTest(1)).setName("forked thread 1").fork();
        new KThread(new PingTest(2)).setName("forked thread 2").fork();
        new PingTest(0).run();

        System.out.println();

        KThread t1 = new KThread(new PingTest(1)).setName("forked thread 1");
        KThread t2 = new KThread(new PingTest(2)).setName("forked thread 2");

        t1.fork();
        t2.fork();

        t1.join();
        t2.join();

        new PingTest(0).run();

        System.out.println("------------------------------------------");
        System.out.println("testing for task 1 finished");
        System.out.println("------------------------------------------");

    }

    private static class PingTest implements Runnable{

        PingTest(int which)
        {
            this.which = which;
        }

        public void run()
        {
            for(int i=0; i<5; i++)
            {
                System.out.println("*** thread " + which + " looped "
                 + i + " times");

                 KThread.yield();
            }
        }

        private final int which;
    }


}
/*

class Condition2Test
{
    public Condition2Test()
    {
        com = new Communicator();
    }

    public void performTest()
    {
        System.out.println("testing for task 2 & 4 initiated");
        System.out.println("---------------------------------");

        KThread l1 = new KThread(new Listener(1,com)).setName("listener thread 1");
        KThread l2 = new KThread(new Listener(2,com)).setName("listener thread 2");

        KThread s1 = new KThread(new Speaker(1,com)).setName("speaker thread 1"); 
        KThread s2 = new KThread(new Speaker(2,com)).setName("speaker thread 2");
        KThread s3 = new KThread(new Speaker(3,com)).setName("speaker thread 3");

        l1.fork();
        l2.fork();
        s1.fork();
        s2.fork();
        s3.fork();

        l1.join();
        l2.join();
        s1.join();
        s2.join();
        s3.join();

        System.out.println("---------------------------------");
        System.out.println("testing for task 2 & 4 finished");
        System.out.println("---------------------------------");

    }

    private static class Speaker implements Runnable{

        Speaker(int which, Communicator com)
        {
            this.which = which;
            this.com = com;
        }
        
        public void run()
        {
            for(int i=0; i<2; i++)
            {
                KThread.yield();
                com.speak(i);
                KThread.yield();
            }
        }

        private int which;
        private Communicator com;
    }

    Communicator com;
}

*/

class AlarmTest{

    private static class AlarmTestRunnable implements Runnable{

        AlarmTestRunnable(long time, Alarm alarm)
        {
            this.time = time;
            this.alarm = alarm;
        }

        public void run()
        {
            System.out.println(KThread.currentThread().getName() + 
            "rings at " + Machine.timer().getTime());
            alarm.waitUntil(time);
            System.out.println(KThread.currentThread().getName() + 
            "rings at " + Machine.timer().getTime());
        }

        private final long time;
        private final Alarm alarm;

    }

    public void performTest()
    {

    }
}
