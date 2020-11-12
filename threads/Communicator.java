package nachos.threads;

import java.util.LinkedList;

import nachos.machine.*;

/**
 * A <i>communicator</i> allows threads to synchronously exchange 32-bit
 * messages. Multiple threads can be waiting to <i>speak</i>, and multiple
 * threads can be waiting to <i>listen</i>. But there should never be a time
 * when both a speaker and a listener are waiting, because the two threads can
 * be paired off at this point.
 */
public class Communicator {

    // each Communicator object will have one Condition object
    // both listener and speaker uses the same Communicator object
    // multiple threads can use the same Communicator object, in this case they'll
    // wait until someone speaks or listens on this object too
    Condition2 listening, speaking;
    // listeners sleep on listening, speakers sleep on speaking
    Lock lock;
    private static int message;
    // LinkedList<KThread> speakers;
    // LinkedList<KThread> listeners;

    int speakers; // keeps track of how many speakers and listeners waiting on this object
    int listeners;

    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        lock = new Lock(); // has release(), acquire() and isHeldByCurrentThread()
        listening = new Condition2(lock); // has sleep(), wake() and wakeAll();
        speaking = new Condition2(lock);
        // transferring = new Condition(lock);
        

        // need 2 lists to keep track of speakers and listeners on this communicator
        // object

    }

    /**
     * Wait for a thread to listen through this communicator, and then transfer
     * <i>word</i> to the listener.
     *
     * <p>
     * Does not return until this thread is paired up with a listening thread.
     * Exactly one listener should receive <i>word</i>.
     *
     * @param word the integer to transfer.
     */
    public void speak(int word) {
        // KThread speaker = KThread.currentThread();

        // speakers.add(speaker);

        

            //a,b,c,d,e need f to finish
        /*
            Lock lock;
            Condition cond(lock)
             a {
                 lock.acquire();
                 if(task not done)
                    cond.sleep()
                 lock.release();
             }
             b {
                 lock.acquire();
                 if(task not done)
                    cond.sleep()
                 lock.release();
             }
            ...
            e {
                lock.acquire();
                if(task not done)
                    cond.sleep()
                lock.release();
            }

            f {
                lock.acquire();
                if(task finished) {
                    cond.wakeAll()
                }
                lock.release();
            }

            
        

        */
        

        lock.acquire(); // current thread acquires
        speakers++;

        if(listeners == 0) {
            speaking.sleep();
    

        }
 
        // Lib.assertTrue(message != null);
        System.out.println("speaking message: " + message);
        message = word;
        listeners--;
        
        listening.wake(); 
        lock.release();

        

    }

    /**
     * Wait for a thread to speak through this communicator, and then return the
     * <i>word</i> that thread passed to <tt>speak()</tt>.
     *
     * @return the integer transferred.
     */
    public int listen() {
        // KThread listener = KThread.currentThread();
        // listeners.add(listener);

        lock.acquire(); // current thread acquires
        listeners++;

        if(speakers == 0) {
            listening.sleep();

        } else {
            speaking.wake(); 
            listening.sleep();
        }

        
        // Lib.assertTrue(message != null);
        System.out.println("listening to message: " + message);
        speakers--;
        lock.release();

        return message;
    }
}
