package nachos.threads;

import java.util.LinkedList;

import javax.swing.plaf.basic.BasicComboBoxUI.KeyHandler;

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
    Condition listening, speaking;
    // listeners sleep on listening, speakers sleep on speaking
    Lock lock;
    // LinkedList<KThread> speakers;
    // LinkedList<KThread> listeners;

    int speakers; // keeps track of how many speakers and listeners waiting on this object
    int listeners;

    /**
     * Allocate a new communicator.
     */
    public Communicator() {
        lock = new Lock(); // has release(), acquire() and isHeldByCurrentThread()
        listening = new Condition(lock); // has sleep(), wake() and wakeAll();
        speaking = new Condition(lock);

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
        

        lock.acquire(); // current thread aka speaker acquires
        

        

        if (listeners > 0) {
            System.out.println("speaking");
            listening.wake(); // idk which thread is woken here
            listeners--;

        } else {
            speakers++;
            speaking.sleep();
        }

        // if (listeners.size() > 0) {
        // // atleast one listener is waiting on this lock
        // KThread listener = listeners.removeFirst(); //dont think i need this tbh
        // listening.wake(); // idk which thread is woken here
        // // send int to listener here

        // } else {

        // speaking.sleep(); // this thread now sleeps on speaking Condition
        // }

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

        lock.acquire();

        speaking.wake();

        

        if (speakers > 0) {
            System.out.println("listening");
            speaking.wake(); // idk which thread is woken here
            speakers++;

        } else {
            listeners++;
            listening.sleep();
        }
        lock.release();

        return 0;
    }
}
